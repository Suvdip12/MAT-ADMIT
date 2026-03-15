package com.admitcard.dto;

import java.util.List;
import java.util.ArrayList;

public class SignatureVerificationResult {
    public boolean hasSignatures;
    public List<SignatureInfo> signatures = new ArrayList<>();

    public static class SignatureInfo {
        public String signatureName;
        public String signerName;
        public String signingDate;
        public boolean isValid;
        public String message;
        public String reason;
        public String location;
        public String algorithm;
        public String certificateAuthority;
        
        public SignatureInfo() {}
        
        public SignatureInfo(String signatureName, String signerName, String signingDate, boolean isValid, String message, String reason, String location, String algorithm, String certificateAuthority) {
            this.signatureName = signatureName;
            this.signerName = signerName;
            this.signingDate = signingDate;
            this.isValid = isValid;
            this.message = message;
            this.reason = reason;
            this.location = location;
            this.algorithm = algorithm;
            this.certificateAuthority = certificateAuthority;
        }
    }
    
    public SignatureVerificationResult() {}
}
