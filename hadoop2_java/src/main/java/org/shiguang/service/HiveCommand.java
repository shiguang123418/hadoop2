package org.shiguang.service;

import java.util.List;
import java.util.Map;

/**
 * Hive命令传输对象，规范前后端交互格式
 */
public class HiveCommand {
    // 命令类型: QUERY(查询), UPDATE(更新), DESCRIBE(表结构), CREATE_TABLE(建表), DROP_TABLE(删表)
    private String type;
    
    // SQL语句
    private String sql;
    
    // 表名(创建表、查询表结构等操作时使用)
    private String tableName;
    
    // 表结构(创建表时使用)
    private List<TableColumn> columns;
    
    // 数据(插入数据时使用)
    private List<Map<String, Object>> data;
    
    // getter和setter方法
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
    
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    
    public List<TableColumn> getColumns() { return columns; }
    public void setColumns(List<TableColumn> columns) { this.columns = columns; }
    
    public List<Map<String, Object>> getData() { return data; }
    public void setData(List<Map<String, Object>> data) { this.data = data; }
    
    /**
     * 表列定义
     */
    public static class TableColumn {
        private String name;        // 列名
        private String type;        // 数据类型
        private String comment;     // 注释
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
    
    /**
     * 根据命令类型和表定义生成标准SQL
     */
    public String generateSql() {
        if (sql != null && !sql.isEmpty()) {
            return sql;
        }
        
        switch (type.toUpperCase()) {
            case "CREATE_TABLE":
                return generateCreateTableSql();
            case "DESCRIBE":
                return "DESCRIBE " + tableName;
            case "DROP_TABLE":
                return "DROP TABLE IF EXISTS " + tableName;
            default:
                return sql;
        }
    }
    
    /**
     * 生成标准的CREATE TABLE语句
     */
    private String generateCreateTableSql() {
        if (tableName == null || columns == null || columns.isEmpty()) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
        
        for (int i = 0; i < columns.size(); i++) {
            TableColumn column = columns.get(i);
            sb.append(column.getName()).append(" ").append(column.getType());
            
            if (column.getComment() != null && !column.getComment().isEmpty()) {
                sb.append(" COMMENT '").append(column.getComment()).append("'");
            }
            
            if (i < columns.size() - 1) {
                sb.append(", ");
            }
        }
        
        sb.append(")");
        return sb.toString();
    }
} 