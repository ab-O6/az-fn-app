package com.example.blogfunctions.security;

public class InvalidSignatureException extends RuntimeException {
    public InvalidSignatureException() {
        super("Invalid request signature");
    }
}
