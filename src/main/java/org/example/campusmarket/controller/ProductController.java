package org.example.campusmarket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.entity.Product;
import org.example.campusmarket.entity.ProductImage;
import org.example.campusmarket.entity.CategoryTree;
import org.example.campusmarket.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductController {
    @Autowired
    private ProductService productService;

    // 发布商品
    @PostMapping("/publish")
    public Map<String, Object> publishProduct(
            @RequestBody Map<String, Object> productData) {
        Product product = new Product();
        product.setMerchantId((Integer) productData.get("merchantId"));
        product.setCategoryId((Integer) productData.get("categoryId"));
        product.setProductName((String) productData.get("productName"));
        product.setOriginalPrice((Double) productData.get("originalPrice"));
        product.setDiscountPrice((Double) productData.get("discountPrice"));
        product.setSize((String) productData.get("size"));
        product.setDescription((String) productData.get("description"));
        product.setIsNegotiable((Boolean) productData.get("isNegotiable"));
        product.setStock((Integer) productData.get("stock"));
        product.setNewness((String) productData.get("newness"));

        List<String> imageUrls = (List<String>) productData.get("imageUrls");
        productService.publishProduct(product, imageUrls);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "发布成功，等待审核");
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
        productService.toggleProductStatus(productId, status);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "操作成功");
        return result;
    }

    // 搜索商品
    @GetMapping("/search")
    public Map<String, Object> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String sortBy) {
        List<Product> products = productService.searchProducts(keyword, categoryId, sortBy);
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
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam Integer merchantId,
            @RequestParam(required = false) String status) {
        Page<Product> productPage = productService.getMerchantProducts(page, size, merchantId, status);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", productPage.getRecords());
        result.put("total", productPage.getTotal());
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