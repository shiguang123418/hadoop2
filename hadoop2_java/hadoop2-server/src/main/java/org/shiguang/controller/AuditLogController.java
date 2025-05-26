package org.shiguang.controller;

import org.shiguang.entity.AuditLog;
import org.shiguang.module.audit.AuditLogService;
import org.shiguang.module.audit.AuditOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 审计日志控制器
 */
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    /**
     * 获取审计日志列表
     */
    @GetMapping
    @AuditOperation(operation = "查询审计日志列表", operationType = "QUERY", resourceType = "AUDIT_LOG")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<AuditLog> auditLogs = auditLogService.getAuditLogs(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", auditLogs.getContent());
        response.put("totalElements", auditLogs.getTotalElements());
        response.put("totalPages", auditLogs.getTotalPages());
        response.put("currentPage", auditLogs.getNumber());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 通过ID获取审计日志详情
     */
    @GetMapping("/{id}")
    @AuditOperation(operation = "查询审计日志详情", operationType = "QUERY", resourceType = "AUDIT_LOG", resourceIdIndex = 0)
    public ResponseEntity<AuditLog> getAuditLogById(@PathVariable Long id) {
        Optional<AuditLog> auditLog = auditLogService.getAuditLogById(id);
        
        if (auditLog.isPresent()) {
            return ResponseEntity.ok(auditLog.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 搜索审计日志
     */
    @GetMapping("/search")
    @AuditOperation(operation = "搜索审计日志", operationType = "QUERY", resourceType = "AUDIT_LOG")
    public ResponseEntity<Map<String, Object>> searchAuditLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<AuditLog> auditLogs = auditLogService.searchAuditLogs(
            username,
            operationType,
            resourceType,
            startTime,
            endTime,
            pageable
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", auditLogs.getContent());
        response.put("totalElements", auditLogs.getTotalElements());
        response.put("totalPages", auditLogs.getTotalPages());
        response.put("currentPage", auditLogs.getNumber());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 删除审计日志
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @AuditOperation(operation = "删除审计日志", operationType = "DELETE", resourceType = "AUDIT_LOG", resourceIdIndex = 0)
    public ResponseEntity<Void> deleteAuditLog(@PathVariable Long id) {
        Optional<AuditLog> auditLog = auditLogService.getAuditLogById(id);
        
        if (auditLog.isPresent()) {
            auditLogService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取审计日志统计数据
     */
    @GetMapping("/stats")
    @AuditOperation(operation = "查询审计日志统计", operationType = "QUERY", resourceType = "AUDIT_LOG_STATS")
    public ResponseEntity<Map<String, Object>> getAuditLogStats() {
        // 这里实现统计逻辑，例如不同操作类型的数量统计等
        // 这里只是一个简单的示例
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogs", auditLogService.count());
        // 可以添加更多的统计数据
        
        return ResponseEntity.ok(stats);
    }

    /**
     * 导出审计日志为Excel
     */
    @GetMapping("/export")
    @AuditOperation(operation = "导出审计日志", operationType = "EXPORT", resourceType = "AUDIT_LOG")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        try {
            // 创建导出文件名
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
            String filename = "audit-logs_" + LocalDateTime.now().format(formatter) + ".xlsx";
            
            // 获取要导出的数据
            // 这里为简单起见，最多导出1000条记录，实际可能需要分页导出
            Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            Page<AuditLog> auditLogPage = auditLogService.searchAuditLogs(
                username,
                operationType,
                resourceType,
                startTime,
                endTime,
                pageable
            );
            
            List<AuditLog> auditLogs = auditLogPage.getContent();
            
            // 生成Excel文件的二进制数据
            byte[] excelContent = generateExcelContent(auditLogs);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 生成Excel内容
     * 注意：实际实现需要引入POI等Excel处理库
     */
    private byte[] generateExcelContent(List<AuditLog> auditLogs) throws Exception {
        // 这里需要使用Apache POI等库来生成Excel
        // 下面是一个简化的示例，实际实现会更复杂
        
        // 需要添加Apache POI依赖
        // 在此仅提供代码框架
        /*
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("审计日志");
        
        // 创建表头
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("用户名");
        headerRow.createCell(2).setCellValue("操作");
        headerRow.createCell(3).setCellValue("操作类型");
        headerRow.createCell(4).setCellValue("资源类型");
        headerRow.createCell(5).setCellValue("资源ID");
        headerRow.createCell(6).setCellValue("客户端IP");
        headerRow.createCell(7).setCellValue("时间");
        headerRow.createCell(8).setCellValue("详情");
        
        // 创建数据行
        int rowNum = 1;
        for (AuditLog log : auditLogs) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(log.getId());
            row.createCell(1).setCellValue(log.getUsername());
            row.createCell(2).setCellValue(log.getOperation());
            row.createCell(3).setCellValue(log.getOperationType());
            row.createCell(4).setCellValue(log.getResourceType());
            row.createCell(5).setCellValue(log.getResourceId() != null ? log.getResourceId() : "");
            row.createCell(6).setCellValue(log.getClientIp());
            row.createCell(7).setCellValue(log.getCreatedAt().toString());
            row.createCell(8).setCellValue(log.getDetails());
        }
        
        // 将工作簿写入字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        
        return baos.toByteArray();
        */
        
        // 临时实现，返回CSV格式数据
        StringBuilder csv = new StringBuilder();
        csv.append("ID,用户名,操作,操作类型,资源类型,资源ID,客户端IP,时间,详情\n");
        
        for (AuditLog log : auditLogs) {
            csv.append(log.getId()).append(",");
            csv.append(escapeCSV(log.getUsername())).append(",");
            csv.append(escapeCSV(log.getOperation())).append(",");
            csv.append(escapeCSV(log.getOperationType())).append(",");
            csv.append(escapeCSV(log.getResourceType())).append(",");
            csv.append(escapeCSV(log.getResourceId())).append(",");
            csv.append(escapeCSV(log.getClientIp())).append(",");
            csv.append(log.getCreatedAt().toString()).append(",");
            csv.append(escapeCSV(log.getDetails())).append("\n");
        }
        
        return csv.toString().getBytes("UTF-8");
    }
    
    /**
     * 转义CSV字段
     */
    private String escapeCSV(String field) {
        if (field == null) {
            return "";
        }
        
        // 如果字段包含逗号、引号或换行符，用引号包围并将引号转义
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        
        return field;
    }
} 