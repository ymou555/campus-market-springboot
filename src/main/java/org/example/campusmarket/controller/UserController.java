package org.example.campusmarket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    // 获取用户列表（管理员）
    @GetMapping("/list")
    public Map<String, Object> getUserList(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        Page<SysUser> userPage = userService.getUserList(page, size, role, status);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", userPage.getRecords());
        result.put("total", userPage.getTotal());
        return result;
    }

    // 审核用户（管理员）
    @PostMapping("/audit")
    public Map<String, Object> auditUser(
            @RequestParam Integer userId,
            @RequestParam String auditStatus,
            @RequestParam String remark) {
        userService.auditUser(userId, auditStatus, remark);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "审核成功");
        return result;
    }

    // 封禁用户（管理员）
    @PostMapping("/block")
    public Map<String, Object> blockUser(@RequestParam Integer userId) {
        userService.blockUser(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "封禁成功");
        return result;
    }

    // 解封用户（管理员）
    @PostMapping("/unblock")
    public Map<String, Object> unblockUser(@RequestParam Integer userId) {
        userService.unblockUser(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "解封成功");
        return result;
    }
}