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

    @Tool(name = "createOrder", description = "为购物车中指定商品下单。goodsIds是商品编号列表，从推荐列表或getCart返回中获取。addressId从getUserAddresses返回中获取。内部自动查找对应的购物车条目")
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = "Agent", type = "写入", description = "创建订单", recordParams = true)
    public String createOrder(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "收货地址ID，从getUserAddresses返回中获取") Long addressId,
            @ToolParam(description = "要下单的商品编号列表，如[10927, 10928]") List<Long> goodsIds) {
        List<ShoppingCart> allItems = cartMapper.findByUserId(userId);
        List<ShoppingCart> cartItems = allItems.stream()
                .filter(c -> goodsIds.contains(c.getGoodsId())).toList();
        if (cartItems.isEmpty()) return "购物车中没有这些商品，请先加入购物车";
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
        for (ShoppingCart c : cartItems) cartMapper.deleteById(c.getId());  // 只删选中的
        StringBuilder detail = new StringBuilder();
        for (ShoppingCart item : cartItems) {
            Goods g = goodsMapper.findById(item.getGoodsId());
            if (g != null) detail.append(g.getName()).append("x").append(item.getGoodsCount()).append(" ");
        }
        return "下单成功 | 订单号:" + orderNo + " | 收件人:" + addr.getUsername()
                + " | 电话:" + addr.getUserPhone() + " | 地址:" + addr.getProvince()
                + addr.getCity() + addr.getRegion() + addr.getDetailAddress()
                + " | 商品:" + detail.toString().trim()
                + " | 金额:" + total / 100.0 + "元 | 状态:待支付";
    }

    @Tool(description = "查询用户订单列表。触发时机：用户说'我的订单''订单列表'时调用。status可选：0待支付1已支付2已发货3已完成4已取消，不传查全部")
    @OperationLog(module = "Agent", type = "查询", description = "订单列表", recordParams = true)
    public String getOrderList(Long userId, Integer status) {
        List<Order> list;
        if (status != null) list = orderMapper.findByUserIdAndStatus(userId, status);
        else list = orderMapper.findByUserId(userId);
        if (list.isEmpty()) return "暂无订单";
        StringBuilder sb = new StringBuilder();
        for (Order o : list) {
            List<OrderItem> items = orderItemMapper.findByOrderId(o.getId());
            String products = items.stream()
                    .map(i -> i.getGoodsName() + "x" + i.getCount())
                    .reduce((a, b) -> a + " " + b).orElse("");
            String st = switch (o.getOrderStatus() != null ? o.getOrderStatus() : -1) {
                case 0 -> "待支付"; case 1 -> "已支付"; case 2 -> "已发货";
                case 3 -> "已完成"; case 4 -> "已取消"; default -> "未知"; };
            sb.append("订单ID:").append(o.getId()).append(" 单号:").append(o.getOrderNo())
              .append(" 商品:").append(products)
              .append(" 金额:").append(o.getTotalPrice() / 100.0).append("元")
              .append(" 状态:").append(st).append(" 时间:").append(o.getCreateTime()).append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "获取订单详情及商品明细。触发时机：用户选定某个订单查看时调用。orderId从getOrderList返回中获取")
    @OperationLog(module = "Agent", type = "查询", description = "订单详情", recordParams = true)
    public String getOrderDetail(Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) return "订单不存在";
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        StringBuilder sb = new StringBuilder();
        sb.append("订单号:").append(order.getOrderNo())
          .append(" 金额:").append(order.getTotalPrice() / 100.0).append("元")
          .append(" 状态:").append(order.getOrderStatus()).append("\n商品明细:\n");
        for (OrderItem item : items) {
            sb.append("- ").append(item.getGoodsName()).append(" ×").append(item.getCount())
              .append(" 单价:").append(item.getPrice() / 100.0).append("元\n");
        }
        return sb.toString();
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
        orderItemMapper.deleteByOrderId(orderId);
        orderAddressMapper.deleteByOrderId(orderId);
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
