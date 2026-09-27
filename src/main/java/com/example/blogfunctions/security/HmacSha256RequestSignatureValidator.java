package com.example.blogfunctions.security;

import com.example.blogfunctions.configuration.WebhookProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class HmacSha256RequestSignatureValidator implements RequestSignatureValidator {
    private final WebhookProperties properties;
    private final Clock clock;

    public HmacSha256RequestSignatureValidator(WebhookProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        if (properties.enabled() && (properties.secret() == null || properties.secret().isBlank())) {
            throw new IllegalStateException("WEBHOOK_HMAC_SECRET must be configured when signatures are enabled");
        }
    }

    @Override
    public void validate(byte[] rawPayload, Map<String, String> headers) {
        if (!properties.enabled()) {
            return;
        }
        String signature = header(headers, properties.header());
        if (signature == null || !signature.matches("sha256=[0-9a-fA-F]{64}")) {
            throw new InvalidSignatureException();
        }
        String timestamp = null;
        if (properties.timestampEnabled()) {
            timestamp = header(headers, properties.timestampHeader());
            try {
                if (timestamp == null || !timestamp.matches("[0-9]{1,12}")) {
                    throw new InvalidSignatureException();
                }
                long age = Math.subtractExact(clock.instant().getEpochSecond(), Long.parseLong(timestamp));
                if (age < 0 || age > properties.maxAgeSeconds()) {
                    throw new InvalidSignatureException();
                }
            } catch (NumberFormatException | ArithmeticException ex) {
                throw new InvalidSignatureException();
            }
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            // Authenticate the timestamp as well as the original bytes to prevent replay-window tampering.
            if (timestamp != null) {
                mac.update((timestamp + ".").getBytes(StandardCharsets.US_ASCII));
            }
            byte[] expected = mac.doFinal(rawPayload);
            byte[] supplied = HexFormat.of().parseHex(signature.substring(7));
            if (!MessageDigest.isEqual(expected, supplied)) {
                throw new InvalidSignatureException();
            }
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC initialization failed", ex);
        }
    }

    private static String header(Map<String, String> headers, String name) {
        return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue).findFirst().orElse(null);
    }
}
