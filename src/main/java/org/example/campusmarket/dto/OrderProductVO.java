package org.example.campusmarket.dto;

import lombok.Data;

@Data
public class OrderProductVO {
    private Integer id;
    private String productName;
    private Double price;
    private Integer quantity;
    private String productImage;
}
