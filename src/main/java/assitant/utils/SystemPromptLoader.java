package assitant.utils;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Slf4j
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
            // 加载主提示词
            StringBuilder sb = new StringBuilder();
            Resource main = resourceLoader.getResource("classpath:prompts/system-prompt.txt");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(main.getInputStream(), StandardCharsets.UTF_8))) {
                sb.append(reader.lines().collect(Collectors.joining("\n")));
            }
            // 扫描并附加所有 skill 文件
            Resource[] skillResources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:prompts/skills/*.txt");
            for (Resource skill : skillResources) {
                sb.append("\n\n");
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(skill.getInputStream(), StandardCharsets.UTF_8))) {
                    sb.append(reader.lines().collect(Collectors.joining("\n")));
                }
            }
            sb.insert(0, "当前时间是 " + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")) + "。\n\n");
            systemPrompt = sb.toString();
            log.info("[SystemPrompt] 加载完成, 主提示词 + {} 个skill", skillResources.length);
        } catch (IOException e) {
            throw new RuntimeException("加载提示词失败", e);
        }
    }
}
