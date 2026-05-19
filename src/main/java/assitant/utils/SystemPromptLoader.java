package assitant.utils;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class SystemPromptLoader {

    private final ResourceLoader resourceLoader;

    public SystemPromptLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Getter
    private String systemPrompt;

    @PostConstruct
    public void init() {
        try {
            Resource resource = resourceLoader.getResource("classpath:prompts/system-prompt.txt");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                systemPrompt = reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new RuntimeException("加载 system-prompt.txt 失败", e);
        }
    }
}
