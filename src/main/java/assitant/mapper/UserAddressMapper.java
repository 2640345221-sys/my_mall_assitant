package assitant.mapper;

import assitant.entity.po.UserAddress;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserAddressMapper {

    @Select("SELECT * FROM my_mall.user_address WHERE user_id = #{userId}")
    List<UserAddress> findByUserId(Long userId);

    @Select("SELECT * FROM my_mall.user_address WHERE user_id = #{userId} AND is_default = 1 LIMIT 1")
    UserAddress findDefaultByUserId(Long userId);

    @Insert("INSERT INTO my_mall.user_address(user_id, username, user_phone, is_default, province, city, region, detail_address) " +
            "VALUES(#{userId}, #{username}, #{userPhone}, #{isDefault}, #{province}, #{city}, #{region}, #{detailAddress})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAddress address);

    @Update("UPDATE my_mall.user_address SET is_default = #{isDefault}, username = #{username}, user_phone = #{userPhone}, " +
            "province = #{province}, city = #{city}, region = #{region}, detail_address = #{detailAddress} WHERE id = #{id}")
    int update(UserAddress address);
}
