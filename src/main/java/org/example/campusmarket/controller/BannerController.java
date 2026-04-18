package org.example.campusmarket.controller;

import org.example.campusmarket.entity.Banner;
import org.example.campusmarket.service.BannerService;
import org.example.campusmarket.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/banner")
public class BannerController {
    @Autowired
    private BannerService bannerService;

    @Autowired
    private FileUploadUtil fileUploadUtil;

    @PostMapping("/upload")
    public Map<String, Object> uploadBannerImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            String imageUrl = fileUploadUtil.uploadBannerImage(file);
            result.put("code", 200);
            result.put("message", "上传成功");
            result.put("imageUrl", imageUrl);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (IOException e) {
            result.put("code", 500);
            result.put("message", "文件上传失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> addBanner(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bannerName", required = false) String bannerName,
            @RequestParam(value = "linkUrl", required = false) String linkUrl,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder) {
        Map<String, Object> result = new HashMap<>();
        try {
            String imageUrl = fileUploadUtil.uploadBannerImage(file);
            
            Banner banner = new Banner();
            banner.setBannerName(bannerName != null && !bannerName.isEmpty() ? bannerName : "未命名");
            banner.setImageUrl(imageUrl);
            banner.setLinkUrl(linkUrl);
            banner.setSortOrder(sortOrder != null ? sortOrder : 0);
            
            bannerService.addBanner(banner);
            
            result.put("code", 200);
            result.put("message", "添加成功");
            result.put("data", banner);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (IOException e) {
            result.put("code", 500);
            result.put("message", "文件上传失败: " + e.getMessage());
        }
        return result;
    }

    // 使用已有URL添加轮播图
    @PostMapping("/add-with-url")
    public Map<String, Object> addBannerWithUrl(@RequestBody Banner banner) {
        bannerService.addBanner(banner);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "添加成功");
        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> getAllBanners() {
        List<Banner> banners = bannerService.getAllBanners();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", banners);
        return result;
    }

    @GetMapping("/active")
    public Map<String, Object> getActiveBanners() {
        List<Banner> banners = bannerService.getActiveBanners();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", banners);
        return result;
    }

    @GetMapping("/detail")
    public Map<String, Object> getBannerById(@RequestParam Integer id) {
        Banner banner = bannerService.getBannerById(id);
        Map<String, Object> result = new HashMap<>();
        if (banner != null) {
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", banner);
        } else {
            result.put("code", 404);
            result.put("message", "轮播图不存在");
        }
        return result;
    }

    @PutMapping("/update")
    public Map<String, Object> updateBanner(@RequestBody Banner banner) {
        bannerService.updateBanner(banner);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        return result;
    }

    // 更新轮播图并可选上传新图片
    @PostMapping("/update-with-image")
    public Map<String, Object> updateBannerWithImage(
            @RequestParam("id") Integer id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "bannerName", required = false) String bannerName,
            @RequestParam(value = "linkUrl", required = false) String linkUrl,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder) {
        Map<String, Object> result = new HashMap<>();
        try {
            Banner banner = bannerService.getBannerById(id);
            if (banner == null) {
                result.put("code", 404);
                result.put("message", "轮播图不存在");
                return result;
            }

            if (file != null && !file.isEmpty()) {
                String oldImageUrl = banner.getImageUrl();
                String imageUrl = fileUploadUtil.uploadBannerImage(file);
                banner.setImageUrl(imageUrl);
                if (oldImageUrl != null && oldImageUrl.startsWith("/uploads")) {
                    fileUploadUtil.deleteFile(oldImageUrl);
                }
            }

            if (bannerName != null) {
                banner.setBannerName(bannerName.isEmpty() ? "未命名" : bannerName);
            }
            if (linkUrl != null) {
                banner.setLinkUrl(linkUrl);
            }
            if (sortOrder != null) {
                banner.setSortOrder(sortOrder);
            }

            bannerService.updateBanner(banner);

            result.put("code", 200);
            result.put("message", "更新成功");
            result.put("data", banner);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (IOException e) {
            result.put("code", 500);
            result.put("message", "文件上传失败: " + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/delete")
    public Map<String, Object> deleteBanner(@RequestParam Integer id) {
        bannerService.deleteBanner(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");
        return result;
    }

    @PostMapping("/activate")
    public Map<String, Object> activateBanner(@RequestParam Integer id) {
        bannerService.activateBanner(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "启用成功");
        return result;
    }

    @PostMapping("/deactivate")
    public Map<String, Object> deactivateBanner(@RequestParam Integer id) {
        bannerService.deactivateBanner(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "停用成功");
        return result;
    }
}
