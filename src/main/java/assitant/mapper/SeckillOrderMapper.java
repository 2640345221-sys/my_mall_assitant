package assitant.mapper;

import assitant.entity.po.SeckillOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface SeckillOrderMapper {

    @org.apache.ibatis.annotations.Select(
        "SELECT COUNT(*) FROM my_mall.seckill_order WHERE user_id = #{userId} AND goods_id = #{goodsId}")
    int countByUserIdAndGoodsId(@org.apache.ibatis.annotations.Param("userId") Long userId,
                                @org.apache.ibatis.annotations.Param("goodsId") Long goodsId);

    @Insert("INSERT INTO my_mall.seckill_order(user_id, goods_id, order_id, status) " +
            "VALUES(#{userId}, #{goodsId}, #{orderId}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SeckillOrder order);
}
