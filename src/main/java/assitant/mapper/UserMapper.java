package assitant.mapper;

import assitant.entity.po.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    User findById(Long id);

    User findByLoginName(String loginName);
}
