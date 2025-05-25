package org.shiguang.module.oss.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 阿里云OSS服务接口
 */
public interface OssService {

    /**
     * 上传文件到OSS
     * @param file 文件
     * @param dir 存储目录（可选）
     * @return 包含URL和其他信息的Map
     */
    Map<String, String> uploadFile(MultipartFile file, String dir) throws IOException;
    
    /**
     * 删除OSS中的文件
     * @param url 文件URL或者文件路径
     * @return 是否删除成功
     */
    boolean deleteFile(String url);
} 