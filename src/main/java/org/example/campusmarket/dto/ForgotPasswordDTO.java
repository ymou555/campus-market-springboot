package org.example.campusmarket.dto;

import lombok.Data;

@Data
public class ForgotPasswordDTO {
    private String username;
    private String email;
    private String newPassword;
}