package assitant.agent.tool;

import assitant.annotation.OperationLog;
import assitant.entity.po.*;
import assitant.mapper.*;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoodsTool {

    @Autowired
    private GoodsMapper goodsMapper;
    @Autowired
    private GoodsCategoryMapper categoryMapper;
    @Autowired
    @Qualifier("goodsVectorStore")
    private VectorStore vectorStore;
    @Autowired
    private JedisPooled jedisPooled;

    @PostConstruct
    @SneakyThrows
    @OperationLog(module = "RAG", type = "初始化", description = "在售商品向量索引")
    public void initVector() {
        try {
            for (String key : jedisPooled.keys("goods:rag:*")) jedisPooled.del(key);
        } catch (Exception e) {
            log.warn("清空旧向量失败", e);
        }
        List<Goods> goodsList = goodsMapper.getSelling();
        int count = 0;
        for (int i = 0; i < goodsList.size(); i += 10) {
            int end = Math.min(i + 10, goodsList.size());
            List<Document> batch = new ArrayList<>();
            for (Goods goods : goodsList.subList(i, end)) {
                String text = "【ID:" + goods.getId() + "】" + goods.getName() + " | "
                        + goods.getIntro() + " | " + goods.getDetailContent();
                batch.add(new Document(text));
            }
            vectorStore.add(batch);
            count += batch.size();
            log.info("向量索引进度: {}/{}", count, goodsList.size());
        }
        log.info("向量索引完成, 共{}件商品", count);
    }

    @OperationLog(module = "RAG", type = "搜索", description = "向量语义搜索", recordParams = true)
    public List<Document> getGoodsList(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(6).similarityThreshold(0.3).build());
    }

    @Tool(description = "语义推荐商品。触发时机：用户有模糊需求（送女友/学生党/办公）或精确搜索失败时调用。返回按相似度排序的商品列表")
    @OperationLog(module = "Agent", type = "查询", description = "语义推荐", recordParams = true, recordResult = true)
    public String semanticRecommend(
            @ToolParam(description = "用户的购买需求或使用场景") String query) {
        List<Document> docs = getGoodsList(query);
        if (docs.isEmpty()) return "未找到匹配的商品推荐";
        StringBuilder sb = new StringBuilder();
        for (Document doc : docs) {
            String goodsId = null;
            if (doc.getText() != null) {
                int s = doc.getText().indexOf("【ID:");
                int e = doc.getText().indexOf("】", s);
                if (s >= 0 && e > s) goodsId = doc.getText().substring(s + 4, e);
            }
            if (goodsId == null) continue;
            Goods goods = goodsMapper.findById(Long.parseLong(goodsId));
            if (goods == null) continue;
            sb.append("ID:").append(goodsId).append(" ").append(goods.getName())
              .append(" | ").append(goods.getIntro())
              .append(" | 价格:").append(goods.getSellingPrice() / 100.0).append("元")
              .append(" | 库存:").append(goods.getStockNum()).append("件\n");
        }
        return sb.toString();
    }

    @Tool(description = "根据商品ID获取详情。触发时机：用户说'第一个的详情''看看这个'时调用。goodsId从上下文或getByName返回中获取，不可编造")
    @OperationLog(module = "Agent", type = "查询", description = "商品详情", recordParams = true)
    public String getDetail(Long goodsId) {
        Goods g = goodsMapper.findById(goodsId);
        if (g == null) return "商品不存在";
        return "商品:" + g.getName() + " | 价格:" + g.getSellingPrice() / 100.0 + "元 | 库存:"
                + g.getStockNum() + "件 | 简介:" + (g.getIntro() != null ? g.getIntro() : "")
                + " | 详情:" + (g.getDetailContent() != null ? g.getDetailContent() : "");
    }

    @Tool(description = "获取商品分类列表，返回分类ID和名称")
    @OperationLog(module = "Agent", type = "查询", description = "商品分类")
    public String getCategories() {
        List<GoodsCategory> list = categoryMapper.findAll();
        return list.stream()
                .map(c -> c.getId() + ":" + c.getName() + "(level" + c.getLevel() + ")")
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "按分类ID获取商品列表。触发时机：用户说'手机分类''看看美妆'时调用")
    @OperationLog(module = "Agent", type = "查询", description = "按分类获取商品", recordParams = true)
    public String getByCategory(Long categoryId) {
        List<Goods> result = goodsMapper.findByCategoryId(categoryId);
        if (!result.isEmpty()) return formatGoodsList(result);
        List<GoodsCategory> children = categoryMapper.findByParentId(categoryId);
        if (children.isEmpty()) return "该分类下暂无商品";
        List<Goods> all = new ArrayList<>();
        for (GoodsCategory child : children) {
            List<Goods> sub = goodsMapper.findByCategoryId(child.getId());
            if (sub != null) all.addAll(sub);
            if (all.isEmpty()) {
                for (GoodsCategory gc : categoryMapper.findByParentId(child.getId())) {
                    List<Goods> gcs = goodsMapper.findByCategoryId(gc.getId());
                    if (gcs != null) all.addAll(gcs);
                }
            }
        }
        return all.isEmpty() ? "该分类下暂无商品" : formatGoodsList(all);
    }

    @Tool(description = "获取最新上架商品。触发时机：用户说'最新'、'新品'时调用")
    @OperationLog(module = "Agent", type = "查询", description = "最新上架", recordParams = true)
    public String getLatest(Integer limit) {
        return formatGoodsList(goodsMapper.findLatest(limit));
    }

    private String formatGoodsList(List<Goods> list) {
        return list.stream()
                .map(g -> "ID:" + g.getId() + " " + g.getName() + " | 价格:" + g.getSellingPrice() / 100.0
                        + "元 | 库存:" + g.getStockNum() + "件")
                .collect(Collectors.joining("\n"));
    }
}
