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

    @Tool(description = "获取正在进行的秒杀活动。触发时机：用户说'秒杀''限时'时调用")
    @OperationLog(module = "Agent", type = "查询", description = "秒杀活动列表")
    public String getActiveList() {
        List<SeckillGoods> list = seckillGoodsMapper.findActiveSales(LocalDateTime.now());
        if (list.isEmpty()) return "暂无秒杀活动";
        StringBuilder sb = new StringBuilder();
        for (SeckillGoods s : list) {
            sb.append("活动ID:").append(s.getId()).append(" 商品ID:").append(s.getGoodsId())
              .append(" 秒杀价:").append(s.getSeckillPrice()).append("元")
              .append(" 库存:").append(s.getStockCount())
              .append(" 时间:").append(s.getStartTime()).append("~").append(s.getEndTime()).append("\n");
        }
        return sb.toString();
    }
}
