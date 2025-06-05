package org.shiguang.module.hdfs.controller;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;

import org.shiguang.entity.dto.ApiResponse;
import org.shiguang.module.hdfs.service.HDFSService;
import org.shiguang.utils.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HDFS操作的REST API控制器
 */
@RestController
@RequestMapping("/hdfs")
@CrossOrigin
public class HDFSController{
    private static final Logger logger = LoggerFactory.getLogger(HDFSController.class);
    
    @Autowired
    private HDFSService hdfsService;
    
    /**
     * HDFS连接状态请求
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        try {
            Map<String, Object> statusInfo = hdfsService.getConnectionStatus();
            
            return ResponseEntity.ok(ApiResponse.success("HDFS连接状态", statusInfo));
        } catch (Exception e) {
            logger.error("获取HDFS状态失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "获取HDFS状态失败：" + e.getMessage()));
        }
    }
    
    /**
     * 列出目录内容
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listFiles(@RequestParam String path) {
        try {
            List<Map<String, Object>> fileList = hdfsService.listFiles(path);
            logger.info("列出目录: {}, 找到 {} 个文件/目录", path, fileList.size());
            return ResponseEntity.ok(ApiResponse.success("列出目录成功", fileList));
        } catch (Exception e) {
            logger.error("列出目录失败: {}", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "列出目录失败：" + e.getMessage()));
        }
    }
    
    /**
     * 创建目录
     */
    @PostMapping("/mkdir")
    public ResponseEntity<ApiResponse<Map<String, Object>>> mkdir(@RequestBody Map<String, Object> request) {
        try {
            String path = request.get("path") != null ? request.get("path").toString() : "";
            String permission = request.get("permission") != null ? request.get("permission").toString() : null;
            
            if (path == null || path.isEmpty()) {
                logger.warn("创建目录失败: 路径为空");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "路径参数不能为空"));
            }
            
            boolean success = hdfsService.mkdir(path, permission);
            
            Map<String, Object> result = new HashMap<>();
            result.put("path", path);
            result.put("success", success);
            
            if (success) {
                logger.info("目录创建成功: {}", path);
                return ResponseEntity.ok(ApiResponse.success("目录创建成功", result));
            } else {
                logger.warn("目录已存在: {}", path);
                return ResponseEntity.ok(ApiResponse.success("目录已存在", result));
            }
        } catch (Exception e) {
            logger.error("创建目录失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "创建目录失败：" + e.getMessage()));
        }
    }
    
    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String path,
            HttpServletRequest request) {
        
        try {
            if (file.isEmpty()) {
                logger.warn("上传失败: 文件为空");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "上传失败: 文件为空"));
            }
            
            if (file.getSize() > AppConstants.MAX_FILE_SIZE) {
                logger.warn("上传失败: 文件大小超过限制 {} bytes", file.getSize());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "上传失败: 文件大小超过限制 " + AppConstants.MAX_FILE_SIZE + " bytes"));
            }
            
            Map<String, Object> result = hdfsService.uploadFile(file, path);
            
            logger.info("文件上传成功: {} ({} bytes)", result.get("path"), result.get("size"));
            return ResponseEntity.ok(ApiResponse.success("文件上传成功", result));
            
        } catch (Exception e) {
            logger.error("上传文件失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "上传文件失败：" + e.getMessage()));
        }
    }
    
    /**
     * 下载文件
     */
    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(@RequestParam String path) {
        try {
            if (path == null || path.isEmpty()) {
                logger.warn("下载失败: 路径为空");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "路径参数不能为空"));
            }
            
            // 检查文件是否存在
            if (!hdfsService.exists(path)) {
                logger.warn("下载失败: 文件不存在 {}", path);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(404, "文件不存在: " + path));
            }
            
            // 获取文件信息
            Map<String, Object> fileInfo = hdfsService.getFileInfo(path);
            if (Boolean.TRUE.equals(fileInfo.get("isDirectory"))) {
                logger.warn("下载失败: 路径是一个目录而不是文件 {}", path);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "路径是一个目录而不是文件: " + path));
            }
            
            // 获取文件输入流
            FSDataInputStream inputStream = hdfsService.getInputStream(path);
            if (inputStream == null) {
                logger.error("下载失败: 无法打开文件 {}", path);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error(500, "无法打开文件: " + path));
            }
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            String fileName = new File(path).getName();
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            
            // 读取文件内容
            byte[] fileBytes = IOUtils.toByteArray(inputStream);
            inputStream.close();
            
            logger.info("文件下载: {}", path);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileBytes);
        } catch (Exception e) {
            logger.error("下载文件失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "下载文件失败：" + e.getMessage()));
        }
    }
    
    /**
     * 删除文件或目录
     */
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteFile(
            @RequestParam String path,
            @RequestParam(defaultValue = "false") boolean recursive) {
        try {
            boolean success = hdfsService.delete(path, recursive);
            
            Map<String, Object> result = new HashMap<>();
            result.put("path", path);
            result.put("success", success);
            
            logger.info("删除文件/目录: {}, 递归: {}, 结果: {}", path, recursive, success);
            return ResponseEntity.ok(ApiResponse.success("删除操作完成", result));
        } catch (Exception e) {
            logger.error("删除文件/目录失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "删除文件/目录失败：" + e.getMessage()));
        }
    }
    
    /**
     * 重命名/移动文件或目录
     */
    @PostMapping("/rename")
    public ResponseEntity<ApiResponse<Map<String, Object>>> renameFile(@RequestBody Map<String, Object> request) {
        try {
            String src = request.get("src") != null ? request.get("src").toString() : "";
            String dst = request.get("dst") != null ? request.get("dst").toString() : "";
            
            if (src == null || src.isEmpty() || dst == null || dst.isEmpty()) {
                logger.warn("重命名失败: 源路径或目标路径为空");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "源路径或目标路径参数不能为空"));
            }
            
            boolean success = hdfsService.rename(src, dst);
            
            Map<String, Object> result = new HashMap<>();
            result.put("source", src);
            result.put("destination", dst);
            result.put("success", success);
            
            logger.info("重命名文件/目录: {} -> {}, 结果: {}", src, dst, success);
            return ResponseEntity.ok(ApiResponse.success("重命名操作完成", result));
        } catch (Exception e) {
            logger.error("重命名文件/目录失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "重命名文件/目录失败：" + e.getMessage()));
        }
    }
    
    /**
     * 检查文件或目录是否存在
     */
    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<Map<String, Object>>> fileExists(@RequestParam String path) {
        try {
            boolean exists = hdfsService.exists(path);
            
            Map<String, Object> result = new HashMap<>();
            result.put("path", path);
            result.put("exists", exists);
            
            logger.info("检查文件/目录是否存在: {}, 结果: {}", path, exists);
            return ResponseEntity.ok(ApiResponse.success("检查文件/目录是否存在", result));
        } catch (Exception e) {
            logger.error("检查文件/目录是否存在失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "检查文件/目录是否存在失败：" + e.getMessage()));
        }
    }
} 