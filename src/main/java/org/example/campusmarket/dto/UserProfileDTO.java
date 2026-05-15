package org.example.campusmarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class UserProfileDTO {
    private Integer id;
    private String username;
    private String name;
    private String phone;
    private String email;
    private String city;
    private String gender;
    private String bankAccount;
    private Double balance;
    private Integer points;
    private Integer orderCount;
    private Integer reviewCount;
    private String role;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
