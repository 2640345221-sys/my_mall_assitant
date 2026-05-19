package assitant.mapper;

import assitant.entity.po.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM my_mall.user WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM my_mall.user WHERE login_name = #{loginName}")
    User findByLoginName(String loginName);
}
