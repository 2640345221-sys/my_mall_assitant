package assitant.mapper;

import assitant.entity.dto.CartItemVO;
import assitant.entity.po.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    List<CartItemVO> findByUserIdWithGoods(Long userId);

    List<ShoppingCart> findByUserId(Long userId);

    ShoppingCart findByUserIdAndGoodsId(@Param("userId") Long userId, @Param("goodsId") Long goodsId);

    int insert(ShoppingCart cart);

    int deleteById(Long id);

    int updateCount(@Param("id") Long id, @Param("count") Integer count);
}
