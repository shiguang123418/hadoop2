package org.shiguang.controller;


import org.shiguang.service.HdfsService;
import org.shiguang.service.HiveService;
import org.shiguang.service.HiveCommand;
import org.shiguang.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agriculture")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
@Slf4j
public class AgricultureController {

    private final HdfsService hdfsService;
    private final HiveService hiveService;

    /**
     * 上传数据到HDFS
     */
    @PostMapping("/data")
    public ResponseEntity<?> uploadData(@RequestBody String data, @RequestParam String path) {
        log.info("Received request to upload data to path: {}, data size: {} bytes", path, data != null ? data.length() : 0);
        try {
            if (data == null || data.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No data provided for upload"));
            }
            
            hdfsService.createFile(path, data);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Data uploaded successfully");
            response.put("path", path);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading data to path: {}", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error uploading data: " + e.getMessage()));
        }
    }

    /**
     * 从HDFS读取数据
     */
    @GetMapping("/data")
    public ResponseEntity<?> readData(@RequestParam String path) {
        log.info("Received request to read data from path: {}", path);
        try {
            String content = hdfsService.readFile(path);
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            log.error("Error reading data from path: {}", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error reading data: " + e.getMessage()));
        }
    }

    /**
     * 列出HDFS目录中的文件
     */
    @GetMapping("/files")
    public ResponseEntity<?> listFiles(@RequestParam String directory) {
        log.info("Received request to list files in directory: {}", directory);
        try {
            List<String> files = hdfsService.listFiles(directory);
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            log.error("Error listing files in directory: {}", directory, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error listing files: " + e.getMessage()));
        }
    }

    /**
     * 删除HDFS中的文件或目录
     */
    @DeleteMapping("/data")
    public ResponseEntity<?> deleteData(@RequestParam String path) {
        log.info("Received request to delete data at path: {}", path);
        try {
            boolean deleted = hdfsService.deleteFile(path);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "File deleted successfully", "path", path));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "File not found or could not be deleted", "path", path));
            }
        } catch (IOException e) {
            log.error("Error deleting data at path: {}", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error deleting data: " + e.getMessage()));
        }
    }

    /**
     * 检查HDFS中的文件或目录是否存在
     */
    @GetMapping("/exists")
    public ResponseEntity<?> fileExists(@RequestParam String path) {
        log.info("Received request to check if file exists at path: {}", path);
        try {
            boolean exists = hdfsService.exists(path);
            return ResponseEntity.ok(Map.of("exists", exists, "path", path));
        } catch (IOException e) {
            log.error("Error checking if file exists at path: {}", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error checking file existence: " + e.getMessage()));
        }
    }

    /**
     * 创建HDFS目录
     */
    @PostMapping("/directory")
    public ResponseEntity<?> createDirectory(@RequestParam String path) {
        log.info("Received request to create directory at path: {}", path);
        try {
            boolean created = hdfsService.makeDirectory(path);
            if (created) {
                return ResponseEntity.ok(Map.of("message", "Directory created successfully", "path", path));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Could not create directory", "path", path));
            }
        } catch (IOException e) {
            log.error("Error creating directory at path: {}", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating directory: " + e.getMessage()));
        }
    }

    /**
     * 执行Hive查询
     */
    @PostMapping("/query")
    public ResponseEntity<?> executeQuery(@RequestBody String query) {

        log.info("Received request to execute Hive query. Raw query: '{}'", query);
        try {
            // 记录查询内容的字节表示，帮助发现不可见字符
            if (log.isDebugEnabled()) {
                byte[] bytes = query.getBytes();
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) {
                    sb.append(String.format("%02X ", b));
                }
                log.debug("Query bytes: {}", sb.toString());
            }
            
            List<Map<String, Object>> result = hiveService.executeQuery(query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error executing Hive query", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error executing query: " + e.getMessage()));
        }
    }

    /**
     * Hive控制台 - 执行任意Hive命令
     */
    @PostMapping("/console")
    public ResponseEntity<?> executeHiveCommand(@RequestBody String command) {
        log.info("Received request to execute Hive command. Raw command: '{}'", command);
        try {
            // 记录命令内容的字节表示，帮助发现不可见字符
            if (log.isDebugEnabled()) {
                byte[] bytes = command.getBytes();
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) {
                    sb.append(String.format("%02X ", b));
                }
                log.debug("Command bytes: {}", sb.toString());
            }
            
            Object result = hiveService.executeHiveCommand(command);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error executing Hive command", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "命令执行失败: " + e.getMessage()));
        }
    }

    /**
     * 创建Hive表
     */
    @PostMapping("/create-table")
    public ResponseEntity<?> createTable(@RequestParam String tableName, @RequestParam String schema) {
        log.info("Received request to create Hive table: {} with schema: {}", tableName, schema);
        try {
            hiveService.createTable(tableName, schema);
            return ResponseEntity.ok(Map.of("message", "Table created successfully", "tableName", tableName));
        } catch (Exception e) {
            log.error("Error creating Hive table", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating table: " + e.getMessage()));
        }
    }

    /**
     * 向Hive表中插入数据
     */
    @PostMapping("/insert-data")
    public ResponseEntity<?> insertData(@RequestParam String tableName, @RequestParam String values) {
        log.info("Received request to insert data into Hive table: {} with values: {}", tableName, values);
        try {
            hiveService.insertData(tableName, values);
            return ResponseEntity.ok(Map.of("message", "Data inserted successfully", "tableName", tableName));
        } catch (Exception e) {
            log.error("Error inserting data into Hive table", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error inserting data: " + e.getMessage()));
        }
    }

    /**
     * 检查Hive表是否存在
     */
    @GetMapping("/table-exists")
    public ResponseEntity<?> tableExists(@RequestParam String tableName) {
        log.info("Received request to check if Hive table exists: {}", tableName);
        try {
            boolean exists = hiveService.tableExists(tableName);
            return ResponseEntity.ok(Map.of("exists", exists, "tableName", tableName));
        } catch (Exception e) {
            log.error("Error checking if Hive table exists", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error checking table existence: " + e.getMessage()));
        }
    }

    /**
     * 删除Hive表
     */
    @DeleteMapping("/table")
    public ResponseEntity<?> dropTable(@RequestParam String tableName) {
        log.info("Received request to drop Hive table: {}", tableName);
        try {
            hiveService.dropTable(tableName);
            return ResponseEntity.ok(Map.of("message", "Table dropped successfully", "tableName", tableName));
        } catch (Exception e) {
            log.error("Error dropping Hive table", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error dropping table: " + e.getMessage()));
        }
    }

    /**
     * 获取Hive表结构
     */
    @GetMapping("/describe-table")
    public ResponseEntity<?> describeTable(@RequestParam String tableName) {
        log.info("Received request to describe Hive table: {}", tableName);
        try {
            List<Map<String, Object>> tableStructure = hiveService.describeTable(tableName);
            return ResponseEntity.ok(tableStructure);
        } catch (Exception e) {
            log.error("Error describing Hive table", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error describing table: " + e.getMessage()));
        }
    }

    /**
     * 执行标准化Hive命令
     */
    @PostMapping("/hive/standard")
    public ResponseEntity<?> executeStandardHiveCommand(@RequestBody HiveCommand command) {
        try {
            log.info("收到标准化Hive命令: {}", command.getType());
            Object result = hiveService.executeStandardCommand(command);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("执行标准化Hive命令出错", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 创建表 - 使用标准化列定义
     */
    @PostMapping("/hive/tables")
    public ResponseEntity<?> createTableWithColumns(
            @RequestParam String tableName,
            @RequestBody List<HiveCommand.TableColumn> columns) {
        try {
            log.info("使用标准化列定义创建表: {}, 列数: {}", tableName, columns.size());
            hiveService.createTableFromColumns(tableName, columns);
            return ResponseEntity.ok(Map.of(
                "message", "表创建成功",
                "tableName", tableName,
                "columnsCount", columns.size()
            ));
        } catch (Exception e) {
            log.error("创建表出错", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 获取表结构 - 使用标准化接口
     */
    @GetMapping("/hive/tables/{tableName}/schema")
    public ResponseEntity<?> getTableSchema(@PathVariable String tableName) {
        try {
            log.info("获取表结构: {}", tableName);
            HiveCommand command = new HiveCommand();
            command.setType("DESCRIBE");
            command.setTableName(tableName);
            List<Map<String, Object>> schema = (List<Map<String, Object>>) 
                hiveService.executeStandardCommand(command);
            return ResponseEntity.ok(schema);
        } catch (Exception e) {
            log.error("获取表结构出错", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 上传文件到HDFS (新版本)
     */
    @PostMapping("/upload-file")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam String path) {
        log.info("Received request to upload file to path: {}, file size: {} bytes", path, file.getSize());
        try {
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No file provided for upload"));
            }

            // 直接使用二进制方式上传文件
            hdfsService.createBinaryFile(path, file.getBytes());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "File uploaded successfully");
            response.put("path", path);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading file to path: {}", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error uploading file: " + e.getMessage()));
        }
    }

    /**
     * 合并HDFS文件分片
     */
    @PostMapping("/merge-chunks")
    public ResponseEntity<?> mergeChunks(
            @RequestParam String targetPath,
            @RequestParam int totalChunks,
            @RequestParam String fileName) {
        log.info("Received request to merge {} chunks for file: {}, target path: {}", 
                totalChunks, fileName, targetPath);
        try {
            // 构建所有分片的路径
            List<String> chunkPaths = new ArrayList<>();
            for (int i = 0; i < totalChunks; i++) {
                chunkPaths.add(targetPath + ".part" + i);
            }
            
            // 获取 FileSystem 实例，如果方法存在
            org.apache.hadoop.fs.FileSystem fs = null;
            try {
                fs = hdfsService.getFileSystem();
            } catch (NoSuchMethodError e) {
                log.error("Method 'getFileSystem' not found in HdfsService.", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Server configuration error: getFileSystem method missing"));
            }
            
            if (fs == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Could not get HDFS FileSystem instance"));
            }
            
            // 合并文件
            FileUtils.mergeFiles(fs, chunkPaths, targetPath);
            
            // 删除分片文件
            for (String chunkPath : chunkPaths) {
                try {
                    hdfsService.deleteFile(chunkPath);
                } catch (IOException e) {
                    log.warn("Failed to delete chunk file: {}", chunkPath, e);
                    // 继续处理，不中断流程
                }
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "File chunks merged successfully");
            response.put("path", targetPath);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error merging file chunks for: {}", targetPath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error merging file chunks: " + e.getMessage()));
        } catch (Exception e) { // 捕获潜在的FileUtils未找到等其他异常
            log.error("Unexpected error during chunk merging: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected server error during merging"));
        }
    }
}