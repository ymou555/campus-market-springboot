package org.example.campusmarket.controller;

import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.service.AuthService;
import org.example.campusmarket.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;
    @Autowired
    private FileUploadUtil fileUploadUtil;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> registerData, HttpServletRequest request) {
        String captcha = (String) registerData.get("captcha");
        HttpSession session = request.getSession();
        String storedCaptcha = (String) session.getAttribute("captcha");

        if (captcha == null || storedCaptcha == null || !storedCaptcha.equalsIgnoreCase(captcha)) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "验证码错误");
            return result;
        }

        SysUser user = new SysUser();
        user.setUsername((String) registerData.get("username"));
        user.setPassword((String) registerData.get("password"));
        user.setName((String) registerData.get("name"));
        user.setPhone((String) registerData.get("phone"));
        user.setEmail((String) registerData.get("email"));
        user.setCity((String) registerData.get("city"));
        user.setGender((String) registerData.get("gender"));
        user.setBankAccount((String) registerData.get("bankAccount"));
        user.setRole("user");

        SysUser registeredUser = authService.register(user, registerData);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "注册成功，等待审核");
        result.put("user", registeredUser);
        return result;
    }

    @PostMapping("/merchant/register")
    public Map<String, Object> merchantRegister(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("name") String name,
            @RequestParam("phone") String phone,
            @RequestParam("email") String email,
            @RequestParam("city") String city,
            @RequestParam("gender") String gender,
            @RequestParam("bankAccount") String bankAccount,
            @RequestParam("shopName") String shopName,
            @RequestParam("businessLicense") MultipartFile businessLicense,
            @RequestParam("idCardPhoto") MultipartFile idCardPhoto,
            @RequestParam("captcha") String captcha,
            HttpServletRequest request) {

        HttpSession session = request.getSession();
        String storedCaptcha = (String) session.getAttribute("captcha");

        if (captcha == null || storedCaptcha == null || !storedCaptcha.equalsIgnoreCase(captcha)) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "验证码错误");
            return result;
        }

        Map<String, Object> registerData = new HashMap<>();
        registerData.put("role", "merchant");
        registerData.put("shopName", shopName);

        try {
            String businessLicenseUrl = fileUploadUtil.uploadMerchantDocument(businessLicense, "license");
            registerData.put("businessLicense", businessLicenseUrl);
        } catch (IOException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "营业执照上传失败: " + e.getMessage());
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", e.getMessage());
            return result;
        }

        try {
            String idCardPhotoUrl = fileUploadUtil.uploadMerchantDocument(idCardPhoto, "idcard");
            registerData.put("idCardPhoto", idCardPhotoUrl);
        } catch (IOException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "身份证照片上传失败: " + e.getMessage());
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", e.getMessage());
            return result;
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setName(name);
        user.setPhone(phone);
        user.setEmail(email);
        user.setCity(city);
        user.setGender(gender);
        user.setBankAccount(bankAccount);
        user.setRole("merchant");

        SysUser registeredUser = authService.register(user, registerData);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "注册成功，等待审核");
        result.put("user", registeredUser);
        return result;
    }

    @ResponseBody
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");
        Map<String, Object> loginResult = authService.login(username, password);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "登录成功");
        result.put("token", loginResult.get("token"));
        result.put("user", loginResult.get("user"));
        return result;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "退出成功");
        return result;
    }
    
    // 修改密码
    @PostMapping("/change-password")
    public Map<String, Object> changePassword(
            @RequestParam Integer userId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        try {
            authService.changePassword(userId, oldPassword, newPassword);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "密码修改成功，请重新登录");
            return result;
        } catch (RuntimeException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", e.getMessage());
            return result;
        }
    }
}
