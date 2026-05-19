package assitant.mapper;

import assitant.entity.po.GoodsCategory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GoodsCategoryMapper {

    List<GoodsCategory> findAll();

    List<GoodsCategory> findByParentId(Long parentId);

    GoodsCategory findById(Long id);
}
