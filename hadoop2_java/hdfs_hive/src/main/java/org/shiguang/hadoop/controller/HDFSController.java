package org.shiguang.hadoop.controller;

import org.shiguang.hadoop.HDFSClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HDFS操作的REST API控制器
 */
@RestController
@RequestMapping("/api/hdfs")
@CrossOrigin
public class HDFSController {

    @Autowired
    private HDFSClient hdfsClient;

    /**
     * 获取HDFS连接状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            boolean connected = hdfsClient.getFileSystem() != null;
            status.put("connected", connected);
            if (connected) {
                status.put("uri", hdfsClient.getFileSystem().getUri().toString());
            }
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            status.put("connected", false);
            status.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(status);
        }
    }

    /**
     * 列出目录内容
     */
    @GetMapping("/list")
    public ResponseEntity<?> listFiles(@RequestParam String path) {
        try {
            List<Map<String, Object>> files = hdfsClient.listFiles(path);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 创建目录
     */
    @PostMapping("/mkdir")
    public ResponseEntity<?> mkdir(@RequestParam String path, @RequestParam(required = false) String permission) {
        try {
            boolean success;
            if (permission != null) {
                success = hdfsClient.mkdir(path, permission);
            } else {
                success = hdfsClient.mkdir(path);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("path", path);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam String path) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "上传文件不能为空"));
            }

            // 创建临时文件
            File tempFile = File.createTempFile("hdfs_upload_", "_" + file.getOriginalFilename());
            try {
                // 将上传的文件内容保存到临时文件
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(file.getBytes());
                }
                
                // 上传文件到HDFS
                hdfsClient.uploadFile(tempFile.getAbsolutePath(), path);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("path", path);
                result.put("size", file.getSize());
                
                return ResponseEntity.ok(result);
            } finally {
                // 删除临时文件
                if (!tempFile.delete()) {
                    tempFile.deleteOnExit();
                }
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 下载文件
     */
    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(@RequestParam String path) {
        try {
            if (!hdfsClient.exists(path)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "文件不存在: " + path));
            }

            try (InputStream is = hdfsClient.getInputStream(path)) {
                // 读取文件内容到字节数组
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                byte[] fileBytes = baos.toByteArray();

                // 提取文件名
                String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
                
                // 设置响应头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", fileName);
                
                return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileBytes);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 删除文件或目录
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFile(@RequestParam String path, @RequestParam(defaultValue = "false") boolean recursive) {
        try {
            boolean success = hdfsClient.delete(path, recursive);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("path", path);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 移动或重命名文件
     */
    @PostMapping("/rename")
    public ResponseEntity<?> renameFile(@RequestParam String src, @RequestParam String dst) {
        try {
            boolean success = hdfsClient.rename(src, dst);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("source", src);
            result.put("destination", dst);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 检查文件是否存在
     */
    @GetMapping("/exists")
    public ResponseEntity<?> fileExists(@RequestParam String path) {
        try {
            boolean exists = hdfsClient.exists(path);
            
            Map<String, Object> result = new HashMap<>();
            result.put("exists", exists);
            result.put("path", path);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
} 