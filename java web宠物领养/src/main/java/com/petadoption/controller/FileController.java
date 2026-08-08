package com.petadoption.controller;

import com.petadoption.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${upload.path}")
    private String uploadPath;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = UUID.randomUUID().toString().replace("-", "") + suffix;
        File dir = new File(uploadPath);
        if (!dir.exists() && !dir.mkdirs()) {
            return Result.error("上传目录创建失败");
        }
        try {
            file.transferTo(new File(dir, newFilename));
            return Result.success("/uploads/" + newFilename);
        } catch (IOException e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }
}
