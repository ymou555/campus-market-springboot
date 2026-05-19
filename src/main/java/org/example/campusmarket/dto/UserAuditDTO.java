package org.example.campusmarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class UserAuditDTO {
    private Integer id;
    private String username;
    private String name;
    private String phone;
    private String email;
    private String type;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date registerTime;
    private String shopName;
    private String businessLicense;
    private String idCardPhoto;
    private String auditStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date auditTime;
    private String auditRemark;
    private String status;
}
