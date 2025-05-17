package org.shiguang.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.*;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HdfsService {

    private final FileSystem fileSystem;

    /**
     * 创建文件
     */
    public void createFile(String path, String content) throws IOException {
        log.info("Creating file at path: {}, content length: {} bytes", path, content.length());
        
        // 确保父目录存在
        tmp(path);

        // 创建文件并写入内容
        try (FSDataOutputStream outputStream = fileSystem.create(new Path(path))) {
            outputStream.write(content.getBytes());
            log.info("File created successfully at: {}", path);
        }
    }

    private void tmp(String path) throws IOException {
        Path parentPath = new Path(path).getParent();
        if (parentPath != null) {
            if (!fileSystem.exists(parentPath)) {
                log.info("Creating parent directory: {}", parentPath);
                fileSystem.mkdirs(parentPath);
            } else {
                log.info("Parent directory already exists: {}", parentPath);
            }
        }
    }

    /**
     * 创建二进制文件
     */
    public void createBinaryFile(String path, byte[] content) throws IOException {
        log.info("Creating binary file at path: {}, content length: {} bytes", path, content.length);
        
        // 确保父目录存在
        tmp(path);

        // 创建文件并写入二进制内容
        try (FSDataOutputStream outputStream = fileSystem.create(new Path(path))) {
            outputStream.write(content);
            log.info("Binary file created successfully at: {}", path);
        }
    }

    /**
     * 读取文件内容
     */
    public String readFile(String path) throws IOException {
        log.info("Reading file from path: {}", path);
        Path hdfsPath = new Path(path);

        if (!fileSystem.exists(hdfsPath)) {
            log.error("File does not exist: {}", path);
            throw new IOException("File does not exist: " + path);
        }

        try (FSDataInputStream inputStream = fileSystem.open(hdfsPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            log.info("File read successfully from: {}", path);
            return content.toString();
        } catch (IOException e) {
            log.error("Failed to read file from: {}", path, e);
            throw e;
        }
    }

    /**
     * 列出目录中的文件
     */
    public List<String> listFiles(String directory) throws IOException {
        log.info("Listing files in directory: {}", directory);
        Path hdfsPath = new Path(directory);
        List<String> fileList = new ArrayList<>();

        try {
            if (!fileSystem.exists(hdfsPath)) {
                log.warn("Directory does not exist: {}", directory);
                return fileList;
            }

            if (!fileSystem.isDirectory(hdfsPath)) {
                log.warn("Path is not a directory: {}", directory);
                fileList.add(hdfsPath.toString());
                return fileList;
            }

            RemoteIterator<LocatedFileStatus> fileStatusListIterator = fileSystem.listFiles(hdfsPath, true);
            while (fileStatusListIterator.hasNext()) {
                LocatedFileStatus fileStatus = fileStatusListIterator.next();
                fileList.add(fileStatus.getPath().toString());
            }

            log.info("Listed {} files in directory: {}", fileList.size(), directory);
            return fileList;
        } catch (IOException e) {
            log.error("Failed to list files in directory: {}", directory, e);
            throw e;
        }
    }

    /**
     * 删除文件或目录
     */
    public boolean deleteFile(String path) throws IOException {
        log.info("Deleting file or directory at path: {}", path);
        Path hdfsPath = new Path(path);

        try {
            if (!fileSystem.exists(hdfsPath)) {
                log.warn("File or directory does not exist: {}", path);
                return false;
            }

            boolean deleted = fileSystem.delete(hdfsPath, true);
            if (deleted) {
                log.info("Successfully deleted: {}", path);
            } else {
                log.warn("Failed to delete: {}", path);
            }

            return deleted;
        } catch (IOException e) {
            log.error("Error deleting file or directory: {}", path, e);
            throw e;
        }
    }

    /**
     * 检查文件或目录是否存在
     */
    public boolean exists(String path) throws IOException {
        Path hdfsPath = new Path(path);
        return fileSystem.exists(hdfsPath);
    }

    /**
     * 创建目录
     */
    public boolean makeDirectory(String path) throws IOException {
        log.info("Creating directory at path: {}", path);
        Path hdfsPath = new Path(path);

        try {
            if (fileSystem.exists(hdfsPath)) {
                log.info("Directory already exists: {}", path);
                return true;
            }

            boolean created = fileSystem.mkdirs(hdfsPath);
            if (created) {
                log.info("Successfully created directory: {}", path);
            } else {
                log.warn("Failed to create directory: {}", path);
            }

            return created;
        } catch (IOException e) {
            log.error("Error creating directory: {}", path, e);
            throw e;
        }
    }

    /**
     * 获取FileSystem实例
     */
    public FileSystem getFileSystem() {
        return fileSystem;
    }
}