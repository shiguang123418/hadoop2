package org.shiguang.controller;

import org.shiguang.hadoop.HDFSClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * HDFS操作的REST API控制器
 */
@RestController
@RequestMapping("/hdfs")
@CrossOrigin
public class HDFSController {
    private static final Logger logger = LoggerFactory.getLogger(HDFSController.class);

    // API响应基类
    public static class ApiResponse {
        private boolean success;
        private String message;

        public ApiResponse() {
        }

        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    // 错误响应类
    public static class ErrorResponse extends ApiResponse {
        private String error;

        public ErrorResponse() {
            setSuccess(false);
        }

        public ErrorResponse(String error) {
            setSuccess(false);
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    // 状态响应类
    public static class StatusResponse extends ApiResponse {
        private boolean connected;
        private String uri;

        public StatusResponse(boolean connected) {
            super(true, connected ? "已连接" : "未连接");
            this.connected = connected;
        }

        public boolean isConnected() {
            return connected;
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }

    // 文件操作响应类
    public static class FileOperationResponse extends ApiResponse {
        private String path;
        
        public FileOperationResponse() {
            setSuccess(true);
        }
        
        public FileOperationResponse(boolean success, String path) {
            setSuccess(success);
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
    
    // 文件重命名响应类
    public static class RenameResponse extends ApiResponse {
        private String source;
        private String destination;
        
        public RenameResponse(boolean success, String source, String destination) {
            setSuccess(success);
            this.source = source;
            this.destination = destination;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }
    }
    
    // 文件存在检查响应类
    public static class ExistsResponse extends ApiResponse {
        private boolean exists;
        private String path;
        
        public ExistsResponse(boolean exists, String path) {
            setSuccess(true);
            this.exists = exists;
            this.path = path;
        }

        public boolean isExists() {
            return exists;
        }

        public void setExists(boolean exists) {
            this.exists = exists;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
    
    // 上传文件响应类
    public static class UploadResponse extends FileOperationResponse {
        private long size;
        
        public UploadResponse(boolean success, String path, long size) {
            super(success, path);
            this.size = size;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    // 创建目录请求类
    public static class MkdirRequest {
        private String path;
        private String permission;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }
        
        @Override
        public String toString() {
            return "MkdirRequest{path='" + path + "', permission='" + permission + "'}";
        }
    }

    // 重命名请求类
    public static class RenameRequest {
        private String src;
        private String dst;
        
        public String getSrc() {
            return src;
        }
        
        public void setSrc(String src) {
            this.src = src;
        }
        
        public String getDst() {
            return dst;
        }
        
        public void setDst(String dst) {
            this.dst = dst;
        }
        
        @Override
        public String toString() {
            return "RenameRequest{src='" + src + "', dst='" + dst + "'}";
        }
    }

    @Autowired
    private HDFSClient hdfsClient;

    /**
     * 获取HDFS连接状态
     */
    @GetMapping("/status")
    public ResponseEntity<Object> getStatus() {
        logger.info("获取HDFS连接状态");
        try {
            boolean connected = hdfsClient.isConnected();
            StatusResponse status = new StatusResponse(connected);
            if (connected) {
                status.setUri(hdfsClient.getFileSystem().getUri().toString());
            }
            logger.info("HDFS连接状态: {}", status.isConnected());
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("获取HDFS连接状态失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 列出目录内容
     */
    @GetMapping("/list")
    public ResponseEntity<?> listFiles(@RequestParam String path) {
        logger.info("列出HDFS目录内容: {}", path);
        try {
            if (!hdfsClient.isConnected()) {
                logger.error("列出目录失败: HDFS未连接");
                ErrorResponse error = new ErrorResponse("HDFS未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            List<Map<String, Object>> files = hdfsClient.listFiles(path);
            logger.info("列出目录成功, 文件数量: {}", files.size());
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            logger.error("列出HDFS目录失败: " + path, e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 创建目录
     */
    @PostMapping("/mkdir")
    public ResponseEntity<?> mkdir(@RequestBody(required = false) Object requestBody) {
        try {
            String path = null;
            String permission = null;
            
            logger.info("收到创建目录请求, 请求体类型: {}", requestBody != null ? requestBody.getClass().getName() : "null");
            if (requestBody != null) {
                logger.debug("请求体内容: {}", requestBody.toString());
            }
            
            // 从请求体中提取参数
            if (requestBody instanceof MkdirRequest) {
                // 如果是MkdirRequest类型
                MkdirRequest request = (MkdirRequest) requestBody;
                path = request.getPath();
                permission = request.getPermission();
                logger.info("MkdirRequest: {}", request);
            } else if (requestBody instanceof Map) {
                // 如果是Map类型
                @SuppressWarnings("unchecked")
                Map<String, Object> requestMap = (Map<String, Object>) requestBody;
                path = (String) requestMap.get("path");
                permission = (String) requestMap.get("permission");
                logger.info("Map请求: path={}, permission={}", path, permission);
            }
            
            // 验证路径参数
            if (path == null || path.isEmpty()) {
                logger.warn("创建目录失败: 路径参数为空");
                ErrorResponse error = new ErrorResponse("路径参数不能为空");
                return ResponseEntity.badRequest().body(error);
            }
            
            // 检查HDFS连接状态
            if (!hdfsClient.isConnected()) {
                logger.error("创建目录失败: HDFS未连接");
                ErrorResponse error = new ErrorResponse("HDFS未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            logger.info("开始创建HDFS目录: {}, 权限: {}", path, permission);
            boolean success;
            if (permission != null) {
                success = hdfsClient.mkdir(path, permission);
            } else {
                success = hdfsClient.mkdir(path);
            }
            
            FileOperationResponse result = new FileOperationResponse(success, path);
            
            logger.info("创建HDFS目录{}: {}", success ? "成功" : "失败", path);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("创建HDFS目录异常", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String path,
            HttpServletRequest request) {
        try {
            // 获取路径参数，优先使用URL参数，如果没有则尝试从表单数据获取
            String targetPath = path;
            if (targetPath == null || targetPath.isEmpty()) {
                targetPath = request.getParameter("path");
            }
            
            // 验证必要参数
            if (targetPath == null || targetPath.isEmpty()) {
                ErrorResponse error = new ErrorResponse("目标路径不能为空");
                return ResponseEntity.badRequest().body(error);
            }

            if (file.isEmpty()) {
                ErrorResponse error = new ErrorResponse("上传文件不能为空");
                return ResponseEntity.badRequest().body(error);
            }
            
            // 检查HDFS连接状态
            if (!hdfsClient.isConnected()) {
                logger.error("上传文件失败: HDFS未连接");
                ErrorResponse error = new ErrorResponse("HDFS未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }

            // 创建临时文件
            File tempFile = File.createTempFile("hdfs_upload_", "_" + file.getOriginalFilename());
            try {
                // 将上传的文件内容保存到临时文件
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(file.getBytes());
                }
                
                // 上传文件到HDFS
                hdfsClient.uploadFile(tempFile.getAbsolutePath(), targetPath);
                
                UploadResponse result = new UploadResponse(true, targetPath, file.getSize());
                
                return ResponseEntity.ok(result);
            } finally {
                // 删除临时文件
                if (!tempFile.delete()) {
                    tempFile.deleteOnExit();
                }
            }
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 下载文件
     */
    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(@RequestParam String path) {
        try {
            // 检查HDFS连接状态
            if (!hdfsClient.isConnected()) {
                logger.error("下载文件失败: HDFS未连接");
                ErrorResponse error = new ErrorResponse("HDFS未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            if (!hdfsClient.exists(path)) {
                ErrorResponse error = new ErrorResponse("文件不存在: " + path);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
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
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 删除文件或目录
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFile(@RequestParam String path, @RequestParam(defaultValue = "false") boolean recursive) {
        try {
            // 检查HDFS连接状态
            if (!hdfsClient.isConnected()) {
                logger.error("删除文件失败: HDFS未连接");
                ErrorResponse error = new ErrorResponse("HDFS未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            boolean success = hdfsClient.delete(path, recursive);
            
            FileOperationResponse result = new FileOperationResponse(success, path);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 移动或重命名文件
     */
    @PostMapping("/rename")
    public ResponseEntity<?> renameFile(
            @RequestParam(required = false) String src, 
            @RequestParam(required = false) String dst,
            @RequestBody(required = false) Object requestBody) {
        try {
            // 优先使用请求体中的参数，其次使用URL参数
            String sourcePath = src;
            String destinationPath = dst;
            
            if (requestBody != null) {
                if (requestBody instanceof RenameRequest) {
                    RenameRequest request = (RenameRequest) requestBody;
                    if (sourcePath == null || sourcePath.isEmpty()) {
                        sourcePath = request.getSrc();
                    }
                    
                    if (destinationPath == null || destinationPath.isEmpty()) {
                        destinationPath = request.getDst();
                    }
                } else if (requestBody instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> requestMap = (Map<String, Object>) requestBody;
                    
                    if (sourcePath == null || sourcePath.isEmpty()) {
                        sourcePath = (String) requestMap.get("src");
                    }
                    
                    if (destinationPath == null || destinationPath.isEmpty()) {
                        destinationPath = (String) requestMap.get("dst");
                    }
                }
            }
            
            // 验证参数
            if (sourcePath == null || sourcePath.isEmpty()) {
                ErrorResponse error = new ErrorResponse("源路径不能为空");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (destinationPath == null || destinationPath.isEmpty()) {
                ErrorResponse error = new ErrorResponse("目标路径不能为空");
                return ResponseEntity.badRequest().body(error);
            }
            
            // 检查HDFS连接状态
            if (!hdfsClient.isConnected()) {
                logger.error("重命名文件失败: HDFS未连接");
                ErrorResponse error = new ErrorResponse("HDFS未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            boolean success = hdfsClient.rename(sourcePath, destinationPath);
            
            RenameResponse result = new RenameResponse(success, sourcePath, destinationPath);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 检查文件是否存在
     */
    @GetMapping("/exists")
    public ResponseEntity<?> fileExists(@RequestParam String path) {
        try {
            // 检查HDFS连接状态
            if (!hdfsClient.isConnected()) {
                logger.error("检查文件存在失败: HDFS未连接");
                ErrorResponse error = new ErrorResponse("HDFS未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            boolean exists = hdfsClient.exists(path);
            
            ExistsResponse result = new ExistsResponse(exists, path);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
} 