package org.shiguang.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.permission.FsPermission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HDFS客户端，提供与HDFS文件系统交互的功能
 */
@Component
public class HDFSClient {
    private static final Logger logger = LoggerFactory.getLogger(HDFSClient.class);

    @Value("${hadoop.hdfs.uri:hdfs://192.168.1.192:9000}")
    private String hdfsUri;

    @Value("${hadoop.hdfs.user:root}")
    private String hdfsUser;
    
    @Value("${hadoop.hdfs.connection.required:false}")
    private boolean connectionRequired;

    private FileSystem fileSystem;
    private Configuration configuration;
    private boolean connected = false;

    @PostConstruct
    public void init() {
        configuration = new Configuration();
        // 可以在这里设置更多配置
        configuration.set("dfs.client.use.datanode.hostname", "true");
        configuration.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
        
        try {
            // 获取文件系统实例
            logger.info("正在连接HDFS: {}", hdfsUri);
            fileSystem = FileSystem.get(new URI(hdfsUri), configuration, hdfsUser);
            
            // 确保根目录存在
            ensureRootDirectoryExists();
            connected = true;
            logger.info("HDFS连接成功: {}", hdfsUri);
        } catch (Exception e) {
            logger.error("HDFS连接失败: {}", e.getMessage());
            if (connectionRequired) {
                throw new RuntimeException("无法连接到HDFS，应用无法启动", e);
            } else {
                logger.warn("HDFS不可用，应用将以有限功能运行");
            }
        }
    }

    /**
     * 确保根目录存在并且具有正确的权限
     */
    private void ensureRootDirectoryExists() throws IOException {
        if (!connected || fileSystem == null) {
            logger.warn("HDFS未连接，无法创建根目录");
            return;
        }
        
        // 从配置中获取根目录（如果没有则使用默认值）
        String rootDir = configuration.get("hadoop.hdfs.root.dir", "/user/" + hdfsUser);
        Path rootPath = new Path(rootDir);
        
        // 检查目录是否存在，不存在则创建
        if (!fileSystem.exists(rootPath)) {
            // 创建目录并设置777权限（所有用户可读写执行）
            boolean created = fileSystem.mkdirs(rootPath);
            if (created) {
                FsPermission permission = new FsPermission((short)0777);
                fileSystem.setPermission(rootPath, permission);
                logger.info("创建HDFS根目录: {} 权限: 777", rootDir);
            } else {
                logger.error("无法创建HDFS根目录: {}", rootDir);
            }
        }
    }

    /**
     * 检查HDFS连接状态
     * @return 是否已连接
     */
    public boolean isConnected() {
        return connected && fileSystem != null;
    }

    /**
     * 创建目录
     * @param path 路径
     * @param permission 权限（如"755"）
     * @return 是否创建成功
     */
    public boolean mkdir(String path, String permission) throws IOException {
        if (!isConnected()) {
            logger.error("HDFS未连接，无法创建目录: {}", path);
            throw new IOException("HDFS未连接，无法创建目录");
        }
        
        // 确保路径始终以/开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        Path hdfsPath = new Path(path);
        if (!fileSystem.exists(hdfsPath)) {
            FsPermission fsPermission;
            if (permission != null) {
                // 使用提供的权限
                try {
                    // 尝试将权限字符串（如"755"）转换为短整型
                    short permissionValue = Short.parseShort(permission, 8);
                    fsPermission = new FsPermission(permissionValue);
                } catch (NumberFormatException e) {
                    // 如果转换失败，使用默认权限
                    fsPermission = FsPermission.getDirDefault();
                }
            } else {
                // 默认755权限
                fsPermission = FsPermission.getDirDefault();
            }
            
            boolean created = fileSystem.mkdirs(hdfsPath, fsPermission);
            if (created) {
                // 设置权限以确保正确应用
                fileSystem.setPermission(hdfsPath, fsPermission);
            }
            return created;
        }
        return true;
    }
    
    /**
     * 创建目录（使用默认权限）
     * @param path 路径
     * @return 是否创建成功
     */
    public boolean mkdir(String path) throws IOException {
        return mkdir(path, null);
    }

    /**
     * 上传文件到HDFS
     * @param localPath 本地路径
     * @param hdfsPath HDFS路径
     * @throws IOException 如果发生I/O错误
     */
    public void uploadFile(String localPath, String hdfsPath) throws IOException {
        if (!isConnected()) {
            logger.error("HDFS未连接，无法上传文件");
            throw new IOException("HDFS未连接，无法上传文件");
        }
        
        Path src = new Path(localPath);
        Path dst = new Path(hdfsPath);
        fileSystem.copyFromLocalFile(src, dst);
    }

    /**
     * 从HDFS下载文件
     * @param hdfsPath HDFS路径
     * @param localPath 本地路径
     * @throws IOException 如果发生I/O错误
     */
    public void downloadFile(String hdfsPath, String localPath) throws IOException {
        if (!isConnected()) {
            logger.error("HDFS未连接，无法下载文件");
            throw new IOException("HDFS未连接，无法下载文件");
        }
        
        Path src = new Path(hdfsPath);
        Path dst = new Path(localPath);
        fileSystem.copyToLocalFile(src, dst);
    }

    /**
     * 删除HDFS文件或目录
     * @param path HDFS路径
     * @param recursive 是否递归删除
     * @return 是否删除成功
     * @throws IOException 如果发生I/O错误
     */
    public boolean delete(String path, boolean recursive) throws IOException {
        if (!isConnected()) {
            logger.error("HDFS未连接，无法删除文件");
            throw new IOException("HDFS未连接，无法删除文件");
        }
        
        return fileSystem.delete(new Path(path), recursive);
    }

    /**
     * 获取文件输入流
     * @param hdfsPath HDFS文件路径
     * @return 文件输入流
     * @throws IOException 如果发生I/O错误
     */
    public FSDataInputStream getInputStream(String hdfsPath) throws IOException {
        if (!isConnected()) {
            logger.error("HDFS未连接，无法获取输入流");
            throw new IOException("HDFS未连接，无法获取输入流");
        }
        
        Path path = new Path(hdfsPath);
        return fileSystem.open(path);
    }

    /**
     * 获取文件输出流
     * @param hdfsPath HDFS文件路径
     * @param overwrite 是否覆盖已有文件
     * @return 文件输出流
     * @throws IOException 如果发生I/O错误
     */
    public FSDataOutputStream getOutputStream(String hdfsPath, boolean overwrite) throws IOException {
        if (!isConnected()) {
            logger.error("HDFS未连接，无法获取输出流");
            throw new IOException("HDFS未连接，无法获取输出流");
        }
        
        Path path = new Path(hdfsPath);
        return fileSystem.create(path, overwrite);
    }

    /**
     * 检查文件或目录是否存在
     * @param path HDFS路径
     * @return 是否存在
     * @throws IOException 如果发生I/O错误
     */
    public boolean exists(String path) throws IOException {
        if (!isConnected()) {
            logger.error("HDFS未连接，无法检查文件是否存在");
            throw new IOException("HDFS未连接，无法检查文件是否存在");
        }
        
        return fileSystem.exists(new Path(path));
    }
    
    /**
     * 列出目录中的文件和子目录
     * @param path HDFS目录路径
     * @return 文件和目录列表
     * @throws IOException 如果发生I/O错误
     */
    public List<Map<String, Object>> listFiles(String path) throws IOException {
        if (!isConnected()) {
            logger.error("HDFS未连接，无法列出文件");
            throw new IOException("HDFS未连接，无法列出文件");
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
     * 重命名文件或目录
     * @param src 源路径
     * @param dst 目标路径
     * @return 是否重命名成功
     * @throws IOException 如果发生I/O错误
     */
    public boolean rename(String src, String dst) throws IOException {
        if (!isConnected()) {
            logger.error("HDFS未连接，无法重命名文件");
            throw new IOException("HDFS未连接，无法重命名文件");
        }
        
        return fileSystem.rename(new Path(src), new Path(dst));
    }

    /**
     * 获取文件系统
     * @return FileSystem实例
     */
    public FileSystem getFileSystem() {
        return fileSystem;
    }

    /**
     * 关闭文件系统
     * @throws IOException 如果发生I/O错误
     */
    @PreDestroy
    public void close() throws IOException {
        if (fileSystem != null) {
            fileSystem.close();
        }
    }
} 