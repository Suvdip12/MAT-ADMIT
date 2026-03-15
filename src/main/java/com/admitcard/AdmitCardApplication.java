package com.admitcard;

import com.admitcard.config.PdfSignatureProperties;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.security.Security;

@SpringBootApplication
@EnableConfigurationProperties(PdfSignatureProperties.class)
public class AdmitCardApplication {

	public static void main(String[] args) {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
		SpringApplication.run(AdmitCardApplication.class, args);
	}

}
