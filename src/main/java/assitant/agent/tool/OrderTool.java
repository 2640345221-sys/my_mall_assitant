package assitant.agent.tool;

import assitant.annotation.OperationLog;
import assitant.entity.dto.OrderDetailVO;
import assitant.entity.dto.StockDeductDTO;
import assitant.entity.po.*;
import assitant.mapper.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

    @Tool(name = "createOrder", description = "为购物车中全部商品创建订单。调用前必须先确认用户要买哪些商品（全下还是部分）。addressId从getUserAddresses返回中获取。返回订单号和总金额")
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = "Agent", type = "写入", description = "创建订单", recordParams = true)
    public String createOrder(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "收货地址ID，从getUserAddresses返回中获取") Long addressId) {
        List<ShoppingCart> cartItems = cartMapper.findByUserId(userId);
        if (cartItems.isEmpty()) return "购物车为空，无法下单";
        UserAddress addr = userAddressMapper.findByUserId(userId).stream()
                .filter(a -> a.getId().equals(addressId)).findFirst().orElse(null);
        if (addr == null) return "收货地址不存在";
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
        return "下单成功，订单号：" + orderNo + "，总金额：" + total / 100.0 + "元，共" + deductList.size() + "件商品";
    }

    @Tool(description = "查询用户订单列表。status:0待支付1已支付2已发货3已完成4已取消，不传查全部")
    @OperationLog(module = "Agent", type = "查询", description = "订单列表", recordParams = true)
    public List<Order> getOrderList(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "订单状态筛选：0待支付1已支付2已发货3已完成4已取消，不传查全部") Integer status) {
        List<Order> list;
        if (status != null) list = orderMapper.findByUserIdAndStatus(userId, status);
        else list = orderMapper.findByUserId(userId);
        list.forEach(o -> o.setTotalPrice(o.getTotalPrice() / 100));
        return list;
    }

    @Tool(description = "获取订单详情，包含商品明细")
    @OperationLog(module = "Agent", type = "查询", description = "订单详情", recordParams = true)
    public OrderDetailVO getOrderDetail(@ToolParam(description = "订单ID，从getOrderList返回中获取") Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) return null;
        order.setTotalPrice(order.getTotalPrice() / 100);
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        items.forEach(i -> i.setPrice(i.getPrice() / 100));
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(order);
        vo.setItems(items);
        return vo;
    }

    @Tool(description = "取消订单，会恢复商品库存。只能取消待支付(0)和已支付(1)的订单")
    @OperationLog(module = "Agent", type = "删除", description = "取消订单", recordParams = true)
    @Transactional
    public String cancelOrder(@ToolParam(description = "订单ID，从getOrderList返回中获取") Long orderId) {
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

    @Tool(description = "消费统计。统计用户已完成订单(状态3)的总金额、订单数、均价。period可选: today/yesterday/this_week/last_week/this_month/last_month/last_3_months/this_year，不传查全部")
    @OperationLog(module = "Agent", type = "查询", description = "消费统计", recordParams = true)
    public String getStats(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "时间范围: today/yesterday/this_week/last_week/this_month/last_month/last_3_months/this_year，不传查全部") String period) {
        java.time.LocalDateTime[] range = assitant.utils.DateUtils.parsePeriod(period);
        List<Order> orders = orderMapper.findByUserStatusAndTime(userId, 3,
                range != null ? range[0] : null, range != null ? range[1] : null);
        if (orders.isEmpty()) return "暂无已完成订单";

        int total = orders.stream().mapToInt(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0).sum();
        int count = orders.size();
        String periodLabel = period != null ? period : "全部";
        return "【" + periodLabel + "消费统计】\n"
                + "已完成订单数: " + count + " 笔\n"
                + "消费总额: " + total / 100.0 + " 元";
    }
}
