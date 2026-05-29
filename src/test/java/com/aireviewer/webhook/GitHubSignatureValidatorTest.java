package com.aireviewer.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.aireviewer.config.WebhookProperties;
import org.junit.jupiter.api.Test;

class GitHubSignatureValidatorTest {

    private static final String SECRET = "It's a Secret to Everybody";
    private static final byte[] BODY = "Hello, World!".getBytes(StandardCharsets.UTF_8);

    private final GitHubSignatureValidator validator =
            new GitHubSignatureValidator(new WebhookProperties(SECRET, List.of("opened")));

    private static String sign(byte[] body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }

    @Test
    void accepts_a_correctly_signed_body() throws Exception {
        assertThat(validator.isValid(BODY, sign(BODY, SECRET))).isTrue();
    }

    @Test
    void rejects_a_tampered_body() throws Exception {
        String validSig = sign(BODY, SECRET);
        byte[] tampered = "Hello, World?".getBytes(StandardCharsets.UTF_8);

        assertThat(validator.isValid(tampered, validSig)).isFalse();
    }

    @Test
    void rejects_signature_made_with_the_wrong_secret() throws Exception {
        assertThat(validator.isValid(BODY, sign(BODY, "wrong-secret"))).isFalse();
    }

    @Test
    void rejects_missing_header() {
        assertThat(validator.isValid(BODY, null)).isFalse();
        assertThat(validator.isValid(BODY, "")).isFalse();
    }

    @Test
    void rejects_header_without_sha256_prefix() throws Exception {
        String hexOnly = sign(BODY, SECRET).substring("sha256=".length());

        assertThat(validator.isValid(BODY, hexOnly)).isFalse();
    }

    @Test
    void rejects_when_secret_is_not_configured() throws Exception {
        GitHubSignatureValidator noSecret =
                new GitHubSignatureValidator(new WebhookProperties("", List.of("opened")));

        assertThat(noSecret.isValid(BODY, sign(BODY, SECRET))).isFalse();
    }
}
