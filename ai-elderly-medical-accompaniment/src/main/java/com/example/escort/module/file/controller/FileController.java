package com.example.escort.module.file.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.example.escort.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件上传控制器
 * /api/file（需登录）
 */
@Slf4j
@Tag(name = "文件上传模块")
@RestController
@RequestMapping("/api/file")
public class FileController {

    /** 允许上传的文件后缀白名单（图片/PDF，不含Office文档） */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "pdf"
    ));

    // ===== 57. POST /api/file/upload 文件上传 =====
    @Operation(summary = "文件上传（图片/PDF，限10MB，白名单后缀，UUID重命名，需登录）")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail(400, "上传文件不能为空");
        }

        // 后缀白名单校验
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            return Result.fail(400, "文件格式不支持");
        }
        String ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return Result.fail(400, "文件格式不支持，仅允许：" + String.join(",", ALLOWED_EXTENSIONS));
        }

        // UUID重命名，保留原始后缀
        String newFileName = IdUtil.fastSimpleUUID() + "." + ext;
        // BUG-F：显式绝对化，保证 mkParentDirs 与 transferTo 使用同一 JVM 工作目录基准
        File dest = new File("./upload/" + newFileName).getAbsoluteFile();

        try {
            FileUtil.mkParentDirs(dest);
            file.transferTo(dest);
            String relativePath = "/upload/" + newFileName;
            log.info("文件上传成功: original={}, saved={}", originalName, relativePath);
            return Result.ok(relativePath);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.fail(500, "文件上传失败，请重试");
        }
    }
}