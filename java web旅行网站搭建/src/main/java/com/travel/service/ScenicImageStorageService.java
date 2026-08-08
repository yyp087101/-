package com.travel.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ScenicImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;

    public String storeScenicImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }

        String extension = resolveExtension(imageFile);
        Path scenicDirectory = Paths.get(uploadBaseDir, "scenic").toAbsolutePath().normalize();

        try {
            Files.createDirectories(scenicDirectory);
            String filename = "scenic-" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path targetFile = scenicDirectory.resolve(filename).normalize();
            imageFile.transferTo(targetFile.toFile());
            return "/uploads/scenic/" + filename;
        } catch (IOException ex) {
            throw new IllegalStateException("图片保存失败，请稍后重试", ex);
        }
    }

    private String resolveExtension(MultipartFile imageFile) {
        String originalFilename = imageFile.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null) {
            extension = fromContentType(imageFile.getContentType());
        }
        if (extension == null) {
            throw new IllegalArgumentException("仅支持 JPG、PNG、GIF、WEBP 图片");
        }

        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(normalizedExtension)) {
            throw new IllegalArgumentException("仅支持 JPG、PNG、GIF、WEBP 图片");
        }
        return normalizedExtension;
    }

    private String fromContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            default:
                return null;
        }
    }
}
