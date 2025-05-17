package org.shiguang.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class FileUtils {

    /**
     * 合并多个HDFS文件
     */
    public static void mergeFiles(FileSystem fileSystem, List<String> sourcePaths, String targetPath) throws IOException {
        log.info("Merging {} files into: {}", sourcePaths.size(), targetPath);
        Path hdfsTargetPath = new Path(targetPath);
        
        // 确保父目录存在
        Path parentPath = hdfsTargetPath.getParent();
        if (parentPath != null) {
            if (!fileSystem.exists(parentPath)) {
                log.info("Creating parent directory: {}", parentPath);
                fileSystem.mkdirs(parentPath);
            }
        }
        
        // 创建目标文件
        try (FSDataOutputStream outputStream = fileSystem.create(hdfsTargetPath)) {
            // 按顺序读取每个源文件并写入目标文件
            byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
            
            for (String sourcePath : sourcePaths) {
                Path hdfsSourcePath = new Path(sourcePath);
                
                if (!fileSystem.exists(hdfsSourcePath)) {
                    log.warn("Source file does not exist: {}, skipping", sourcePath);
                    continue;
                }
                
                try (FSDataInputStream inputStream = fileSystem.open(hdfsSourcePath)) {
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
            
            log.info("Successfully merged files into: {}", targetPath);
        } catch (IOException e) {
            log.error("Error merging files into: {}", targetPath, e);
            throw e;
        }
    }
    
    /**
     * 读取二进制文件内容
     */
    public static byte[] readBinaryFile(FileSystem fileSystem, String path) throws IOException {
        log.info("Reading binary file from path: {}", path);
        Path hdfsPath = new Path(path);

        if (!fileSystem.exists(hdfsPath)) {
            log.error("File does not exist: {}", path);
            throw new IOException("File does not exist: " + path);
        }

        try (FSDataInputStream inputStream = fileSystem.open(hdfsPath)) {
            FileStatus fileStatus = fileSystem.getFileStatus(hdfsPath);
            long length = fileStatus.getLen();
            // 注意：这里假设文件大小在int范围内，对于超大文件可能需要调整
            if (length > Integer.MAX_VALUE) {
                throw new IOException("File is too large to read into a byte array: " + length);
            }
            byte[] content = new byte[(int) length];
            inputStream.readFully(0, content);
            log.info("Binary file read successfully from: {}", path);
            return content;
        } catch (IOException e) {
            log.error("Failed to read binary file from: {}", path, e);
            throw e;
        }
    }
} 