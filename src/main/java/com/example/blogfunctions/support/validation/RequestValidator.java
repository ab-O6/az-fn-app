package com.example.blogfunctions.support.validation;

import com.example.blogfunctions.support.error.RequestValidationException;
import jakarta.validation.Validator;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

@Component
public class RequestValidator {
    private final Validator validator;

    public RequestValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> T validate(T value) {
        if (value == null) {
            throw new RequestValidationException(Map.of("body", "must not be null"));
        }
        Map<String, String> errors = new TreeMap<>();
        validator.validate(value).forEach(v -> errors.merge(v.getPropertyPath().toString(),
                v.getMessage(), (a, b) -> a.compareTo(b) < 0 ? a : b));
        if (!errors.isEmpty()) {
            throw new RequestValidationException(errors);
        }
        return value;
    }
}
