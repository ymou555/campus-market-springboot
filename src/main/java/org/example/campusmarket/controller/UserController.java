package org.example.campusmarket.controller;

import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;

    // 获取个人信息
    @GetMapping("/profile")
    public Map<String, Object> getProfile(@RequestParam Integer id) {
        SysUser user = userService.getUserById(id);
        if (user == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 404);
            result.put("message", "用户不存在");
            return result;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("user", user);
        return result;
    }

    // 更新个人信息
    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody SysUser user) {
        userService.updateUser(user);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        return result;
    }
}