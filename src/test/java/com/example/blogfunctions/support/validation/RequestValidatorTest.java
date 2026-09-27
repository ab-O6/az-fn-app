package com.example.blogfunctions.support.validation;

import com.example.blogfunctions.adapter.inbound.http.SubmitBlogRequest;
import com.example.blogfunctions.support.error.RequestValidationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class RequestValidatorTest {
    private static ValidatorFactory factory;
    private RequestValidator validator;
    @BeforeAll static void open() { factory = Validation.buildDefaultValidatorFactory(); }
    @AfterAll static void close() { factory.close(); }
    @BeforeEach void setup() { validator = new RequestValidator(factory.getValidator()); }

    @Test void acceptsValidAndOptionalSource() {
        var request = new SubmitBlogRequest("title", "content", "author", null);
        assertSame(request, validator.validate(request));
    }
    @Test void rejectsBlankRequiredFields() {
        var ex = assertThrows(RequestValidationException.class,
                () -> validator.validate(new SubmitBlogRequest(" ", "", null, null)));
        assertEquals(java.util.Set.of("title", "content", "author"), ex.fieldErrors().keySet());
    }
    @Test void rejectsOversizeFields() {
        var ex = assertThrows(RequestValidationException.class, () -> validator.validate(
                new SubmitBlogRequest("t".repeat(251), "body", "a".repeat(201), "s".repeat(101))));
        assertEquals(java.util.Set.of("title", "author", "source"), ex.fieldErrors().keySet());
    }
    @Test void rejectsNullBody() { assertThrows(RequestValidationException.class, () -> validator.validate(null)); }
}
