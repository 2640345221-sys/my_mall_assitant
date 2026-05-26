package assitant.mapper;

import assitant.entity.po.OrderAddress;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderAddressMapper {

    int insert(OrderAddress address);

    int deleteByOrderId(Long orderId);

    OrderAddress findByOrderId(Long orderId);
}
