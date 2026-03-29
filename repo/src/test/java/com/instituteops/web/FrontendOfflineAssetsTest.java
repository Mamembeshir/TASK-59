package com.instituteops.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FrontendOfflineAssetsTest {

    @Test
    void templatesDoNotReferenceExternalHttpsAssets() throws IOException {
        Path templatesRoot = Path.of("src/main/resources/templates");
        try (Stream<Path> stream = Files.walk(templatesRoot)) {
            stream
                .filter(path -> path.toString().endsWith(".html"))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        assertThat(content)
                            .withFailMessage("Template %s contains external https asset reference", path)
                            .doesNotContain("https://");
                    } catch (IOException ex) {
                        throw new IllegalStateException("Failed to read template " + path, ex);
                    }
                });
        }
    }
}
