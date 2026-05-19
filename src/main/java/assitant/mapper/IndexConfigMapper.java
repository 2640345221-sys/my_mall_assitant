package assitant.mapper;

import assitant.entity.po.IndexConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface IndexConfigMapper {

    @Select("SELECT * FROM my_mall.index_config ORDER BY `rank`")
    List<IndexConfig> findAll();

    @Select("SELECT * FROM my_mall.index_config WHERE type = #{type} ORDER BY `rank`")
    List<IndexConfig> findByType(Integer type);
}
