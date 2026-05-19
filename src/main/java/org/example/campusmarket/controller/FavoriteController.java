package org.example.campusmarket.controller;

import org.example.campusmarket.dto.FavoriteProductVO;
import org.example.campusmarket.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {
    
    @Autowired
    private FavoriteService favoriteService;
    
    @PostMapping("/add")
    public Map<String, Object> addFavorite(@RequestParam Integer userId, @RequestParam Integer productId) {
        favoriteService.addFavorite(userId, productId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "收藏成功");
        return result;
    }
    
    @DeleteMapping("/remove")
    public Map<String, Object> removeFavorite(@RequestParam Integer userId, @RequestParam Integer productId) {
        favoriteService.removeFavorite(userId, productId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "取消收藏成功");
        return result;
    }
    
    @GetMapping("/list")
    public Map<String, Object> getUserFavorites(@RequestParam Integer userId) {
        List<FavoriteProductVO> favorites = favoriteService.getUserFavorites(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", favorites);
        return result;
    }
    
    @GetMapping("/check")
    public Map<String, Object> checkFavorite(@RequestParam Integer userId, @RequestParam Integer productId) {
        boolean isFavorited = favoriteService.isFavorited(userId, productId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "查询成功");
        result.put("data", isFavorited);
        return result;
    }
}
