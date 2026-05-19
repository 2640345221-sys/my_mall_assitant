package assitant.agent.tool;

import assitant.entity.po.*;
import assitant.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserTool {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserAddressMapper addressMapper;

    @Tool(description = "获取用户个人信息，返回昵称/登录名等，密码已脱敏")
    public User getUserProfile(@ToolParam(description = "用户ID") Long userId) {
        log.info("[UserTool] getUserProfile: userId={}", userId);
        User user = userMapper.findById(userId);
        if (user != null) user.setPassword(null);
        log.info("[UserTool] getUserProfile: {}", user != null ? user.getNickName() : "null");
        return user;
    }

    @Tool(description = "获取用户所有收货地址。返回的地址ID可用于createOrder下单")
    public List<UserAddress> getUserAddresses(@ToolParam(description = "用户ID") Long userId) {
        log.info("[UserTool] getUserAddresses: userId={}", userId);
        List<UserAddress> result = addressMapper.findByUserId(userId);
        log.info("[UserTool] getUserAddresses: 返回 {} 条", result.size());
        return result;
    }

    @Tool(description = "新增收货地址。isDefault=true会把旧默认地址取消。返回新地址ID")
    public String addAddress(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "收件人姓名") String username,
            @ToolParam(description = "收件人手机号") String phone,
            @ToolParam(description = "省份") String province,
            @ToolParam(description = "城市") String city,
            @ToolParam(description = "区/县") String region,
            @ToolParam(description = "详细地址") String detail,
            @ToolParam(description = "是否设为默认地址") Boolean isDefault) {
        log.info("[UserTool] addAddress: userId={}, username={}, isDefault={}", userId, username, isDefault);
        if (Boolean.TRUE.equals(isDefault)) {
            UserAddress prev = addressMapper.findDefaultByUserId(userId);
            if (prev != null) {
                prev.setIsDefault(false);
                addressMapper.update(prev);
            }
        }
        UserAddress addr = UserAddress.builder()
                .userId(userId).username(username).userPhone(phone)
                .province(province).city(city).region(region)
                .detailAddress(detail).isDefault(isDefault != null && isDefault)
                .build();
        addressMapper.insert(addr);
        log.info("[UserTool] addAddress: 成功, addressId={}", addr.getId());
        return "地址添加成功, id=" + addr.getId();
    }
}
