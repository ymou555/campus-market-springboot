package org.example.campusmarket.controller;

import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

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
        user.setRole((String) registerData.get("role"));
        
        SysUser registeredUser = authService.register(user);
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
        // 这里可以实现Token的注销逻辑
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "退出成功");
        return result;
    }
}