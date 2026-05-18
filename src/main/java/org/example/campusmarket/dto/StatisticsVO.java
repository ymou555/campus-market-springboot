package org.example.campusmarket.dto;

import lombok.Data;

@Data
public class StatisticsVO {
    private Long userCount;
    private Long productCount;
    private Double totalOrderAmount;
    private Long orderCount;
}
