package org.example.campusmarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class MerchantDetailDTO {
    private BasicInfo basicInfo;
    private ShopInfo shopInfo;
    private List<BanRecord> banRecords;
    
    @Data
    public static class BasicInfo {
        private Integer userId;
        private String name;
        private String phone;
        private String email;
        private String gender;
        private String city;
        private String bankAccount;
        private String status;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;
    }
    
    @Data
    public static class ShopInfo {
        private String shopName;
        private String businessLicense;
        private String idCardPhoto;
        private Integer levelId;
        private Double rate;
        private String shopStatus;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;
    }
    
    @Data
    public static class BanRecord {
        private Integer id;
        private String banReason;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date banStartTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date banEndTime;
        private String status;
    }
}
