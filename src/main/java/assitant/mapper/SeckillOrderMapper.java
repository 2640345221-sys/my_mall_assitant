package assitant.mapper;

import assitant.entity.po.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SeckillOrderMapper {

    int countByUserIdAndGoodsId(@Param("userId") Long userId,
                                @Param("goodsId") Long goodsId);

    int insert(SeckillOrder order);
}
