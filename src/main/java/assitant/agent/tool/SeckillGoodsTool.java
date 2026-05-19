package assitant.agent.tool;

import assitant.entity.po.*;
import assitant.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SeckillGoodsTool {

    private static final String STOCK_KEY = "seckill:stock:%d";
    private static final String USER_KEY = "seckill:user:%d:%d";

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private final DefaultRedisScript<Long> seckillScript;

    public SeckillGoodsTool() {
        seckillScript = new DefaultRedisScript<>();
        seckillScript.setLocation(new org.springframework.core.io.ClassPathResource("lua/seckill.lua"));
        seckillScript.setResultType(Long.class);
    }

    @Tool(description = "获取正在进行的秒杀活动，返回活动ID/商品名/秒杀价/库存/时间段")
    public List<SeckillGoods> getActiveList() {
        log.info("[SeckillGoodsTool] getActiveList");
        List<SeckillGoods> result = seckillGoodsMapper.findActiveSales(LocalDateTime.now());
        log.info("[SeckillGoodsTool] getActiveList: 返回 {} 条", result.size());
        return result;
    }

    @Tool(name = "createSeckillOrder", description = "参与秒杀下单。seckillGoodsId从getActiveList返回的活动id字段获取。同一活动每人限购一次，Redis+Lua原子扣库存")
    public String createSeckillOrder(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "秒杀活动ID，从getActiveList返回的id获取") Long seckillGoodsId) {
        log.info("[SeckillGoodsTool] createSeckillOrder: userId={}, seckillGoodsId={}", userId, seckillGoodsId);
        SeckillGoods seckill = seckillGoodsMapper.findById(seckillGoodsId);
        if (seckill == null) return "秒杀活动不存在";
        if (seckill.getStatus() != 1) return "秒杀活动已结束";
        LocalDateTime now = LocalDateTime.now();
        if (seckill.getStartTime() != null && now.isBefore(seckill.getStartTime())) return "秒杀尚未开始";
        if (seckill.getEndTime() != null && now.isAfter(seckill.getEndTime())) return "秒杀已过期";

        String stockKey = String.format(STOCK_KEY, seckillGoodsId);
        if (Boolean.FALSE.equals(redisTemplate.hasKey(stockKey))) {
            int dbStock = seckill.getStockCount() != null ? seckill.getStockCount().intValue() : 0;
            redisTemplate.opsForValue().set(stockKey, String.valueOf(dbStock));
        }

        String userKey = String.format(USER_KEY, seckillGoodsId, userId);
        Long result = redisTemplate.execute(seckillScript, List.of(stockKey, userKey), "1", "3600");

        if (result == null) return "秒杀系统繁忙";
        int code = result.intValue();
        if (code >= 0) {
            int dbRows = seckillGoodsMapper.decreaseStock(seckillGoodsId, 1);
            if (dbRows == 0) {
                redisTemplate.opsForValue().increment(stockKey);
                redisTemplate.delete(userKey);
                return "库存不足";
            }
            SeckillOrder order = SeckillOrder.builder().userId(userId)
                    .goodsId(seckill.getGoodsId()).status(0).build();
            seckillOrderMapper.insert(order);
            log.info("[SeckillGoodsTool] createSeckillOrder: 成功, orderId={}, 剩余库存={}", order.getId(), code);
            return "秒杀下单成功，剩余库存：" + code;
        } else if (code == -1) return "库存不足";
        else if (code == -2) return "您已参与过该活动";
        else return "秒杀失败";
    }
}
