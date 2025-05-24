package org.shiguang.module.hdfs.service.impl;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.shiguang.module.hdfs.service.HDFSService;
import org.shiguang.utils.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HDFS服务实现类
 */
@Service
public class HDFSServiceImpl implements HDFSService {
    private static final Logger logger = LoggerFactory.getLogger(HDFSServiceImpl.class);
    
    private FileSystem fileSystem;
    
    @Value("${hadoop.hdfs.uri}")
    private String hdfsUri;
    
    @Value("${hadoop.hdfs.user:root}")
    private String hdfsUser;
    
    @Value("${hadoop.hdfs.connection.required:false}")
    private boolean connectionRequired;
    
    /**
     * 初始化HDFS连接
     */
    @PostConstruct
    public void init() {
        try {
            Configuration configuration = new Configuration();
            configuration.set("fs.defaultFS", hdfsUri);
            
            // 构建FileSystem实例
            fileSystem = FileSystem.get(URI.create(hdfsUri), configuration, hdfsUser);
            logger.info("HDFS服务初始化成功，连接到 {}", hdfsUri);
        } catch (Exception e) {
            logger.error("HDFS服务初始化失败: {}", e.getMessage());
            if (connectionRequired) {
                throw new RuntimeException("HDFS连接失败，无法继续", e);
            } else {
                logger.warn("HDFS连接失败，但由于配置未要求强制连接，服务将继续运行");
            }
        }
    }
    
    /**
     * 关闭HDFS连接
     */
    @PreDestroy
    public void close() {
        if (fileSystem != null) {
            try {
                fileSystem.close();
                logger.info("HDFS连接已关闭");
            } catch (IOException e) {
                logger.error("关闭HDFS连接时出错: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 获取HDFS连接状态
     */
    @Override
    public Map<String, Object> getConnectionStatus() throws IOException {
        Map<String, Object> statusInfo = new HashMap<>();
        boolean isConnected = fileSystem != null;
        statusInfo.put("connected", isConnected);
        
        if (isConnected) {
            statusInfo.put("uri", fileSystem.getUri().toString());
        }
        
        return statusInfo;
    }
    
    /**
     * 获取FileSystem实例
     */
    @Override
    public FileSystem getFileSystem() {
        return fileSystem;
    }
    
    /**
     * 检查HDFS连接状态
     */
    private boolean isConnected() {
        return fileSystem != null;
    }
    
    /**
     * 列出目录内容
     */
    @Override
    public List<Map<String, Object>> listFiles(String path) throws IOException {
        if (!isConnected()) {
            throw new IOException("HDFS未连接");
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        Path hdfsPath = new Path(path);
        
        if (!fileSystem.exists(hdfsPath)) {
            throw new IOException("路径不存在: " + path);
        }
        
        FileStatus[] fileStatuses = fileSystem.listStatus(hdfsPath);
        for (FileStatus status : fileStatuses) {
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("path", status.getPath().toString());
            fileInfo.put("name", status.getPath().getName());
            fileInfo.put("isDirectory", status.isDirectory());
            fileInfo.put("length", status.getLen());
            fileInfo.put("modificationTime", status.getModificationTime());
            fileInfo.put("owner", status.getOwner());
            fileInfo.put("group", status.getGroup());
            fileInfo.put("permission", status.getPermission().toString());
            result.add(fileInfo);
        }
        
        return result;
    }
    
    /**
     * 创建目录
     */
    @Override
    public boolean mkdir(String path) throws IOException {
        if (!isConnected()) {
            throw new IOException("HDFS未连接");
        }
        
        Path hdfsPath = new Path(path);
        if (fileSystem.exists(hdfsPath)) {
            return false;
        }
        
        return fileSystem.mkdirs(hdfsPath);
    }
    
    /**
     * 创建目录（指定权限）
     */
    @Override
    public boolean mkdir(String path, String permission) throws IOException {
        if (!isConnected()) {
            throw new IOException("HDFS未连接");
        }
        
        Path hdfsPath = new Path(path);
        if (fileSystem.exists(hdfsPath)) {
            return false;
        }
        
        if (permission != null && !permission.isEmpty()) {
            try {
                FsPermission fsPermission = new FsPermission(Short.parseShort(permission, 8));
                return fileSystem.mkdirs(hdfsPath, fsPermission);
            } catch (NumberFormatException e) {
                // 如果权限格式不正确，使用默认权限
                logger.warn("权限格式不正确: {}，使用默认权限", permission);
                return fileSystem.mkdirs(hdfsPath);
            }
        } else {
            return fileSystem.mkdirs(hdfsPath);
        }
    }
    
    /**
     * 上传文件
     */
    @Override
    public Map<String, Object> uploadFile(MultipartFile file, String path) throws IOException {
        if (!isConnected()) {
            throw new IOException("HDFS未连接");
        }
        
        String targetPath = path;
        if (targetPath == null || targetPath.isEmpty()) {
            targetPath = AppConstants.HDFS_DEFAULT_PATH;
        }
        
        // 如果路径不以/结尾，添加/
        if (!targetPath.endsWith("/")) {
            targetPath += "/";
        }
        
        // 添加文件名
        targetPath += file.getOriginalFilename();
        Path hdfsPath = new Path(targetPath);
        
        // 如果目标目录不存在，创建它
        Path parentPath = hdfsPath.getParent();
        if (parentPath != null && !fileSystem.exists(parentPath)) {
            fileSystem.mkdirs(parentPath);
        }
        
        // 创建HDFS文件输出流
        FSDataOutputStream outputStream = fileSystem.create(hdfsPath, true);
        
        // 从输入流复制到HDFS文件
        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[AppConstants.HDFS_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            
            Map<String, Object> result = new HashMap<>();
            result.put("path", targetPath);
            result.put("size", file.getSize());
            result.put("success", true);
            
            return result;
        }
    }
    
    /**
     * 检查文件或目录是否存在
     */
    @Override
    public boolean exists(String path) throws IOException {
        if (!isConnected()) {
            throw new IOException("HDFS未连接");
        }
        
        return fileSystem.exists(new Path(path));
    }
    
    /**
     * 获取文件信息
     */
    @Override
    public Map<String, Object> getFileInfo(String path) throws IOException {
        if (!isConnected()) {
            throw new IOException("HDFS未连接");
        }
        
        Path hdfsPath = new Path(path);
        if (!fileSystem.exists(hdfsPath)) {
            throw new IOException("文件或目录不存在: " + path);
        }
        
        FileStatus status = fileSystem.getFileStatus(hdfsPath);
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("name", status.getPath().getName());
        fileInfo.put("path", status.getPath().toString());
        fileInfo.put("isDirectory", status.isDirectory());
        fileInfo.put("length", status.getLen());
        fileInfo.put("owner", status.getOwner());
        fileInfo.put("group", status.getGroup());
        fileInfo.put("permission", status.getPermission().toString());
        fileInfo.put("modificationTime", status.getModificationTime());
        
        return fileInfo;
    }
    
    /**
     * 获取文件输入流
     */
    @Override
    public FSDataInputStream getInputStream(String path) throws IOException {
        if (!isConnected()) {
            throw new IOException("HDFS未连接");
        }
        
        Path hdfsPath = new Path(path);
        if (!fileSystem.exists(hdfsPath)) {
            throw new IOException("文件不存在: " + path);
        }
        
        if (fileSystem.getFileStatus(hdfsPath).isDirectory()) {
            throw new IOException("路径是一个目录，而不是文件: " + path);
        }
        
        return fileSystem.open(hdfsPath);
    }
    
    /**
     * 删除文件或目录
     */
    @Override
    public boolean delete(String path, boolean recursive) throws IOException {
        if (!isConnected()) {
            throw new IOException("HDFS未连接");
        }
        
        Path hdfsPath = new Path(path);
        if (!fileSystem.exists(hdfsPath)) {
            return false;
        }
        
        return fileSystem.delete(hdfsPath, recursive);
    }
    
    /**
     * 重命名或移动文件/目录
     */
    @Override
    public boolean rename(String src, String dst) throws IOException {
        if (!isConnected()) {
            throw new IOException("HDFS未连接");
        }
        
        Path srcPath = new Path(src);
        Path dstPath = new Path(dst);
        
        if (!fileSystem.exists(srcPath)) {
            throw new IOException("源文件或目录不存在: " + src);
        }
        
        return fileSystem.rename(srcPath, dstPath);
    }
} 