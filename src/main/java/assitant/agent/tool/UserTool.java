package assitant.agent.tool;

import assitant.annotation.OperationLog;
import assitant.entity.po.*;
import assitant.mapper.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserTool {

    @Resource
    private UserMapper userMapper;
    @Resource
    private UserAddressMapper addressMapper;

    @Tool(description = "获取用户个人信息")
    @OperationLog(module = "Agent", type = "查询", description = "用户信息")
    public String getUserProfile(@ToolParam(description = "用户ID") Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) return "用户不存在";
        if (user != null) user.setPassword(null);
        return "昵称:" + user.getNickName() + " 登录名:" + user.getLoginName()
                + " 签名:" + (user.getIntroduceSign() != null ? user.getIntroduceSign() : "");
    }

    @Tool(description = "获取用户收货地址列表，返回每条地址的ID可用于createOrder")
    @OperationLog(module = "Agent", type = "查询", description = "收货地址列表")
    public String getUserAddresses(@ToolParam(description = "用户ID") Long userId) {
        List<UserAddress> list = addressMapper.findByUserId(userId);
        if (list.isEmpty()) return "暂无收货地址";
        StringBuilder sb = new StringBuilder();
        for (UserAddress a : list) {
            sb.append("地址ID:").append(a.getId()).append(" ").append(a.getUsername())
              .append(" ").append(a.getUserPhone()).append(" ")
              .append(a.getProvince()).append(a.getCity()).append(a.getRegion())
              .append(" ").append(a.getDetailAddress())
              .append(a.getIsDefault() != null && a.getIsDefault() ? " [默认]" : "").append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "新增收货地址。isDefault=true会把旧默认地址取消。返回新地址ID")
    @Transactional
    @OperationLog(module = "Agent", type = "写入", description = "新增地址")
    public String addAddress(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "收件人姓名") String username,
            @ToolParam(description = "收件人手机号") String phone,
            @ToolParam(description = "省份") String province,
            @ToolParam(description = "城市") String city,
            @ToolParam(description = "区/县") String region,
            @ToolParam(description = "详细地址") String detail,
            @ToolParam(description = "是否设为默认地址") Boolean isDefault) {
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
        return "地址添加成功, id=" + addr.getId();
    }

    @Tool(description = "修改已有收货地址。只需传要修改的字段，不传则保留原值。addressId从getUserAddresses返回的id获取")
    @Transactional
    @OperationLog(module = "Agent", type = "修改", description = "修改地址")
    public String updateAddress(
            @ToolParam(description = "地址ID，从getUserAddresses返回的id获取") Long addressId,
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "收件人姓名，不传不变") String username,
            @ToolParam(description = "收件人手机号，不传不变") String phone,
            @ToolParam(description = "省份，不传不变") String province,
            @ToolParam(description = "城市，不传不变") String city,
            @ToolParam(description = "区/县，不传不变") String region,
            @ToolParam(description = "详细地址，不传不变") String detail,
            @ToolParam(description = "是否设为默认地址") Boolean isDefault) {
        UserAddress addr = addressMapper.findByUserId(userId).stream()
                .filter(a -> a.getId().equals(addressId)).findFirst().orElse(null);
        if (addr == null) return "地址不存在或不属于该用户";
        if (username != null) addr.setUsername(username);
        if (phone != null) addr.setUserPhone(phone);
        if (province != null) addr.setProvince(province);
        if (city != null) addr.setCity(city);
        if (region != null) addr.setRegion(region);
        if (detail != null) addr.setDetailAddress(detail);
        if (isDefault != null && isDefault) {
            UserAddress prev = addressMapper.findDefaultByUserId(userId);
            if (prev != null && !prev.getId().equals(addressId)) {
                prev.setIsDefault(false);
                addressMapper.update(prev);
            }
            addr.setIsDefault(true);
        }
        addressMapper.update(addr);
        return "地址修改成功";
    }
}
