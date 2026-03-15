package com.admitcard.service;

import com.admitcard.config.PdfSignatureProperties;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.IExternalDigest;
import com.itextpdf.signatures.IExternalSignature;
import com.itextpdf.signatures.PdfSignatureAppearance;
import com.itextpdf.signatures.PdfSigner;
import com.itextpdf.signatures.PrivateKeySignature;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Locale;
import java.util.UUID;

@Service
public class PdfSigningService {

    private static final String DEFAULT_KEYSTORE_TYPE = "PKCS12";
    private static final String SIGNATURE_FIELD_PREFIX = "AdmitProSignature";
    private static final DateTimeFormatter SIGNING_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);

    private final PdfSignatureProperties signatureProperties;

    public PdfSigningService(PdfSignatureProperties signatureProperties) {
        this.signatureProperties = signatureProperties;
    }

    public boolean isEnabled() {
        return signatureProperties.isEnabled()
                && StringUtils.hasText(signatureProperties.getKeystorePath())
                && StringUtils.hasText(signatureProperties.getKeystorePassword());
    }

    public void signPdfFileInPlace(String filePath) throws Exception {
        if (!isEnabled()) {
            return;
        }

        Path path = Path.of(filePath);
        byte[] signedBytes = signPdf(Files.readAllBytes(path), signatureProperties.isVisible());
        Path tempFile = Files.createTempFile(path.getParent(), "signed-", ".pdf");
        Files.write(tempFile, signedBytes);
        Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
    }

    public byte[] signPdfIfEnabled(byte[] pdfBytes) throws Exception {
        return signPdf(pdfBytes, signatureProperties.isVisible());
    }

    public byte[] signPdfInvisiblyIfEnabled(byte[] pdfBytes) throws Exception {
        return signPdf(pdfBytes, false);
    }

    public byte[] signPdf(byte[] pdfBytes, boolean visibleSignature) throws Exception {
        if (!isEnabled()) {
            return pdfBytes;
        }

        ensureBouncyCastleProvider();
        SigningMaterial signingMaterial = loadSigningMaterial();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes))) {
            PdfSigner signer = new PdfSigner(reader, outputStream, new StampingProperties());
            signer.setFieldName(SIGNATURE_FIELD_PREFIX + UUID.randomUUID().toString().replace("-", ""));
            signer.setCertificationLevel(resolveCertificationLevel(signatureProperties.getCertificationLevel()));

            PdfSignatureAppearance appearance = signer.getSignatureAppearance()
                    .setReason(defaultReason())
                    .setLocation(defaultLocation())
                    .setReuseAppearance(false)
                    .setRenderingMode(PdfSignatureAppearance.RenderingMode.DESCRIPTION)
                    .setCertificate(signingMaterial.certificate())
                    .setLayer2Text(buildLayer2Text(signingMaterial.certificate()));

            if (visibleSignature) {
                appearance.setPageNumber(Math.max(1, signatureProperties.getPage()));
                appearance.setPageRect(new Rectangle(
                        signatureProperties.getX(),
                        signatureProperties.getY(),
                        signatureProperties.getWidth(),
                        signatureProperties.getHeight()
                ));
            }

            IExternalDigest digest = new BouncyCastleDigest();
            IExternalSignature signature = new PrivateKeySignature(
                    signingMaterial.privateKey(),
                    DigestAlgorithms.SHA256,
                    BouncyCastleProvider.PROVIDER_NAME
            );

            signer.signDetached(
                    digest,
                    signature,
                    signingMaterial.chain(),
                    null,
                    null,
                    null,
                    0,
                    PdfSigner.CryptoStandard.CMS
            );
        }

        return outputStream.toByteArray();
    }

    private SigningMaterial loadSigningMaterial() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(DEFAULT_KEYSTORE_TYPE);
        Path keystorePath = Path.of(signatureProperties.getKeystorePath());

        try (InputStream inputStream = Files.newInputStream(keystorePath)) {
            keyStore.load(inputStream, signatureProperties.getKeystorePassword().toCharArray());
        }

        String alias = resolveAlias(keyStore);
        char[] keyPassword = resolveKeyPassword();
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, keyPassword);
        Certificate[] chain = keyStore.getCertificateChain(alias);

        if (privateKey == null || chain == null || chain.length == 0) {
            throw new IllegalStateException("Unable to load signing key material from the configured keystore.");
        }

        return new SigningMaterial(privateKey, chain);
    }

    private String resolveAlias(KeyStore keyStore) throws Exception {
        if (StringUtils.hasText(signatureProperties.getAlias())) {
            return signatureProperties.getAlias();
        }

        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                return alias;
            }
        }

        throw new IllegalStateException("No private key entry was found in the configured keystore.");
    }

    private char[] resolveKeyPassword() {
        String keyPassword = StringUtils.hasText(signatureProperties.getKeyPassword())
                ? signatureProperties.getKeyPassword()
                : signatureProperties.getKeystorePassword();
        return keyPassword.toCharArray();
    }

    private String buildLayer2Text(X509Certificate certificate) {
        String signerName = extractSignerName(certificate);
        String signingDate = SIGNING_DATE_FORMAT.format(ZonedDateTime.now(ZoneId.systemDefault()));

        return "Digitally Signed."
                + "\nName: " + signerName
                + "\nDate: " + signingDate
                + "\nReason: " + defaultReason()
                + "\nLocation: " + defaultLocation();
    }

    private String extractSignerName(X509Certificate certificate) {
        if (certificate == null) {
            return "Unknown";
        }

        String subject = certificate.getSubjectX500Principal().getName();
        for (String part : subject.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("CN=")) {
                return trimmed.substring(3);
            }
        }

        return subject;
    }

    private int resolveCertificationLevel(String certificationLevel) {
        if (!StringUtils.hasText(certificationLevel)) {
            return PdfSigner.CERTIFIED_NO_CHANGES_ALLOWED;
        }

        return switch (certificationLevel.trim().toUpperCase(Locale.ROOT)) {
            case "NOT_CERTIFIED" -> PdfSigner.NOT_CERTIFIED;
            case "CERTIFIED_FORM_FILLING" -> PdfSigner.CERTIFIED_FORM_FILLING;
            case "CERTIFIED_FORM_FILLING_AND_ANNOTATIONS" -> PdfSigner.CERTIFIED_FORM_FILLING_AND_ANNOTATIONS;
            default -> PdfSigner.CERTIFIED_NO_CHANGES_ALLOWED;
        };
    }

    private String defaultReason() {
        return StringUtils.hasText(signatureProperties.getReason())
                ? signatureProperties.getReason()
                : "Digitally signed by AdmitPro";
    }

    private String defaultLocation() {
        return StringUtils.hasText(signatureProperties.getLocation())
                ? signatureProperties.getLocation()
                : "AdmitPro";
    }

    private void ensureBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private record SigningMaterial(PrivateKey privateKey, Certificate[] chain) {
        private X509Certificate certificate() {
            return (X509Certificate) chain[0];
        }
    }
}
