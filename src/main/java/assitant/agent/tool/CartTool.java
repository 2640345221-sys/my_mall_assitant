package assitant.agent.tool;

import assitant.entity.dto.CartItemVO;
import assitant.entity.po.*;
import assitant.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CartTool {

    @Autowired
    private ShoppingCartMapper cartMapper;

    @Tool(description = "查看用户购物车，返回商品名/价格/数量等信息")
    public List<CartItemVO> getCart(@ToolParam(description = "用户ID") Long userId) {
        log.info("[CartTool] getCart: userId={}", userId);
        List<CartItemVO> result = cartMapper.findByUserIdWithGoods(userId);
        log.info("[CartTool] getCart: 返回 {} 件商品", result.size());
        return result;
    }

    @Tool(description = "将商品加入购物车。goodsId可从语义推荐的'ID:数字'中提取数字部分")
    public String addToCart(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "商品ID，从推荐的'ID:数字'中取数字") Long goodsId,
            @ToolParam(description = "数量，默认1") Integer count) {
        log.info("[CartTool] addToCart: userId={}, goodsId={}, count={}", userId, goodsId, count);
        ShoppingCart exist = cartMapper.findByUserIdAndGoodsId(userId, goodsId);
        if (exist != null) {
            cartMapper.updateCount(exist.getId(), exist.getGoodsCount() + count);
            log.info("[CartTool] addToCart: 已存在，数量增加");
            return "购物车中已有此商品，数量已增加";
        }
        ShoppingCart cart = ShoppingCart.builder()
                .userId(userId).goodsId(goodsId)
                .goodsCount(count != null ? count : 1).build();
        cartMapper.insert(cart);
        log.info("[CartTool] addToCart: 新增, cartId={}", cart.getId());
        return "已加入购物车";
    }

    @Tool(description = "删除购物车中指定条目。cartItemId从getCart返回中获取")
    public String removeFromCart(@ToolParam(description = "购物车条目ID，从getCart返回中获取") Long cartItemId) {
        log.info("[CartTool] removeFromCart: cartItemId={}", cartItemId);
        cartMapper.deleteById(cartItemId);
        return "已从购物车删除";
    }

    @Tool(description = "修改购物车中商品数量。cartItemId从getCart返回中获取")
    public String updateCartQuantity(
            @ToolParam(description = "购物车条目ID，从getCart返回中获取") Long cartItemId,
            @ToolParam(description = "新的数量") Integer count) {
        log.info("[CartTool] updateCartQuantity: cartItemId={}, count={}", cartItemId, count);
        cartMapper.updateCount(cartItemId, count);
        return "数量已更新";
    }
}
