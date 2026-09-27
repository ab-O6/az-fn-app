package com.example.blogfunctions.security;

import java.util.Map;

public interface RequestSignatureValidator {
    void validate(byte[] rawPayload, Map<String, String> headers);
}
