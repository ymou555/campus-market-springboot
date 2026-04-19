package org.example.campusmarket.dto;

import lombok.Data;

import java.util.List;

@Data
public class MerchantCartVO {
    private Integer merchantId;
    private String merchantName;
    private List<CartItemVO> products;
}
