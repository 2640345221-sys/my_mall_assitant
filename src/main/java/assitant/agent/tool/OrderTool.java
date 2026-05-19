package assitant.agent.tool;

import assitant.entity.dto.OrderDetailVO;
import assitant.entity.dto.StockDeductDTO;
import assitant.entity.po.*;
import assitant.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OrderTool {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderAddressMapper orderAddressMapper;
    @Autowired
    private ShoppingCartMapper cartMapper;
    @Autowired
    private UserAddressMapper userAddressMapper;
    @Autowired
    private GoodsMapper goodsMapper;

    @Tool(name = "createOrder", description = "从购物车创建订单。addressId从getUserAddresses返回的地址列表中获取。返回订单号和总金额")
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "收货地址ID，从getUserAddresses返回中获取") Long addressId) {
        log.info("[OrderTool] createOrder: userId={}, addressId={}", userId, addressId);
        List<ShoppingCart> cartItems = cartMapper.findByUserId(userId);
        if (cartItems.isEmpty()) {
            log.warn("[OrderTool] createOrder: 购物车为空");
            return "购物车为空，无法下单";
        }
        UserAddress addr = userAddressMapper.findByUserId(userId).stream()
                .filter(a -> a.getId().equals(addressId)).findFirst().orElse(null);
        if (addr == null) {
            log.warn("[OrderTool] createOrder: 地址不存在");
            return "收货地址不存在";
        }
        List<StockDeductDTO> deductList = new ArrayList<>();
        int total = 0;
        for (ShoppingCart item : cartItems) {
            Goods goods = goodsMapper.findById(item.getGoodsId());
            if (goods == null) return "商品不存在（ID=" + item.getGoodsId() + "）";
            if (goods.getSellStatus() != null && goods.getSellStatus())
                return "商品「" + goods.getName() + "」已下架";
            int count = item.getGoodsCount() != null ? item.getGoodsCount() : 1;
            if (goods.getStockNum() != null && count > goods.getStockNum())
                return "商品「" + goods.getName() + "」库存不足";
            total += goods.getSellingPrice() * count;
            deductList.add(new StockDeductDTO(goods.getId(), count));
            item.setGoodsCount(count);
        }
        goodsMapper.deductStock(deductList);
        String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + String.format("%03d", (int) (Math.random() * 1000));
        Order order = Order.builder().orderNo(orderNo).userId(userId).totalPrice(total)
                .payStatus(0).payType(0).orderStatus(0).extraInfo("").build();
        orderMapper.insert(order);
        OrderAddress orderAddr = OrderAddress.builder().username(addr.getUsername()).userPhone(addr.getUserPhone())
                .province(addr.getProvince()).city(addr.getCity()).region(addr.getRegion())
                .detailAddress(addr.getDetailAddress()).orderId(order.getId()).build();
        orderAddressMapper.insert(orderAddr);
        for (ShoppingCart item : cartItems) {
            Goods goods = goodsMapper.findById(item.getGoodsId());
            if (goods == null) continue;
            OrderItem oi = OrderItem.builder().orderId(order.getId()).goodsId(goods.getId())
                    .goodsName(goods.getName()).coverImg(goods.getCoverImg())
                    .price(goods.getSellingPrice()).count(item.getGoodsCount()).build();
            orderItemMapper.insert(oi);
        }
        for (ShoppingCart c : cartItems) cartMapper.deleteById(c.getId());
        // TODO [修复] 价格单位统一：数据库存分(fen)，但返回时应统一为元(yuan)。当前 total/100.0 变元，但工具返回的 Goods 对象里 sellingPrice 仍是分，LLM 可能混淆
        return "下单成功，订单号：" + orderNo + "，总金额：" + total / 100.0 + "元，共" + deductList.size() + "件商品";
    }

    @Tool(description = "查询用户订单列表。status:0待支付1已支付2已发货3已完成4已取消，不传查全部")
    public List<Order> getOrderList(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "订单状态筛选：0待支付1已支付2已发货3已完成4已取消，不传查全部") Integer status) {
        log.info("[OrderTool] getOrderList: userId={}, status={}", userId, status);
        if (status != null) return orderMapper.findByUserIdAndStatus(userId, status);
        return orderMapper.findByUserId(userId);
    }

    @Tool(description = "获取订单详情，包含商品明细")
    public OrderDetailVO getOrderDetail(@ToolParam(description = "订单ID，从getOrderList返回中获取") Long orderId) {
        log.info("[OrderTool] getOrderDetail: orderId={}", orderId);
        Order order = orderMapper.findById(orderId);
        if (order == null) return null;
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(order);
        vo.setItems(items);
        return vo;
    }

    @Tool(description = "取消订单，会恢复商品库存。只能取消待支付(0)和已支付(1)的订单")
    public String cancelOrder(@ToolParam(description = "订单ID，从getOrderList返回中获取") Long orderId) {
        log.info("[OrderTool] cancelOrder: orderId={}", orderId);
        Order order = orderMapper.findById(orderId);
        if (order == null) return "订单不存在";
        int status = order.getOrderStatus() != null ? order.getOrderStatus() : -1;
        if (status != 0 && status != 1) return "订单状态不允许取消（当前状态：" + status + "）";
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        List<StockDeductDTO> recoverList = items.stream()
                .map(item -> new StockDeductDTO(item.getGoodsId(), item.getCount())).toList();
        if (!recoverList.isEmpty()) goodsMapper.recoverStock(recoverList);
        orderMapper.updateStatus(orderId, 4);
        return "订单 " + order.getOrderNo() + " 已取消，库存已恢复";
    }
}
