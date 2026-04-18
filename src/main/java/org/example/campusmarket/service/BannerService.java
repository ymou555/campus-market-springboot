package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.campusmarket.entity.Banner;
import org.example.campusmarket.mapper.BannerMapper;
import org.example.campusmarket.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class BannerService {
    @Autowired
    private BannerMapper bannerMapper;

    @Autowired
    private FileUploadUtil fileUploadUtil;

    public void addBanner(Banner banner) {
        banner.setCreateTime(new Date());
        if (banner.getBannerName() == null || banner.getBannerName().isEmpty()) {
            banner.setBannerName("未命名");
        }
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(0);
        }
        if (banner.getStatus() == null) {
            banner.setStatus("active");
        }
        bannerMapper.insert(banner);
    }

    public List<Banner> getAllBanners() {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Banner::getSortOrder);
        wrapper.orderByDesc(Banner::getCreateTime);
        return bannerMapper.selectList(wrapper);
    }

    public List<Banner> getActiveBanners() {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Banner::getStatus, "active");
        wrapper.orderByAsc(Banner::getSortOrder);
        wrapper.orderByDesc(Banner::getCreateTime);
        return bannerMapper.selectList(wrapper);
    }

    public void updateBanner(Banner banner) {
        bannerMapper.updateById(banner);
    }

    public void deleteBanner(Integer id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner != null) {
            if (banner.getImageUrl() != null && banner.getImageUrl().startsWith("/uploads")) {
                fileUploadUtil.deleteFile(banner.getImageUrl());
            }
            bannerMapper.deleteById(id);
        }
    }

    public void activateBanner(Integer id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner != null) {
            banner.setStatus("active");
            bannerMapper.updateById(banner);
        }
    }

    public void deactivateBanner(Integer id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner != null) {
            banner.setStatus("inactive");
            bannerMapper.updateById(banner);
        }
    }

    public Banner getBannerById(Integer id) {
        return bannerMapper.selectById(id);
    }
}
