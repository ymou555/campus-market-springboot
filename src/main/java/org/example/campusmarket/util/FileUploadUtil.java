package org.example.campusmarket.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class FileUploadUtil {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${file.upload.banner-path:/banners}")
    private String bannerPath;

    @Value("${file.upload.merchant-path:/merchant}")
    private String merchantPath;

    public String uploadBannerImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "unknown";
        }

        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }

        if (!isValidImageExtension(extension)) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持 jpg, jpeg, png, gif, webp");
        }

        String newFilename = UUID.randomUUID().toString() + extension;

        Path bannerDir = Paths.get(uploadPath, bannerPath).toAbsolutePath().normalize();
        if (!Files.exists(bannerDir)) {
            Files.createDirectories(bannerDir);
        }

        Path targetPath = bannerDir.resolve(newFilename);
        file.transferTo(targetPath.toFile());

        return "/uploads" + bannerPath + "/" + newFilename;
    }

    public String uploadMerchantDocument(MultipartFile file, String documentType) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "unknown";
        }

        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }

        if (!isValidImageExtension(extension)) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持 jpg, jpeg, png, gif, webp");
        }

        String newFilename = UUID.randomUUID().toString() + extension;

        Path merchantDir = Paths.get(uploadPath, merchantPath, documentType).toAbsolutePath().normalize();
        if (!Files.exists(merchantDir)) {
            Files.createDirectories(merchantDir);
        }

        Path targetPath = merchantDir.resolve(newFilename);
        file.transferTo(targetPath.toFile());

        return "/uploads" + merchantPath + "/" + documentType + "/" + newFilename;
    }

    public String uploadCommodityImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "unknown";
        }

        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }

        if (!isValidImageExtension(extension)) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持 jpg, jpeg, png, gif, webp");
        }

        String newFilename = UUID.randomUUID().toString() + extension;

        Path commodityDir = Paths.get(uploadPath, merchantPath, "commodity").toAbsolutePath().normalize();
        if (!Files.exists(commodityDir)) {
            Files.createDirectories(commodityDir);
        }

        Path targetPath = commodityDir.resolve(newFilename);
        file.transferTo(targetPath.toFile());

        return "/uploads" + merchantPath + "/commodity/" + newFilename;
    }

    private boolean isValidImageExtension(String extension) {
        String lowerExt = extension.toLowerCase();
        return lowerExt.equals(".jpg") ||
                lowerExt.equals(".jpeg") ||
                lowerExt.equals(".png") ||
                lowerExt.equals(".gif") ||
                lowerExt.equals(".webp");
    }

    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }

        try {
            String relativePath = fileUrl.replace("/uploads", "");
            Path filePath = Paths.get(uploadPath, relativePath).toAbsolutePath().normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }
}
