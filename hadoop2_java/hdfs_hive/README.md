# HDFS和Hive接口模块

该模块提供了HDFS和Hive的REST API接口，可以通过HTTP请求进行文件操作和数据查询。

## 运行方式

使用以下命令启动应用：

```bash
mvn spring-boot:run
```

应用启动后，会在端口8080上提供REST API服务。

## HDFS API

基础URL: `/api/hdfs`

### 获取HDFS连接状态

- **GET** `/api/hdfs/status`
- 响应示例:
```json
{
  "connected": true,
  "uri": "hdfs://192.168.1.192:9000"
}
```

### 列出目录内容

- **GET** `/api/hdfs/list?path=/user/data`
- 参数: 
  - `path`: HDFS路径
- 响应示例:
```json
[
  {
    "path": "hdfs://192.168.1.192:9000/user/data/file1.txt",
    "name": "file1.txt",
    "isDirectory": false,
    "length": 1024,
    "modificationTime": 1688123456789,
    "owner": "hadoop",
    "group": "hadoop",
    "permission": "rw-r--r--"
  },
  {
    "path": "hdfs://192.168.1.192:9000/user/data/folder1",
    "name": "folder1",
    "isDirectory": true,
    "length": 0,
    "modificationTime": 1688123456700,
    "owner": "hadoop",
    "group": "hadoop",
    "permission": "rwxr-xr-x"
  }
]
```

### 创建目录

- **POST** `/api/hdfs/mkdir?path=/user/data/new-folder&permission=755`
- 参数: 
  - `path`: 要创建的目录路径
  - `permission`: 可选，权限字符串(如"755")
- 响应示例:
```json
{
  "success": true,
  "path": "/user/data/new-folder"
}
```

### 上传文件

- **POST** `/api/hdfs/upload`
- 参数: 
  - `file`: 文件数据 (multipart/form-data)
  - `path`: 目标HDFS路径
- 响应示例:
```json
{
  "success": true,
  "path": "/user/data/file.txt",
  "size": 1024
}
```

### 下载文件

- **GET** `/api/hdfs/download?path=/user/data/file.txt`
- 参数: 
  - `path`: HDFS文件路径
- 响应: 文件内容（二进制流）

### 删除文件或目录

- **DELETE** `/api/hdfs/delete?path=/user/data/file.txt&recursive=false`
- 参数: 
  - `path`: 要删除的路径
  - `recursive`: 可选，是否递归删除(默认false)
- 响应示例:
```json
{
  "success": true,
  "path": "/user/data/file.txt"
}
```

### 移动或重命名文件

- **POST** `/api/hdfs/rename?src=/user/data/old.txt&dst=/user/data/new.txt`
- 参数: 
  - `src`: 源路径
  - `dst`: 目标路径
- 响应示例:
```json
{
  "success": true,
  "source": "/user/data/old.txt",
  "destination": "/user/data/new.txt"
}
```

### 检查文件是否存在

- **GET** `/api/hdfs/exists?path=/user/data/file.txt`
- 参数: 
  - `path`: 要检查的路径
- 响应示例:
```json
{
  "exists": true,
  "path": "/user/data/file.txt"
}
```

## Hive API

基础URL: `/api/hive`

### 获取Hive连接状态

- **GET** `/api/hive/status`
- 响应示例:
```json
{
  "connected": true,
  "url": "jdbc:hive2://192.168.1.192:10000/default"
}
```

### 列出所有数据库

- **GET** `/api/hive/databases`
- 响应示例:
```json
["default", "test", "agriculture"]
```

### 列出数据库中的表

- **GET** `/api/hive/tables?database=default`
- 参数: 
  - `database`: 可选，数据库名称
- 响应示例:
```json
["users", "products", "orders"]
```

### 执行查询

- **POST** `/api/hive/query`
- 请求体:
```json
{
  "sql": "SELECT * FROM users LIMIT 10"
}
```
- 响应示例:
```json
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "age": 30
  },
  {
    "id": 2,
    "name": "Jane Smith",
    "email": "jane@example.com",
    "age": 25
  }
]
```

### 执行更新操作

- **POST** `/api/hive/update`
- 请求体:
```json
{
  "sql": "CREATE TABLE new_table (id INT, name STRING)"
}
```
- 响应示例:
```json
{
  "success": true,
  "rowsAffected": 0
}
```

### 获取表结构

- **GET** `/api/hive/schema?table=users&database=default`
- 参数: 
  - `table`: 表名
  - `database`: 可选，数据库名称
- 响应示例:
```json
[
  {
    "col_name": "id",
    "data_type": "int",
    "comment": ""
  },
  {
    "col_name": "name",
    "data_type": "string",
    "comment": "用户名"
  },
  {
    "col_name": "email",
    "data_type": "string",
    "comment": "邮箱地址"
  },
  {
    "col_name": "age",
    "data_type": "int",
    "comment": "年龄"
  }
]
```

### 创建表

- **POST** `/api/hive/table`
- 请求体:
```json
{
  "name": "new_table",
  "columns": [
    {
      "name": "id",
      "type": "INT",
      "comment": "唯一标识"
    },
    {
      "name": "name",
      "type": "STRING",
      "comment": "名称"
    }
  ],
  "comment": "新建表",
  "fileFormat": "TEXTFILE"
}
```
- 响应示例:
```json
{
  "success": true,
  "tableName": "new_table"
}
```

### 删除表

- **DELETE** `/api/hive/table?name=old_table`
- 参数: 
  - `name`: 表名
- 响应示例:
```json
{
  "success": true,
  "tableName": "old_table"
}
``` 