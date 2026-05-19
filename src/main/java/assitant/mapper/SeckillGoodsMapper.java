package assitant.mapper;

import assitant.entity.po.SeckillGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SeckillGoodsMapper {

    @Select("SELECT * FROM my_mall.seckill_goods WHERE id = #{id}")
    SeckillGoods findById(Long id);

    @Select("SELECT * FROM my_mall.seckill_goods WHERE status = 1 AND start_time <= #{now} AND end_time >= #{now}")
    List<SeckillGoods> findActiveSales(LocalDateTime now);

    @org.apache.ibatis.annotations.Update(
        "UPDATE my_mall.seckill_goods SET stock_count = stock_count - #{count} WHERE id = #{id} AND stock_count >= #{count}")
    int decreaseStock(@org.apache.ibatis.annotations.Param("id") Long id,
                      @org.apache.ibatis.annotations.Param("count") Integer count);
}
