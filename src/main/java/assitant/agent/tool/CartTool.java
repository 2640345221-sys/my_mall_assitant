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
import java.util.stream.Collectors;

@Service
public class CartTool {

    @Autowired
    private ShoppingCartMapper cartMapper;

    @Tool(description = "查看购物车。cartId是内部标识，展示给用户时只显示商品名/数量/单价，不要显示cartId。但记住cartId用于后续下单")
    @OperationLog(module = "Agent", type = "查询", description = "查看购物车", recordParams = true)
    public String getCart(@ToolParam(description = "用户ID") Long userId) {
        List<CartItemVO> list = cartMapper.findByUserIdWithGoods(userId);
        if (list.isEmpty()) return "购物车为空";
        return list.stream()
                .map(item -> "cartId:" + item.getCartId() + " goodsId:" + item.getGoodsId()
                        + " 商品:" + item.getGoodsName() + " 数量:" + item.getGoodsCount()
                        + "件 单价:" + item.getSellingPrice() / 100.0 + "元")
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "加入购物车。触发时机：用户确认加购后调用。goodsId从getByName或上下文获取。count为用户指定数量")
    @OperationLog(module = "Agent", type = "写入", description = "加入购物车", recordParams = true)
    public String addToCart(Long userId, Long goodsId, Integer count) {
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

    @Tool(description = "删除购物车条目。触发时机：用户说'删除''不要了'并确认后调用。cartItemId从getCart返回中获取")
    @OperationLog(module = "Agent", type = "删除", description = "删除购物车条目", recordParams = true)
    public String removeFromCart(Long cartItemId) {
        cartMapper.deleteById(cartItemId);
        return "已从购物车删除";
    }

    @Tool(description = "修改购物车数量。触发时机：用户说'改成3件'时调用。cartItemId从getCart返回中获取")
    @OperationLog(module = "Agent", type = "修改", description = "修改购物车数量", recordParams = true)
    public String updateCartQuantity(Long cartItemId, Integer count) {
        cartMapper.updateCount(cartItemId, count);
        return "数量已更新";
    }
}
