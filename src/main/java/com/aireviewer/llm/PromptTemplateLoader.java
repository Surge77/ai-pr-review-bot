package com.aireviewer.llm;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

/**
 * Loads the code-review prompt template once and renders it per file.
 *
 * <p>Placeholders {@code {filename}} and {@code {patch}} are replaced by literal
 * substitution (not a templating engine) so the JSON braces in the template are
 * left untouched.
 */
@Component
public class PromptTemplateLoader {

    private static final String TEMPLATE_PATH = "prompts/code-review.st";

    private String template;

    @PostConstruct
    void load() {
        try {
            byte[] bytes = FileCopyUtils.copyToByteArray(new ClassPathResource(TEMPLATE_PATH).getInputStream());
            this.template = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load prompt template: " + TEMPLATE_PATH, e);
        }
    }

    /**
     * Renders the review prompt for a file.
     *
     * @param filename the file under review
     * @param patch    the diff content
     * @return the rendered prompt
     */
    public String render(String filename, String patch) {
        return template
                .replace("{filename}", filename == null ? "" : filename)
                .replace("{patch}", patch == null ? "" : patch);
    }
}
