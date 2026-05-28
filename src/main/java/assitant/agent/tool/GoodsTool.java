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
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoodsTool {

    @Resource
    private GoodsMapper goodsMapper;
    @Resource
    private GoodsCategoryMapper categoryMapper;
    @Resource
    @Qualifier("goodsVectorStore")
    private VectorStore vectorStore;
    @Resource
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
        }
        log.info("向量索引完成, 共{}件商品", goodsList.size());
    }

    @OperationLog(module = "RAG", type = "搜索", description = "向量语义搜索")
    public List<Document> getGoodsList(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(6).similarityThreshold(0.3).build());
    }

    @Tool(description = "搜索推荐商品。当用户提到购买需求、使用场景、送礼物、找某类商品时必须先调用此方法。返回格式: ID:XXX 商品名 | 简介 | 价格 | 库存。后续如需详情或加购，必须使用返回结果中的ID，绝对禁止编造ID")
    @OperationLog(module = "Agent", type = "查询", description = "语义推荐", recordResult = true)
    public String semanticRecommend(
            @ToolParam(description = "用户的购买需求或使用场景") String query) {
        List<Document> docs = getGoodsList(query);
        if (docs.isEmpty()) return "未找到匹配的商品推荐";
        StringBuilder sb = new StringBuilder();
        for (Document doc : docs) {
            String goodsId = null;
            if (doc.getText() != null) {
                int s = doc.getText().indexOf("【ID:");
                int e = doc.getText().indexOf("】");
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

    @Tool(description = "获取单个商品详情。goodsId必须来自semanticRecommend/getByCategory/getLatest/getByName返回的ID，禁止编造。如果ID无效或查不到，必须重新调用semanticRecommend或getByName搜索")
    @OperationLog(module = "Agent", type = "查询", description = "商品详情")
    public String getDetail(Long goodsId) {
        Goods g = goodsMapper.findById(goodsId);
        if (g == null) return "商品不存在，请调用 semanticRecommend 或 getByName 重新搜索";
        return "商品:" + g.getName() + " | 价格:" + g.getSellingPrice() / 100.0 + "元 | 库存:"
                + g.getStockNum() + "件 | 简介:" + (g.getIntro() != null ? g.getIntro() : "")
                + " | 详情:" + (g.getDetailContent() != null ? g.getDetailContent() : "");
    }

    @Tool(description = "对比多个商品的参数。goodsIds至少2个，必须来自semanticRecommend/getByCategory/getLatest/getByName返回的ID，禁止编造。返回并排对比表")
    @OperationLog(module = "Agent", type = "查询", description = "对比商品")
    public String compare(
            @ToolParam(description = "要对比的商品ID列表") List<Long> goodsIds) {
        if (goodsIds == null || goodsIds.size() < 2) return "至少需要2个商品ID进行对比";
        StringBuilder sb = new StringBuilder();
        sb.append("商品ID | 名称 | 价格 | 库存 | 简介\n");
        for (Long id : goodsIds) {
            Goods g = goodsMapper.findById(id);
            if (g == null) continue;
            sb.append(g.getId()).append(" | ").append(g.getName())
              .append(" | ").append(g.getSellingPrice() / 100.0).append("元")
              .append(" | ").append(g.getStockNum()).append("件")
              .append(" | ").append(g.getIntro() != null ? g.getIntro() : "").append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "根据商品名称关键词搜索。当用户提到具体商品名或你需要通过名称找商品ID时调用。返回格式: ID:XXX 商品名 | 简介 | 价格 | 库存")
    @OperationLog(module = "Agent", type = "查询", description = "按名称搜索")
    public String getByName(
            @ToolParam(description = "商品名称关键词") String name) {
        List<Goods> list = goodsMapper.searchByKeyword(name);
        if (list.isEmpty()) return "未找到名称包含「" + name + "」的商品，建议调 semanticRecommend 搜索";
        return list.stream()
                .map(g -> "ID:" + g.getId() + " " + g.getName()
                        + " | " + (g.getIntro() != null ? g.getIntro() : "")
                        + " | 价格:" + g.getSellingPrice() / 100.0 + "元"
                        + " | 库存:" + g.getStockNum() + "件")
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "获取全部商品分类树。返回格式: 分类ID:名称(层级N)。当用户想浏览分类或你不知道有哪些分类时调用。后续按分类查商品时，categoryId 必须从本方法返回结果中取")
    @OperationLog(module = "Agent", type = "查询", description = "商品分类")
    public String getCategories() {
        List<GoodsCategory> list = categoryMapper.findAll();
        return list.stream()
                .map(c -> c.getId() + ":" + c.getName() + "(level" + c.getLevel() + ")")
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "按分类ID获取商品列表。categoryId 必须从 getCategories 返回结果中获取，禁止编造。返回格式: ID:XXX 商品名 | 价格 | 库存")
    @OperationLog(module = "Agent", type = "查询", description = "按分类获取商品")
    public String getByCategory(Long categoryId) {
        List<Goods> result = goodsMapper.findByCategoryId(categoryId);
        if (!result.isEmpty()) return formatGoodsList(result);
        List<GoodsCategory> children = categoryMapper.findByParentId(categoryId);
        if (children.isEmpty()) return "该分类下暂无商品";
        List<Goods> all = new ArrayList<>();
        for (GoodsCategory child : children) {
            List<Goods> sub = goodsMapper.findByCategoryId(child.getId());
            if (sub != null && !sub.isEmpty()) {
                all.addAll(sub);
            } else {
                for (GoodsCategory gc : categoryMapper.findByParentId(child.getId())) {
                    List<Goods> gcs = goodsMapper.findByCategoryId(gc.getId());
                    if (gcs != null) all.addAll(gcs);
                }
            }
        }
        return all.isEmpty() ? "该分类下暂无商品" : formatGoodsList(all);
    }

    @Tool(description = "获取最新上架商品。用户说最新之类的词汇时调用。返回格式: ID:XXX 商品名 | 价格 | 库存")
    @OperationLog(module = "Agent", type = "查询", description = "最新上架")
    public String getLatest(Integer limit) {
        List<Goods> list= goodsMapper.findLatest(limit);
        return formatGoodsList(goodsMapper.findLatest(limit));
    }

    private String formatGoodsList(List<Goods> list) {
        return list.stream()
                .map(g -> "ID:" + g.getId() + " " + g.getName()
                        + " | " + (g.getIntro() != null ? g.getIntro() : "")
                        + " | 价格:" + g.getSellingPrice() / 100.0 + "元"
                        + " | 库存:" + g.getStockNum() + "件")
                .collect(Collectors.joining("\n"));
    }
}
