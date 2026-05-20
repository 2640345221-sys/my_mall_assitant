package assitant.agent.tool;

import assitant.annotation.OperationLog;
import assitant.entity.dto.CartItemVO;
import assitant.entity.po.*;
import assitant.mapper.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartTool {

    @Autowired
    private ShoppingCartMapper cartMapper;

    @Tool(description = "查看用户购物车，返回购物ID，商品id，商品数量，商品名称，售价等信息")
    @OperationLog(module = "Agent", type = "查询", description = "查看购物车", recordParams = true)
    public List<CartItemVO> getCart(@ToolParam(description = "用户ID") Long userId) {
        List<CartItemVO> list = cartMapper.findByUserIdWithGoods(userId);
        list.forEach(item -> item.setSellingPrice(item.getSellingPrice() / 100));
        return list;
    }

    @Tool(description = "将商品加入购物车。goodsId可从商品推荐的'商品ID'中提取数字部分")
    @OperationLog(module = "Agent", type = "写入", description = "加入购物车", recordParams = true)
    public String addToCart(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "商品ID，从商品推荐的'商品ID'中取数字") Long goodsId,
            @ToolParam(description = "数量，默认1除非用户要求") Integer count) {
        ShoppingCart exist = cartMapper.findByUserIdAndGoodsId(userId, goodsId);
        if (exist != null) {
            cartMapper.updateCount(exist.getId(), exist.getGoodsCount() + count);
            return "购物车中已有此商品，数量已增加";
        }
        ShoppingCart cart = ShoppingCart.builder()
                .userId(userId).goodsId(goodsId)
                .goodsCount(count != null ? count : 1).build();
        cartMapper.insert(cart);
        return "已加入购物车";
    }

    @Tool(description = "删除购物车中指定条目。cartItemId从getCart返回中获取")
    @OperationLog(module = "Agent", type = "删除", description = "删除购物车条目", recordParams = true)
    public String removeFromCart(@ToolParam(description = "购物车条目ID，从getCart返回中获取") Long cartItemId) {
        cartMapper.deleteById(cartItemId);
        return "已从购物车删除";
    }

    @Tool(description = "修改购物车中商品数量。cartItemId从getCart返回中获取")
    @OperationLog(module = "Agent", type = "修改", description = "修改购物车数量", recordParams = true)
    public String updateCartQuantity(
            @ToolParam(description = "购物车条目ID，从getCart返回中获取") Long cartItemId,
            @ToolParam(description = "新的数量") Integer count) {
        cartMapper.updateCount(cartItemId, count);
        return "数量已更新";
    }
}
