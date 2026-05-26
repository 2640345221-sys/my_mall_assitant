package assitant.mapper;

import assitant.entity.po.OrderItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    List<OrderItem> findByOrderId(Long orderId);

    int insert(OrderItem item);

    int deleteByOrderId(Long orderId);
}
