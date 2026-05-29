package com.aireviewer.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PromptTemplateLoaderTest {

    private PromptTemplateLoader loader;

    @BeforeEach
    void setUp() {
        loader = new PromptTemplateLoader();
        ReflectionTestUtils.invokeMethod(loader, "load");
    }

    @Test
    void renders_filename_and_patch_while_preserving_json_braces() {
        String prompt = loader.render("src/Main.java", "@@ -1 +1 @@\n-old\n+new");

        assertThat(prompt).contains("file: src/Main.java");
        assertThat(prompt).contains("+new");
        // JSON skeleton braces from the template survive substitution
        assertThat(prompt).contains("\"summary\":")
                .doesNotContain("{filename}")
                .doesNotContain("{patch}");
    }

    @Test
    void handles_null_inputs_without_leaving_placeholders() {
        String prompt = loader.render(null, null);

        assertThat(prompt).doesNotContain("{filename}").doesNotContain("{patch}");
    }
}
