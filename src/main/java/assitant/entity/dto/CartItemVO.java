package assitant.entity.dto;

import lombok.Data;

@Data
public class CartItemVO {
    private Long cartId;
    private Long userId;
    private Long goodsId;
    private Integer goodsCount;
    private String goodsName;
    private String coverImg;
    private Integer sellingPrice;
}
