package org.shiguang.module.oss.controller;

import org.shiguang.entity.dto.ApiResponse;
import org.shiguang.module.oss.service.OssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/file")
@CrossOrigin
public class FileController {

    @Autowired
    private OssService ossService;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dir", required = false) String dir) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(400, "文件不能为空"));
            }
            
            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.ok(ApiResponse.error(400, "只支持图片文件上传"));
            }
            
            // 检查文件大小
            long fileSize = file.getSize();
            if (fileSize > 5 * 1024 * 1024) { // 5MB
                return ResponseEntity.ok(ApiResponse.error(400, "文件大小不能超过5MB"));
            }
            
            System.out.println("开始上传文件，原始名称: " + file.getOriginalFilename() + 
                             ", 大小: " + fileSize + "字节, 类型: " + contentType);
            System.out.println("目标目录: " + (dir == null ? "默认目录" : dir));
            
            // 如果是头像上传，增加额外日志
            boolean isAvatar = dir != null && dir.contains("avatar");
            if (isAvatar) {
                System.out.println("正在处理头像上传...");
            }
            
            Map<String, String> result = ossService.uploadFile(file, dir);
            
            System.out.println("文件上传成功，返回结果: " + result);
            
            // 确保URL字段存在
            if (result != null && result.containsKey("url")) {
                String url = result.get("url");
                System.out.println("文件URL: " + url);
                
                if (isAvatar) {
                    System.out.println("头像上传成功，URL: " + url);
                    System.out.println("请确保前端正确处理此URL并更新用户资料");
                }
            } else {
                System.out.println("警告：返回结果中不包含URL字段");
                if (result != null) {
                    result.forEach((key, value) -> System.out.println(key + ": " + value));
                }
            }
            
            return ResponseEntity.ok(ApiResponse.success("文件上传成功", result));
        } catch (Exception e) {
            System.err.println("文件上传失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.error(500, "文件上传失败: " + e.getMessage()));
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@RequestParam("url") String url) {
        try {
            boolean deleted = ossService.deleteFile(url);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("文件删除成功"));
            } else {
                return ResponseEntity.ok(ApiResponse.error(500, "文件删除失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "文件删除失败: " + e.getMessage()));
        }
    }
} 