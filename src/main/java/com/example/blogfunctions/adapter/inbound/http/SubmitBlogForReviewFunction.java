package com.example.blogfunctions.adapter.inbound.http;

import com.example.blogfunctions.application.port.inbound.SubmitBlogForReviewUseCase;
import com.example.blogfunctions.domain.model.BlogSubmission;
import com.example.blogfunctions.security.InvalidSignatureException;
import com.example.blogfunctions.security.RequestSignatureValidator;
import com.example.blogfunctions.support.error.ApiError;
import com.example.blogfunctions.support.error.RequestValidationException;
import com.example.blogfunctions.support.validation.RequestValidator;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SubmitBlogForReviewFunction {
    private static final Logger log = LoggerFactory.getLogger(SubmitBlogForReviewFunction.class);
    private final RequestSignatureValidator signatures;
    private final JsonMapper mapper;
    private final RequestValidator validator;
    private final SubmitBlogForReviewUseCase useCase;
    private final Clock clock;

    public SubmitBlogForReviewFunction(RequestSignatureValidator signatures, JsonMapper mapper,
            RequestValidator validator, SubmitBlogForReviewUseCase useCase, Clock clock) {
        this.signatures = signatures;
        this.mapper = mapper;
        this.validator = validator;
        this.useCase = useCase;
        this.clock = clock;
    }

    @FunctionName("submitBlogForReview")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = HttpMethod.POST, route = "blogs/review",
                    authLevel = AuthorizationLevel.ANONYMOUS, dataType = "binary")
            HttpRequestMessage<Optional<byte[]>> request, ExecutionContext context) {
        try {
            String contentType = request.getHeaders().entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase("Content-Type"))
                    .map(Map.Entry::getValue).findFirst().orElse("");
            if (!contentType.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
                return error(request, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                        "Content-Type must be application/json", Map.of());
            }
            byte[] raw = request.getBody().orElseGet(() -> new byte[0]);
            signatures.validate(raw, request.getHeaders());
            SubmitBlogRequest dto;
            try {
                dto = validator.validate(mapper.readValue(raw, SubmitBlogRequest.class));
            } catch (JacksonException ex) {
                return error(request, HttpStatus.BAD_REQUEST, "INVALID_JSON", "Malformed JSON request", Map.of());
            }
            useCase.submit(new BlogSubmission(dto.title(), dto.content(), dto.author(), dto.source()));
            log.info("Blog request outcome=accepted invocationId={}", context.getInvocationId());
            return json(request, HttpStatus.ACCEPTED, Map.of("status", "accepted"));
        } catch (InvalidSignatureException ex) {
            return error(request, HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE", "Request authentication failed", Map.of());
        } catch (RequestValidationException ex) {
            return error(request, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", ex.fieldErrors());
        } catch (Exception ex) {
            // Exception messages and traces can contain payloads or secrets.
            log.error("Blog request outcome=failed invocationId={}", context.getInvocationId());
            return error(request, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", Map.of());
        }
    }

    private HttpResponseMessage error(HttpRequestMessage<?> request, HttpStatus status, String code,
            String message, Map<String, String> fields) {
        return json(request, status, new ApiError(code, message, clock.instant(), fields));
    }

    private HttpResponseMessage json(HttpRequestMessage<?> request, HttpStatus status, Object body) {
        return request.createResponseBuilder(status).header("Content-Type", "application/json")
                .body(mapper.writeValueAsString(body)).build();
    }
}
