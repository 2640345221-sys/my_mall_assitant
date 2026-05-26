package assitant.mapper;

import assitant.entity.dto.StockDeductDTO;
import assitant.entity.po.Goods;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GoodsMapper {

    Goods findById(Long id);

    List<Goods> searchByKeyword(String keyword);

    List<Goods> findByCategoryId(Long categoryId);

    List<Goods> findByPriceRange(Integer minPrice, Integer maxPrice);

    List<Goods> searchByKeywordAndPriceRange(@Param("keyword") String keyword,
                                             @Param("minPrice") Integer minPrice,
                                             @Param("maxPrice") Integer maxPrice);

    List<Goods> findLatest(Integer limit);

    List<Goods> getAll();

    int deductStock(List<StockDeductDTO> list);

    int recoverStock(List<StockDeductDTO> list);
    List<Goods> getSelling();
}
