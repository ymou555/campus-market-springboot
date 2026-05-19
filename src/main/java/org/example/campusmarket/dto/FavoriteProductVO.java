package org.example.campusmarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class FavoriteProductVO {
    private Integer id;
    private Integer productId;
    private String productName;
    private String productImage;
    private Double price;
    private Integer stock;
    private String status;
    private String merchant;
    private Integer merchantId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date favoriteTime;
}
