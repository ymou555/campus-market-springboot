package org.example.campusmarket.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SelectedCartItemVO {
    private Integer cartId;
    private Integer productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private String productImage;
    private Integer merchantId;
    private String merchantName;
}
