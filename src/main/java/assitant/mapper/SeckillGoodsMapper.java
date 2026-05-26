package assitant.mapper;

import assitant.entity.po.SeckillGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SeckillGoodsMapper {

    SeckillGoods findById(Long id);

    List<SeckillGoods> findActiveSales(LocalDateTime now);

    int decreaseStock(@Param("id") Long id, @Param("count") Integer count);
}
