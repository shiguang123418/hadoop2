package org.shiguang.utils.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 阿里云OSS工具类
 */
@Component
public class OssUtil {
    private static final Logger logger = LoggerFactory.getLogger(OssUtil.class);

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    @Value("${aliyun.oss.urlPrefix}")
    private String urlPrefix;

    private static String staticEndpoint;
    private static String staticAccessKeyId;
    private static String staticAccessKeySecret;
    private static String staticBucketName;
    private static String staticUrlPrefix;
    private static OSS ossClient;
    private static boolean initialized = false;
    
    /**
     * 从配置文件中读取配置并初始化OSS客户端
     */
    @PostConstruct
    public void init() {
        logger.info("正在初始化OSS工具类...");
        try {
            staticEndpoint = endpoint;
            staticAccessKeyId = accessKeyId;
            staticAccessKeySecret = accessKeySecret;
            staticBucketName = bucketName;
            staticUrlPrefix = urlPrefix;
            
            ossClient = new OSSClientBuilder().build(staticEndpoint, staticAccessKeyId, staticAccessKeySecret);
            initialized = true;
            logger.info("OSS工具类初始化成功，端点: {}, 存储桶: {}", staticEndpoint, staticBucketName);
        } catch (Exception e) {
            logger.error("OSS工具类初始化失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 应用关闭时关闭OSS客户端
     */
    @PreDestroy
    public void destroy() {
        shutdown();
    }
    
    /**
     * 检查是否初始化
     */
    private static void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("OSS工具类尚未初始化");
        }
    }
    
    /**
     * 上传文件到OSS
     * @param file 文件
     * @param dir 存储目录（可选）
     * @return 包含URL和其他信息的Map
     */
    public static Map<String, String> uploadFile(MultipartFile file, String dir) throws IOException {
        checkInitialized();
        
        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString().replaceAll("-", "") + extension;
        
        // 构建文件路径
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String fileDir = dir == null || dir.isEmpty() ? "uploads/" : dir;
        if (!fileDir.endsWith("/")) {
            fileDir += "/";
        }
        String objectKey = fileDir + datePath + "/" + fileName;
        
        logger.info("准备上传文件: 原始名称={}, 新文件名={}, 存储路径={}", originalFilename, fileName, objectKey);
        
        // 设置文件元信息
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(getContentType(extension));
        metadata.setContentLength(file.getSize());
        
        // 上传文件
        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(staticBucketName, objectKey, inputStream, metadata);
            
            // 构建返回结果
            Map<String, String> result = new HashMap<>();
            
            // 确保URL格式正确，修复URL前缀问题
            String finalUrlPrefix = staticUrlPrefix;
            
            // 检查URL前缀是否以http开头
            if (!finalUrlPrefix.startsWith("http")) {
                logger.warn("URL前缀不是有效的URL: {}", finalUrlPrefix);
                finalUrlPrefix = "http://" + finalUrlPrefix;
                logger.info("已修正URL前缀: {}", finalUrlPrefix);
            }
            
            // 确保URL前缀和objectKey之间有正确的分隔符
            if (!finalUrlPrefix.endsWith("/") && !objectKey.startsWith("/")) {
                finalUrlPrefix = finalUrlPrefix + "/";
                logger.info("已添加URL分隔符: {}", finalUrlPrefix);
            }
            
            String url = finalUrlPrefix + objectKey;
            logger.info("最终生成的文件URL: {}", url);
            
            result.put("url", url);
            result.put("filename", fileName);
            result.put("path", objectKey);
            result.put("size", String.valueOf(file.getSize()));
            result.put("type", file.getContentType());
            
            // 打印返回结果
            logger.info("文件上传结果: {}", result);
            
            return result;
        } catch (Exception e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            throw new IOException("文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传文件到OSS（使用默认目录）
     * @param file 文件
     * @return 包含URL和其他信息的Map
     */
    public static Map<String, String> uploadFile(MultipartFile file) throws IOException {
        return uploadFile(file, null);
    }

    /**
     * 删除OSS中的文件
     * @param url 文件URL或者文件路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String url) {
        checkInitialized();
        
        try {
            // 如果传入的是完整URL，则需要提取objectKey
            String objectKey = url;
            if (url.startsWith(staticUrlPrefix)) {
                objectKey = url.substring(staticUrlPrefix.length());
            }
            
            // 确保objectKey不以/开头
            if (objectKey.startsWith("/")) {
                objectKey = objectKey.substring(1);
            }
            
            ossClient.deleteObject(staticBucketName, objectKey);
            logger.info("文件删除成功: {}", url);
            return true;
        } catch (Exception e) {
            logger.error("文件删除失败: {}", url, e);
            return false;
        }
    }
    
    /**
     * 关闭OSS客户端
     */
    public static void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
            initialized = false;
            logger.info("OSS客户端已关闭");
        }
    }
    
    /**
     * 根据文件扩展名获取内容类型
     */
    private static String getContentType(String extension) {
        if (extension == null) {
            return "application/octet-stream";
        }
        
        switch (extension.toLowerCase()) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".bmp":
                return "image/bmp";
            case ".webp":
                return "image/webp";
            case ".svg":
                return "image/svg+xml";
            case ".pdf":
                return "application/pdf";
            case ".doc":
            case ".docx":
                return "application/msword";
            case ".xls":
            case ".xlsx":
                return "application/vnd.ms-excel";
            case ".ppt":
            case ".pptx":
                return "application/vnd.ms-powerpoint";
            case ".txt":
                return "text/plain";
            case ".html":
            case ".htm":
                return "text/html";
            case ".zip":
                return "application/zip";
            case ".rar":
                return "application/x-rar-compressed";
            case ".mp4":
                return "video/mp4";
            case ".mp3":
                return "audio/mp3";
            default:
                return "application/octet-stream";
        }
    }
} 