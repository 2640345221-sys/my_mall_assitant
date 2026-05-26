package assitant.agent.tool;

import assitant.annotation.OperationLog;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FaqTool {

    @Resource
    @Qualifier("faqVectorStore")
    private VectorStore vectorStore;
    @Resource
    private ObjectMapper objectMapper;

    @Data
    public static class FaqEntry {
        private String question;
        private String answer;
    }

    @PostConstruct
    @OperationLog(module = "RAG", type = "初始化", description = "FAQ向量索引")
    public void init() {
        try {
            List<FaqEntry> entries = objectMapper.readValue(
                    new ClassPathResource("rag/faq.json").getInputStream(),
                    new TypeReference<List<FaqEntry>>() {});
            for (int i = 0; i < entries.size(); i += 10) {
                int end = Math.min(i + 10, entries.size());
                List<Document> batch = new ArrayList<>();
                for (int j = i; j < end; j++) {
                    batch.add(new Document(entries.get(j).getQuestion(),
                            Map.of("answer", entries.get(j).getAnswer())));
                }
                vectorStore.add(batch);
            }
        } catch (Exception e) {
            throw new RuntimeException("FAQ初始化失败", e);
        }
    }

    @Tool(description = "搜索FAQ知识库，回答退换货、发货、支付、售后等常见问题")
    @OperationLog(module = "Agent", type = "查询", description = "FAQ搜索")
    public String ask(@ToolParam(description = "用户的问题，如'怎么退货'、'几天到货'等") String query) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(2).similarityThreshold(0.3).build());
        if (docs.isEmpty()) return "抱歉，这个问题我暂时无法回答，建议联系人工客服。";

        StringBuilder sb = new StringBuilder("根据知识库为您找到以下相关信息：\n");
        for (Document doc : docs) {
            String answer = doc.getMetadata() != null ? (String) doc.getMetadata().get("answer") : null;
            if (answer == null && doc.getText() != null) answer = doc.getText();
            if (answer != null) sb.append(answer).append("\n");
        }
        sb.append("如仍有疑问，随时告诉我。");
        return sb.toString();
    }
}
