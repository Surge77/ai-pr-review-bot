package com.aireviewer.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

class CacheKeyStrategyTest {

    private final CacheKeyStrategy strategy = new CacheKeyStrategy();

    @Test
    void builds_key_with_prefix_repo_filename_and_patch_sha256() throws Exception {
        String patch = "@@ -1 +1 @@\n-old\n+new";
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(patch.getBytes(StandardCharsets.UTF_8));
        String expectedSha = HexFormat.of().formatHex(digest);

        String key = strategy.key("octo/repo", "src/Main.java", patch);

        assertThat(key).isEqualTo("review:octo/repo:src/Main.java:" + expectedSha);
    }

    @Test
    void different_patches_produce_different_keys() {
        String a = strategy.key("octo/repo", "f.java", "patch-A");
        String b = strategy.key("octo/repo", "f.java", "patch-B");

        assertThat(a).isNotEqualTo(b);
    }
}
