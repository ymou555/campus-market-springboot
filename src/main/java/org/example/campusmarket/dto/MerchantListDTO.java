package org.example.campusmarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class MerchantListDTO {
    private Integer id;
    private String shopName;
    private Integer levelId;
    private Integer totalSales;
    private Double avgRating;
    private String status; // active, banned, closed
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date banEndTime;
    private String banReason;
    private Integer productCount;
}
