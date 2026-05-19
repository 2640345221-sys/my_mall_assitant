package assitant.mapper;

import assitant.entity.dto.StockDeductDTO;
import assitant.entity.po.Goods;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GoodsMapper {

    @Select("SELECT * FROM my_mall.goods WHERE id = #{id}")
    Goods findById(Long id);

    @Select("SELECT * FROM my_mall.goods WHERE name LIKE CONCAT('%', #{keyword}, '%')")
    List<Goods> searchByKeyword(String keyword);

    @Select("SELECT * FROM my_mall.goods WHERE category_id = #{categoryId}")
    List<Goods> findByCategoryId(Long categoryId);

    @Select("SELECT * FROM my_mall.goods WHERE selling_price BETWEEN #{minPrice} AND #{maxPrice}")
    List<Goods> findByPriceRange(Integer minPrice, Integer maxPrice);

    List<Goods> searchByKeywordAndPriceRange(@Param("keyword") String keyword,
                                             @Param("minPrice") Integer minPrice,
                                             @Param("maxPrice") Integer maxPrice);

    @Select("SELECT * FROM my_mall.goods WHERE sell_status = 0 ORDER BY create_time DESC LIMIT #{limit}")
    List<Goods> findLatest(Integer limit);

    @Select("select * from my_mall.goods where sell_status=0")
    List<Goods> getAll();

    int deductStock(List<StockDeductDTO> list);

    int recoverStock(List<StockDeductDTO> list);
    @Select("select * from my_mall.goods where sell_status=0")
    List<Goods> getSelling();
}
