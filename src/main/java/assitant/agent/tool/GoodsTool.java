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
import java.util.Map;

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
        log.info("清空旧向量数据");
        try {
            for (String key : jedisPooled.keys("goods:rag:*")) jedisPooled.del(key);
        } catch (Exception e) {
            log.warn("清空旧向量失败", e);
        }
        log.info("初始化商品向量");
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

    @Tool(description = "语义推荐商品，根据用户的使用场景或需求从知识库中匹配。返回格式: 商品的ID:数字 商品名 | 商品简介 | 商品价格:元 | 商品库存:件。可以用返回的'商品ID:数字'调getDetail查详情，或者用'商品ID:数字'调addToCart加入购物车")
    @OperationLog(module = "Agent", type = "查询", description = "语义推荐", recordParams = true, recordResult = true)
    public String semanticRecommend(
            @ToolParam(description = "用户的购买需求或使用场景，如'送女友的礼物'、'学生党手机'、'老年人用手机'、'办公好物'等") String query) {
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
              .append(" | 价格:").append(goods.getSellingPrice()).append("分")
              .append(" | 库存:").append(goods.getStockNum()).append("件\n");
        }
        sb.append("请根据以上信息为用户推荐最合适的商品。");
        return sb.toString();
    }

    @Tool(description = "根据商品ID获取商品完整信息，返回name/价格/库存/详情等")
    @OperationLog(module = "Agent", type = "查询", description = "商品详情", recordParams = true)
    public Goods getDetail(@ToolParam(description = "商品ID，从semanticRecommend返回的'ID:数字'中提取数字部分") Long goodsId) {
        return goodsMapper.findById(goodsId);
    }

    @Tool(description = "获取全部商品分类列表")
    @OperationLog(module = "Agent", type = "查询", description = "商品分类")
    public List<GoodsCategory> getCategories() {
        return categoryMapper.findAll();
    }

    @Tool(description = "按分类ID获取商品列表。如果该分类下无商品，自动查找子分类下的商品")
    @OperationLog(module = "Agent", type = "查询", description = "按分类获取商品", recordParams = true)
    public List<Goods> getByCategory(@ToolParam(description = "分类ID") Long categoryId) {
        List<Goods> result = goodsMapper.findByCategoryId(categoryId);
        if (!result.isEmpty()) return result;
        // 降级：level-1/2 分类下无商品，查所有 level-3 子分类
        List<GoodsCategory> children = categoryMapper.findByParentId(categoryId);
        if (children.isEmpty()) return result;
        List<Goods> all = new ArrayList<>();
        for (GoodsCategory child : children) {
            List<Goods> subResult = goodsMapper.findByCategoryId(child.getId());
            if (subResult != null) all.addAll(subResult);
            if (all.isEmpty()) {
                List<GoodsCategory> grandChildren = categoryMapper.findByParentId(child.getId());
                for (GoodsCategory gc : grandChildren) {
                    List<Goods> gcResult = goodsMapper.findByCategoryId(gc.getId());
                    if (gcResult != null) all.addAll(gcResult);
                }
            }
        }
        return all;
    }

    @Tool(description = "获取最新上架的商品")
    @OperationLog(module = "Agent", type = "查询", description = "最新上架", recordParams = true)
    public List<Goods> getLatest(@ToolParam(description = "返回数量") Integer limit) {
        return goodsMapper.findLatest(limit);
    }
}
