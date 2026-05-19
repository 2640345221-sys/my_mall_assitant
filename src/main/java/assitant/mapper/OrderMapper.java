package assitant.mapper;

import assitant.entity.po.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Select("SELECT * FROM my_mall.`order` WHERE id = #{id}")
    Order findById(Long id);

    @Select("SELECT * FROM my_mall.`order` WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    @Insert("INSERT INTO my_mall.`order`(order_no, user_id, total_price, pay_status, pay_type, order_status) " +
            "VALUES(#{orderNo}, #{userId}, #{totalPrice}, #{payStatus}, #{payType}, #{orderStatus})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Update("UPDATE my_mall.`order` SET order_status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
