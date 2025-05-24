package org.shiguang.module.hdfs.service;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * HDFS服务接口
 */
public interface HDFSService {

    /**
     * 获取HDFS连接状态
     * @return 连接状态信息
     * @throws IOException 如果发生I/O错误
     */
    Map<String, Object> getConnectionStatus() throws IOException;
    
    /**
     * 获取HDFS文件系统实例
     * @return FileSystem实例
     */
    FileSystem getFileSystem();
    
    /**
     * 列出目录内容
     * @param path 目录路径
     * @return 文件和目录列表
     * @throws IOException 如果发生I/O错误
     */
    List<Map<String, Object>> listFiles(String path) throws IOException;
    
    /**
     * 创建目录
     * @param path 路径
     * @return 是否创建成功
     * @throws IOException 如果发生I/O错误
     */
    boolean mkdir(String path) throws IOException;
    
    /**
     * 创建目录（指定权限）
     * @param path 路径
     * @param permission 权限字符串
     * @return 是否创建成功
     * @throws IOException 如果发生I/O错误
     */
    boolean mkdir(String path, String permission) throws IOException;
    
    /**
     * 上传文件
     * @param file 上传的文件
     * @param path 上传目标路径
     * @return 上传结果信息
     * @throws IOException 如果发生I/O错误
     */
    Map<String, Object> uploadFile(MultipartFile file, String path) throws IOException;
    
    /**
     * 检查文件或目录是否存在
     * @param path 路径
     * @return 是否存在
     * @throws IOException 如果发生I/O错误
     */
    boolean exists(String path) throws IOException;
    
    /**
     * 获取文件信息
     * @param path 文件路径
     * @return 文件信息
     * @throws IOException 如果发生I/O错误
     */
    Map<String, Object> getFileInfo(String path) throws IOException;
    
    /**
     * 获取文件输入流
     * @param path 文件路径
     * @return 文件输入流
     * @throws IOException 如果发生I/O错误
     */
    FSDataInputStream getInputStream(String path) throws IOException;
    
    /**
     * 删除文件或目录
     * @param path 路径
     * @param recursive 是否递归删除
     * @return 是否删除成功
     * @throws IOException 如果发生I/O错误
     */
    boolean delete(String path, boolean recursive) throws IOException;
    
    /**
     * 重命名或移动文件/目录
     * @param src 源路径
     * @param dst 目标路径
     * @return 是否重命名成功
     * @throws IOException 如果发生I/O错误
     */
    boolean rename(String src, String dst) throws IOException;
} 