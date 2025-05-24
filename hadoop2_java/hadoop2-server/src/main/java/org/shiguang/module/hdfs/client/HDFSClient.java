package org.shiguang.module.hdfs.client;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.commons.io.IOUtils;
import org.shiguang.module.hdfs.service.HDFSService;
import org.shiguang.utils.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HDFS客户端工具类
 * 提供HDFS文件系统操作的便捷方法
 */
@Component
public class HDFSClient {
    private static final Logger logger = LoggerFactory.getLogger(HDFSClient.class);

    @Autowired
    private HDFSService hdfsService;

    private FileSystem fileSystem;

    @PostConstruct
    public void init() {
        fileSystem = hdfsService.getFileSystem();
        logger.info("HDFS客户端初始化完成");
    }

    /**
     * 获取文件系统实例
     * @return FileSystem实例
     */
    public FileSystem getFileSystem() {
        return fileSystem;
    }

    /**
     * 列出目录内容
     * @param path 目录路径
     * @return 文件列表
     * @throws IOException IO异常
     */
    public List<Map<String, Object>> listFiles(String path) throws IOException {
        return hdfsService.listFiles(path);
    }

    /**
     * 创建目录
     * @param path 目录路径
     * @return 是否创建成功
     * @throws IOException IO异常
     */
    public boolean mkdir(String path) throws IOException {
        return hdfsService.mkdir(path);
    }

    /**
     * 创建目录（指定权限）
     * @param path 目录路径
     * @param permission 权限字符串
     * @return 是否创建成功
     * @throws IOException IO异常
     */
    public boolean mkdir(String path, String permission) throws IOException {
        return hdfsService.mkdir(path, permission);
    }

    /**
     * 检查文件/目录是否存在
     * @param path 路径
     * @return 是否存在
     * @throws IOException IO异常
     */
    public boolean exists(String path) throws IOException {
        return hdfsService.exists(path);
    }

    /**
     * 获取文件信息
     * @param path 文件路径
     * @return 文件信息
     * @throws IOException IO异常
     */
    public Map<String, Object> getFileInfo(String path) throws IOException {
        return hdfsService.getFileInfo(path);
    }

    /**
     * 删除文件或目录
     * @param path 路径
     * @param recursive 是否递归删除
     * @return 是否删除成功
     * @throws IOException IO异常
     */
    public boolean delete(String path, boolean recursive) throws IOException {
        return hdfsService.delete(path, recursive);
    }

    /**
     * 重命名或移动文件/目录
     * @param src 源路径
     * @param dst 目标路径
     * @return 是否重命名成功
     * @throws IOException IO异常
     */
    public boolean rename(String src, String dst) throws IOException {
        return hdfsService.rename(src, dst);
    }

    /**
     * 上传文件
     * @param file 待上传的文件
     * @param path 目标路径
     * @return 上传结果
     * @throws IOException IO异常
     */
    public Map<String, Object> uploadFile(MultipartFile file, String path) throws IOException {
        return hdfsService.uploadFile(file, path);
    }

    /**
     * 上传本地文件
     * @param localFilePath 本地文件路径
     * @param hdfsPath HDFS目标路径
     * @return 上传结果
     * @throws IOException IO异常
     */
    public Map<String, Object> uploadLocalFile(String localFilePath, String hdfsPath) throws IOException {
        File localFile = new File(localFilePath);
        if (!localFile.exists()) {
            throw new IOException("本地文件不存在: " + localFilePath);
        }
        
        if (hdfsPath == null || hdfsPath.isEmpty()) {
            hdfsPath = AppConstants.HDFS_DEFAULT_PATH;
        }
        
        if (!hdfsPath.endsWith("/")) {
            hdfsPath += "/";
        }
        
        hdfsPath += localFile.getName();
        Path destPath = new Path(hdfsPath);
        
        // 确保父目录存在
        Path parentPath = destPath.getParent();
        if (parentPath != null && !fileSystem.exists(parentPath)) {
            fileSystem.mkdirs(parentPath);
        }
        
        // 上传文件
        fileSystem.copyFromLocalFile(new Path(localFilePath), destPath);
        
        Map<String, Object> result = new HashMap<>();
        result.put("path", hdfsPath);
        result.put("size", localFile.length());
        result.put("success", true);
        
        return result;
    }
    
    /**
     * 下载文件到本地
     * @param hdfsPath HDFS文件路径
     * @param localPath 本地保存路径
     * @throws IOException IO异常
     */
    public void downloadFile(String hdfsPath, String localPath) throws IOException {
        if (!exists(hdfsPath)) {
            throw new IOException("HDFS文件不存在: " + hdfsPath);
        }
        
        Map<String, Object> fileInfo = getFileInfo(hdfsPath);
        if (Boolean.TRUE.equals(fileInfo.get("isDirectory"))) {
            throw new IOException("不能下载目录: " + hdfsPath);
        }
        
        // 如果本地路径是目录，则在其中创建与源文件同名的文件
        File localFile = new File(localPath);
        if (localFile.isDirectory()) {
            String fileName = new Path(hdfsPath).getName();
            localFile = new File(localPath, fileName);
        }
        
        // 确保父目录存在
        File parentDir = localFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // 下载文件
        fileSystem.copyToLocalFile(false, new Path(hdfsPath), new Path(localFile.getAbsolutePath()));
        
        logger.info("文件已下载到: {}", localFile.getAbsolutePath());
    }
    
    /**
     * 读取文本文件内容
     * @param hdfsPath HDFS文件路径
     * @return 文件内容
     * @throws IOException IO异常
     */
    public String readTextFile(String hdfsPath) throws IOException {
        FSDataInputStream inputStream = hdfsService.getInputStream(hdfsPath);
        try {
            return IOUtils.toString(inputStream, "UTF-8");
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
    
    /**
     * 写入文本文件
     * @param hdfsPath HDFS文件路径
     * @param content 文件内容
     * @throws IOException IO异常
     */
    public void writeTextFile(String hdfsPath, String content) throws IOException {
        Path path = new Path(hdfsPath);
        
        // 确保父目录存在
        Path parentPath = path.getParent();
        if (parentPath != null && !fileSystem.exists(parentPath)) {
            fileSystem.mkdirs(parentPath);
        }
        
        // 写入文件
        try (FSDataOutputStream out = fileSystem.create(path, true)) {
            out.write(content.getBytes("UTF-8"));
            out.flush();
        }
        
        logger.info("文本文件已写入到: {}", hdfsPath);
    }
    
    /**
     * 追加内容到文本文件
     * @param hdfsPath HDFS文件路径
     * @param content 要追加的内容
     * @throws IOException IO异常
     */
    public void appendToFile(String hdfsPath, String content) throws IOException {
        Path path = new Path(hdfsPath);
        
        if (!fileSystem.exists(path)) {
            writeTextFile(hdfsPath, content);
            return;
        }
        
        // 追加内容
        try (FSDataOutputStream out = fileSystem.append(path)) {
            out.write(content.getBytes("UTF-8"));
            out.flush();
        }
        
        logger.info("内容已追加到文件: {}", hdfsPath);
    }
} 