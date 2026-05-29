package com.aireviewer.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.aireviewer.config.WebhookProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Validates the GitHub {@code X-Hub-Signature-256} header against the raw request
 * body using HMAC-SHA256 and the configured shared secret.
 *
 * <p>The comparison is constant-time to avoid timing side-channels, and the
 * signature is computed over the exact bytes GitHub signed — which is why the
 * controller must read the raw body before any deserialization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubSignatureValidator {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final WebhookProperties properties;

    /**
     * Verifies that the signature header matches the HMAC-SHA256 of the body.
     *
     * @param payload         raw request body bytes, exactly as received
     * @param signatureHeader value of the {@code X-Hub-Signature-256} header
     * @return {@code true} only if the header is present, well-formed, and matches
     */
    public boolean isValid(byte[] payload, String signatureHeader) {
        if (payload == null || !StringUtils.hasText(signatureHeader)
                || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }
        if (!StringUtils.hasText(properties.secret())) {
            log.warn("Webhook secret is not configured; rejecting webhook");
            return false;
        }

        String expected = SIGNATURE_PREFIX + computeHmacHex(payload);
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = signatureHeader.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, providedBytes);
    }

    private String computeHmacHex(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload));
        } catch (Exception e) {
            // Misconfiguration (bad key/algorithm) must never pass validation.
            log.error("Failed to compute HMAC signature: {}", e.getMessage());
            throw new IllegalStateException("Unable to compute webhook signature", e);
        }
    }
}
