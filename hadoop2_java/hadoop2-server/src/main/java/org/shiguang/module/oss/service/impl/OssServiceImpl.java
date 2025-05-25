package org.shiguang.module.oss.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import org.shiguang.module.oss.config.OssConfig;
import org.shiguang.module.oss.service.OssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OssServiceImpl implements OssService {

    private static final Logger logger = LoggerFactory.getLogger(OssServiceImpl.class);

    @Autowired
    private OSS ossClient;

    @Autowired
    private OssConfig ossConfig;

    @Override
    public Map<String, String> uploadFile(MultipartFile file, String dir) throws IOException {
        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString().replaceAll("-", "") + extension;
        
        // 构建文件路径
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String fileDir = dir == null || dir.isEmpty() ? "uploads/" : dir;
        String objectKey = fileDir + datePath + "/" + fileName;
        
        logger.info("准备上传文件: 原始名称={}, 新文件名={}, 存储路径={}", originalFilename, fileName, objectKey);
        
        // 设置文件元信息
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(getContentType(extension));
        metadata.setContentLength(file.getSize());
        
        // 上传文件
        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(ossConfig.getBucketName(), objectKey, inputStream, metadata);
            
            // 构建返回结果
            Map<String, String> result = new HashMap<>();
            
            // 确保URL格式正确，修复URL前缀问题
            String urlPrefix = ossConfig.getUrlPrefix();
            
            // 检查URL前缀是否以http开头
            if (!urlPrefix.startsWith("http")) {
                logger.warn("URL前缀不是有效的URL: {}", urlPrefix);
                urlPrefix = "http://" + urlPrefix;
                logger.info("已修正URL前缀: {}", urlPrefix);
            }
            
            // 确保URL前缀和objectKey之间有正确的分隔符
            if (!urlPrefix.endsWith("/") && !objectKey.startsWith("/")) {
                urlPrefix = urlPrefix + "/";
                logger.info("已添加URL分隔符: {}", urlPrefix);
            }
            
            String url = urlPrefix + objectKey;
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

    @Override
    public boolean deleteFile(String url) {
        try {
            // 如果传入的是完整URL，则需要提取objectKey
            String objectKey = url;
            if (url.startsWith(ossConfig.getUrlPrefix())) {
                objectKey = url.substring(ossConfig.getUrlPrefix().length());
            }
            
            ossClient.deleteObject(ossConfig.getBucketName(), objectKey);
            logger.info("文件删除成功: {}", url);
            return true;
        } catch (Exception e) {
            logger.error("文件删除失败: {}", url, e);
            return false;
        }
    }
    
    /**
     * 根据文件扩展名获取内容类型
     */
    private String getContentType(String extension) {
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
            default:
                return "application/octet-stream";
        }
    }
} 