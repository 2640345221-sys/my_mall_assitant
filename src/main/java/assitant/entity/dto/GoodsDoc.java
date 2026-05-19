package assitant.entity.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoodsDoc {
    private Long id;
    private String text;
    private String vector;
 }
