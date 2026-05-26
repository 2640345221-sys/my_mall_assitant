package assitant.mapper;

import assitant.entity.po.UserAddress;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserAddressMapper {

    List<UserAddress> findByUserId(Long userId);

    UserAddress findDefaultByUserId(Long userId);

    int insert(UserAddress address);

    int update(UserAddress address);
}
