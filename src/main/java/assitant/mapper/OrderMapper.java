package assitant.mapper;

import assitant.entity.po.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    Order findById(Long id);

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    List<Order> findByUserStatusAndTime(@Param("userId") Long userId,
                                        @Param("status") Integer status,
                                        @Param("startTime") java.time.LocalDateTime startTime,
                                        @Param("endTime") java.time.LocalDateTime endTime);

    int insert(Order order);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

}
