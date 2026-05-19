package assitant.entity.dto;

import assitant.entity.po.Order;
import assitant.entity.po.OrderAddress;
import assitant.entity.po.OrderItem;
import lombok.Data;
import java.util.List;

@Data
public class OrderDetailVO {
    private Order order;
    private List<OrderItem> items;
    private OrderAddress address;
}
