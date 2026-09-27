package com.example.blogfunctions.adapter.inbound.http;

import com.example.blogfunctions.application.port.inbound.SubmitBlogForReviewUseCase;
import com.example.blogfunctions.configuration.JacksonConfiguration;
import com.example.blogfunctions.domain.model.BlogSubmission;
import com.example.blogfunctions.security.*;
import com.example.blogfunctions.support.validation.RequestValidator;
import com.microsoft.azure.functions.*;
import jakarta.validation.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubmitBlogForReviewFunctionTest {
    private static final String VALID = """
            {"title":"Blog","content":"Body","author":"Author","source":"web"}
            """;
    private static ValidatorFactory factory;
    private final SubmitBlogForReviewUseCase useCase = mock(SubmitBlogForReviewUseCase.class);
    private final RequestSignatureValidator signatures = mock(RequestSignatureValidator.class);
    private final ExecutionContext context = mock(ExecutionContext.class);
    private final HttpRequestMessage<Optional<String>> request = mock();
    private final HttpResponseMessage.Builder builder = mock(HttpResponseMessage.Builder.class, RETURNS_SELF);
    private final HttpResponseMessage response = mock(HttpResponseMessage.class);
    private SubmitBlogForReviewFunction function;

    @BeforeAll static void open() { factory = Validation.buildDefaultValidatorFactory(); }
    @AfterAll static void close() { factory.close(); }
    @BeforeEach void setup() {
        function = new SubmitBlogForReviewFunction(signatures, new JacksonConfiguration().jsonMapper(),
                new RequestValidator(factory.getValidator()), useCase, Clock.systemUTC());
        when(request.getHeaders()).thenReturn(Map.of("content-type", "application/json; charset=utf-8"));
        when(request.createResponseBuilder(any(HttpStatus.class))).thenReturn(builder);
        when(builder.build()).thenReturn(response);
        when(context.getInvocationId()).thenReturn("test-invocation");
    }
    private void invoke(String body) {
        when(request.getBody()).thenReturn(Optional.of(body));
        assertSame(response, function.run(request, context));
    }
    @Test void acceptsAndDelegatesAfterSignatureValidation() {
        invoke(VALID);
        var order = inOrder(signatures, useCase);
        order.verify(signatures).validate(eq(VALID.getBytes(StandardCharsets.UTF_8)), anyMap());
        order.verify(useCase).submit(new BlogSubmission("Blog", "Body", "Author", "web"));
        verify(request).createResponseBuilder(HttpStatus.ACCEPTED);
        verify(builder).body("{\"status\":\"accepted\"}");
    }
    @ParameterizedTest
    @ValueSource(strings = {"{", "null", "[]", "{}", "{\"title\":\"a\",\"title\":\"b\"}", "{} {}", "{\"unknown\":1}"})
    void rejectsInvalidPayload(String body) {
        invoke(body);
        verify(request).createResponseBuilder(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(useCase);
    }
    @Test void rejectsBadSignatureBeforeParsing() {
        doThrow(new InvalidSignatureException()).when(signatures).validate(any(), anyMap());
        invoke("malformed");
        verify(request).createResponseBuilder(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(useCase);
    }
    @Test void rejectsUnsupportedMediaType() {
        when(request.getHeaders()).thenReturn(Map.of("Content-Type", "text/plain"));
        invoke(VALID);
        verify(request).createResponseBuilder(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        verifyNoInteractions(signatures, useCase);
    }
    @Test void hidesUnexpectedExceptionDetails() {
        doThrow(new IllegalStateException("private-secret")).when(useCase).submit(any());
        invoke(VALID);
        verify(request).createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR);
        var body = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(builder).body(body.capture());
        assertFalse(body.getValue().toString().contains("private-secret"));
        assertTrue(body.getValue().toString().contains("INTERNAL_ERROR"));
    }
    @Test void preservesUtf8WhitespaceAndUnicodeBeforeDeserialization() {
        String body = "  \r\n" + VALID.replace("Blog", "Café ☕");
        invoke(body);
        var order = inOrder(signatures, useCase);
        order.verify(signatures).validate(eq(body.getBytes(StandardCharsets.UTF_8)), anyMap());
        order.verify(useCase).submit(new BlogSubmission("Café ☕", "Body", "Author", "web"));
        verify(request).createResponseBuilder(HttpStatus.ACCEPTED);
    }
    @Test void rejectsNonUtf8CharsetBeforeAuthentication() {
        when(request.getHeaders()).thenReturn(Map.of("Content-Type", "application/json; charset=utf-16"));
        invoke(VALID);
        verify(request).createResponseBuilder(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        verifyNoInteractions(signatures, useCase);
    }
    @Test void acceptsQuotedUtf8Charset() {
        when(request.getHeaders()).thenReturn(Map.of("Content-Type", "application/json; charset=\"UTF-8\""));
        invoke(VALID);
        verify(request).createResponseBuilder(HttpStatus.ACCEPTED);
    }
    @Test void missingBodyIsBadRequest() {
        when(request.getBody()).thenReturn(Optional.empty());
        function.run(request, context);
        verify(request).createResponseBuilder(HttpStatus.BAD_REQUEST);
    }
}
