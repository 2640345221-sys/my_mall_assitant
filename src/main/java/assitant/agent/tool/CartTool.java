package assitant.agent.tool;

import assitant.annotation.OperationLog;
import assitant.entity.dto.CartItemVO;
import assitant.entity.po.*;
import assitant.mapper.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartTool {

    @Resource
    private ShoppingCartMapper cartMapper;

    @Tool(description = "查看用户购物车。返回格式: cartId:XXX goodsId:XXX 商品:XXX 数量:XXX件 单价:XXX元，不需要给用户展示ID信息。cartId用于修改/删除购物车条目，goodsId用于创建订单。展示给用户时只显示商品名/数量/单价")
    @OperationLog(module = "Agent", type = "查询", description = "查看购物车")
    public String getCart(@ToolParam(description = "用户ID") Long userId) {
        List<CartItemVO> list = cartMapper.findByUserIdWithGoods(userId);
        if (list.isEmpty()) return "购物车为空";
        return list.stream()
                .map(item -> "cartId:" + item.getCartId() + " goodsId:" + item.getGoodsId()
                        + " 商品:" + item.getGoodsName() + " 数量:" + item.getGoodsCount()
                        + "件 单价:" + item.getSellingPrice() / 100.0 + "元")
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "将商品加入购物车。goodsId 必须来自 semanticRecommend/getByName/getByCategory/getLatest 返回结果中的ID，绝对禁止编造。加购前必须先向用户展示商品并确认数量")
    @OperationLog(module = "Agent", type = "写入", description = "加入购物车")
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

    @Tool(description = "删除购物车条目。cartItemId 必须且只能从 getCart 返回结果中的 cartId 获取，禁止编造。删除前须获得用户确认")
    @OperationLog(module = "Agent", type = "删除", description = "删除购物车条目")
    public String removeFromCart(Long cartItemId) {
        cartMapper.deleteById(cartItemId);
        return "已从购物车删除";
    }

    @Tool(description = "修改购物车商品数量。cartItemId 必须且只能从 getCart 返回结果中的 cartId 获取")
    @OperationLog(module = "Agent", type = "修改", description = "修改购物车数量")
    public String updateCartQuantity(Long cartItemId, Integer count) {
        cartMapper.updateCount(cartItemId, count);
        return "数量已更新";
    }
}
