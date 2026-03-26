package com.admitcard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String FILE_TOO_LARGE_MESSAGE =
            "Uploaded PDF is too large. Current maximum upload size is 25MB.";

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        return payloadTooLargeResponse(FILE_TOO_LARGE_MESSAGE);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipartException(MultipartException exception) {
        if (isPayloadTooLarge(exception)) {
            return payloadTooLargeResponse(FILE_TOO_LARGE_MESSAGE);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("message", "Invalid upload request. Please re-select the PDF and try again.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private ResponseEntity<Map<String, Object>> payloadTooLargeResponse(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    private boolean isPayloadTooLarge(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("maximum upload size exceeded")
                        || normalized.contains("request entity too large")
                        || normalized.contains("file size exceeds")
                        || normalized.contains("size exceeds")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
