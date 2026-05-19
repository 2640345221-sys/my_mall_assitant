package assitant.mapper;

import assitant.entity.po.OrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    @Select("SELECT * FROM my_mall.order_item WHERE order_id = #{orderId}")
    List<OrderItem> findByOrderId(Long orderId);

    @Insert("INSERT INTO my_mall.order_item(order_id, goods_id, goods_name, cover_img, price, count) " +
            "VALUES(#{orderId}, #{goodsId}, #{goodsName}, #{coverImg}, #{price}, #{count})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrderItem item);
}
