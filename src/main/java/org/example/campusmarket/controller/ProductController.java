package org.example.campusmarket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.dto.ProductWithAuditDTO;
import org.example.campusmarket.entity.Product;
import org.example.campusmarket.entity.ProductImage;
import org.example.campusmarket.entity.CategoryTree;
import org.example.campusmarket.service.ProductService;
import org.example.campusmarket.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductController {
    @Autowired
    private ProductService productService;

    @Autowired
    private FileUploadUtil fileUploadUtil;

    // 发布商品
    @PostMapping("/publish")
    public Map<String, Object> publishProduct(
            @RequestParam("merchantId") Integer merchantId,
            @RequestParam("categoryId") Integer categoryId,
            @RequestParam("productName") String productName,
            @RequestParam("originalPrice") Double originalPrice,
            @RequestParam("discountPrice") Double discountPrice,
            @RequestParam("size") String size,
            @RequestParam("description") String description,
            @RequestParam("isNegotiable") Boolean isNegotiable,
            @RequestParam("stock") Integer stock,
            @RequestParam("newness") String newness,
            @RequestParam("images") MultipartFile[] images) {

        if (images == null || images.length == 0) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "请至少上传一张商品图片");
            return result;
        }

        if (images.length > 4) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "最多只能上传4张图片");
            return result;
        }

        List<String> imageUrls = new ArrayList<>();
        for (int i = 0; i < images.length; i++) {
            try {
                String imageUrl = fileUploadUtil.uploadCommodityImage(images[i]);
                imageUrls.add(imageUrl);
            } catch (IOException e) {
                Map<String, Object> result = new HashMap<>();
                result.put("code", 500);
                result.put("message", "第" + (i + 1) + "张图片上传失败: " + e.getMessage());
                return result;
            } catch (IllegalArgumentException e) {
                Map<String, Object> result = new HashMap<>();
                result.put("code", 400);
                result.put("message", "第" + (i + 1) + "张图片格式错误: " + e.getMessage());
                return result;
            }
        }

        Product product = new Product();
        product.setMerchantId(merchantId);
        product.setCategoryId(categoryId);
        product.setProductName(productName);
        product.setOriginalPrice(originalPrice);
        product.setDiscountPrice(discountPrice);
        product.setSize(size);
        product.setDescription(description);
        product.setIsNegotiable(isNegotiable);
        product.setStock(stock);
        product.setNewness(newness);

        productService.publishProduct(product, imageUrls);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "发布成功，等待审核");
        result.put("imageUrls", imageUrls);
        return result;
    }

    // 审核商品
    @PostMapping("/audit")
    public Map<String, Object> auditProduct(
            @RequestParam Integer productId,
            @RequestParam String auditStatus,
            @RequestParam String remark) {
        productService.auditProduct(productId, auditStatus, remark);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "审核成功");
        return result;
    }

    // 商品上下架
    @PostMapping("/toggle-status")
    public Map<String, Object> toggleProductStatus(
            @RequestParam Integer productId,
            @RequestParam String status) {
        Map<String, Object> result = new HashMap<>();
        try {
            productService.toggleProductStatus(productId, status);
            result.put("code", 200);
            result.put("message", "操作成功");
        } catch (RuntimeException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 搜索商品
    @GetMapping("/search")
    public Map<String, Object> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String newness) {
        List<Product> products = productService.searchProducts(keyword, categoryId, sortBy, minPrice, maxPrice, newness);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "搜索成功");
        result.put("data", products);
        return result;
    }
    
    // 获取商品分类树
    @GetMapping("/categories")
    public Map<String, Object> getCategoryTree() {
        List<CategoryTree> categoryTree = productService.getCategoryTree();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", categoryTree);
        return result;
    }

    // 获取商品详情
    @GetMapping("/detail")
    public Map<String, Object> getProductDetail(@RequestParam Integer id) {
        Product product = productService.getProductById(id);
        // 清除不需要的字段
        product.setMerchantName(null);
        product.setFirstImage(null);
        
        List<ProductImage> images = productService.getProductImages(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("product", product);
        result.put("images", images);
        return result;
    }

    // 获取商家商品列表
    @GetMapping("/merchant/list")
    public Map<String, Object> getMerchantProducts(
            @RequestParam Integer merchantId,
            @RequestParam(required = false) String status) {
        List<ProductWithAuditDTO> products = productService.getMerchantProducts(merchantId, status);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", products);
        return result;
    }

    // 更新商品信息
    @PutMapping("/update")
    public Map<String, Object> updateProduct(@RequestBody Product product) {
        productService.updateProduct(product);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        return result;
    }

    // 更新商品信息（包含图片）
    @PostMapping("/update-with-images")
    public Map<String, Object> updateProductWithImages(
            @RequestParam("productId") Integer productId,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "productName", required = false) String productName,
            @RequestParam(value = "originalPrice", required = false) Double originalPrice,
            @RequestParam(value = "discountPrice", required = false) Double discountPrice,
            @RequestParam(value = "size", required = false) String size,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "isNegotiable", required = false) Boolean isNegotiable,
            @RequestParam(value = "stock", required = false) Integer stock,
            @RequestParam(value = "newness", required = false) String newness,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {

        Map<String, Object> result = new HashMap<>();
        try {
            Product product = productService.getProductById(productId);
            if (product == null) {
                result.put("code", 404);
                result.put("message", "商品不存在");
                return result;
            }

            if (images != null && images.length > 4) {
                result.put("code", 400);
                result.put("message", "最多只能上传4张图片");
                return result;
            }

            List<String> imageUrls = new ArrayList<>();
            if (images != null && images.length > 0) {
                List<ProductImage> oldImages = productService.getProductImages(productId);
                for (int i = 0; i < images.length; i++) {
                    try {
                        String imageUrl = fileUploadUtil.uploadCommodityImage(images[i]);
                        imageUrls.add(imageUrl);
                    } catch (IOException e) {
                        result.put("code", 500);
                        result.put("message", "第" + (i + 1) + "张图片上传失败: " + e.getMessage());
                        return result;
                    } catch (IllegalArgumentException e) {
                        result.put("code", 400);
                        result.put("message", "第" + (i + 1) + "张图片格式错误: " + e.getMessage());
                        return result;
                    }
                }
                
                for (ProductImage oldImage : oldImages) {
                    String oldImageUrl = oldImage.getImageUrl();
                    if (oldImageUrl != null && oldImageUrl.startsWith("/uploads")) {
                        fileUploadUtil.deleteFile(oldImageUrl);
                    }
                }
            }

            if (categoryId != null) {
                product.setCategoryId(categoryId);
            }
            if (productName != null) {
                product.setProductName(productName);
            }
            if (originalPrice != null) {
                product.setOriginalPrice(originalPrice);
            }
            if (discountPrice != null) {
                product.setDiscountPrice(discountPrice);
            }
            if (size != null) {
                product.setSize(size);
            }
            if (description != null) {
                product.setDescription(description);
            }
            if (isNegotiable != null) {
                product.setIsNegotiable(isNegotiable);
            }
            if (stock != null) {
                product.setStock(stock);
            }
            if (newness != null) {
                product.setNewness(newness);
            }

            productService.updateProductWithImages(product, imageUrls.isEmpty() ? null : imageUrls);

            result.put("code", 200);
            result.put("message", "更新成功");
            result.put("imageUrls", imageUrls);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "更新失败: " + e.getMessage());
        }
        return result;
    }

    // 删除商品
    @DeleteMapping("/delete")
    public Map<String, Object> deleteProduct(@RequestParam Integer productId) {
        productService.deleteProduct(productId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");
        return result;
    }

    // 获取商家店铺信息
    @GetMapping("/shop/info")
    public Map<String, Object> getShopInfo(@RequestParam Integer merchantId) {
        Map<String, Object> shopInfo = productService.getMerchantShopInfo(merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", shopInfo);
        return result;
    }

    // 获取商家店铺的商品列表
    @GetMapping("/shop/products")
    public Map<String, Object> getShopProducts(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam Integer merchantId,
            @RequestParam(required = false) String sortBy) {
        Page<Product> productPage = productService.getShopProducts(page, size, merchantId, sortBy);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", productPage.getRecords());
        result.put("total", productPage.getTotal());
        return result;
    }
}