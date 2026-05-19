package assitant.mapper;

import assitant.entity.dto.CartItemVO;
import assitant.entity.po.ShoppingCart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    List<CartItemVO> findByUserIdWithGoods(Long userId);

    @Select("SELECT * FROM my_mall.shopping_cart WHERE user_id = #{userId}")
    List<ShoppingCart> findByUserId(Long userId);

    @Select("SELECT * FROM my_mall.shopping_cart WHERE user_id = #{userId} AND goods_id = #{goodsId}")
    ShoppingCart findByUserIdAndGoodsId(@Param("userId") Long userId, @Param("goodsId") Long goodsId);

    @Insert("INSERT INTO my_mall.shopping_cart(user_id, goods_id, goods_count) VALUES(#{userId}, #{goodsId}, #{goodsCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ShoppingCart cart);

    @Delete("DELETE FROM my_mall.shopping_cart WHERE id = #{id}")
    int deleteById(Long id);

    @Update("UPDATE my_mall.shopping_cart SET goods_count = #{count} WHERE id = #{id}")
    int updateCount(@Param("id") Long id, @Param("count") Integer count);
}
