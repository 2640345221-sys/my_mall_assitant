package assitant.agent.tool;

import assitant.annotation.OperationLog;
import assitant.entity.po.*;
import assitant.mapper.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SeckillGoodsTool {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Tool(description = "获取正在进行的秒杀活动，返回活动ID/商品名/秒杀价/库存/时间段")
    @OperationLog(module = "Agent", type = "查询", description = "秒杀活动列表")
    public List<SeckillGoods> getActiveList() {
        return seckillGoodsMapper.findActiveSales(LocalDateTime.now());
    }
}
