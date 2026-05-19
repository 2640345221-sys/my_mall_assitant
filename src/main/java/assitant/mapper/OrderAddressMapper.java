package assitant.mapper;

import assitant.entity.po.OrderAddress;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface OrderAddressMapper {

    @Insert("INSERT INTO my_mall.order_address(username, user_phone, province, city, region, detail_address, order_id) " +
            "VALUES(#{username}, #{userPhone}, #{province}, #{city}, #{region}, #{detailAddress}, #{orderId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrderAddress address);
}
