package com.example.blogfunctions.security;

import com.example.blogfunctions.configuration.WebhookProperties;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HmacSha256RequestSignatureValidatorTest {
    private static final String SECRET = UUID.randomUUID().toString();
    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC);
    private static final byte[] BODY = "{ \"title\": \"café\" }".getBytes(StandardCharsets.UTF_8);

    private HmacSha256RequestSignatureValidator validator(boolean enabled, boolean timestamp) {
        return new HmacSha256RequestSignatureValidator(new WebhookProperties(
                enabled, SECRET, "X-Signature", timestamp, "X-Timestamp", 300), CLOCK);
    }

    private Map<String, String> headers(String timestamp, byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        if (timestamp != null)
            mac.update((timestamp + ".").getBytes(StandardCharsets.US_ASCII));
        Map<String, String> headers = new HashMap<>();
        headers.put("x-signature", "sha256=" + HexFormat.of().formatHex(mac.doFinal(body)));
        if (timestamp != null)
            headers.put("x-timestamp", timestamp);
        return headers;
    }

    @Test
    void acceptsExactRawBytesAndCaseInsensitiveHeaders() throws Exception {
        validator(true, true).validate(BODY, headers("1000", BODY));
    }

    @Test
    void detectsBodyMutation() throws Exception {
        var headers = headers("1000", BODY);
        assertThrows(InvalidSignatureException.class, () -> validator(true, true).validate("{}".getBytes(), headers));
    }

    @Test
    void rejectsExpiredFutureAndMalformedTimestamps() throws Exception {
        for (String ts : List.of("699", "1001", "bad", "999999999999999999999999")) {
            var headers = headers(ts, BODY);
            assertThrows(InvalidSignatureException.class, () -> validator(true, true).validate(BODY, headers));
        }
        assertThrows(InvalidSignatureException.class, () -> validator(true, true).validate(BODY, headers(null, BODY)));
    }

    @Test
    void acceptsReplayWindowBoundary() throws Exception {
        validator(true, true).validate(BODY, headers("700", BODY));
    }

    @Test
    void timestampCannotBeReplacedToRefreshAnOldSignature() throws Exception {
        var headers = headers("699", BODY);
        headers.put("x-timestamp", "1000");
        assertThrows(InvalidSignatureException.class, () -> validator(true, true).validate(BODY, headers));
    }

    @Test
    void rejectsMalformedAndMissingSignatures() {
        for (String signature : List.of("", "sha256=aa", "sha256=" + "z".repeat(64), "x".repeat(1000))) {
            assertThrows(InvalidSignatureException.class,
                    () -> validator(true, false).validate(BODY, Map.of("X-Signature", signature)));
        }
        assertThrows(InvalidSignatureException.class, () -> validator(true, false).validate(BODY, Map.of()));
    }

    @Test
    void supportsBodyOnlyProtocolWhenTimestampDisabled() throws Exception {
        validator(true, false).validate(BODY, headers(null, BODY));
    }

    @Test
    void disabledValidationDoesNotRequireHeaders() {
        validator(false, true).validate(BODY, Map.of());
    }

    @Test
    void enabledValidationFailsFastWithoutSecret() {
        assertThrows(IllegalStateException.class, () -> new HmacSha256RequestSignatureValidator(
                new WebhookProperties(true, "", "X-Signature", true, "X-Timestamp", 300), CLOCK));
    }

    @Test
    void configurationToStringRedactsSecret() {
        assertFalse(new WebhookProperties(true, SECRET, "s", true, "t", 300).toString().contains(SECRET));
    }
}
