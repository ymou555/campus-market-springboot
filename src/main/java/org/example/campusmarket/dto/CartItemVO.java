package org.example.campusmarket.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemVO {
    private Integer cartId;
    private Integer productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer quantity;
    private Boolean isSelected;
    private Integer stock;
}
