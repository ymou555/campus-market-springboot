package org.example.campusmarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class BlacklistUserDTO {
    private Integer id;
    private String username;
    private String name;
    private String blockedBy;
    private Integer blockedById;
    private String reason;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date time;
    private String blacklistType;
    private String merchantName;
}
