package org.shiguang.module.scheduler;

import org.shiguang.entity.ScheduledTask;
import org.shiguang.module.audit.AuditOperation;
import org.shiguang.repository.ScheduledTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

/**
 * 定时任务服务
 */
@Service
public class ScheduledTaskService {

    @Autowired
    private ScheduledTaskRepository taskRepository;
    
    @Autowired
    private TaskScheduler taskScheduler;
    
    // 存储正在运行的任务
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new HashMap<>();
    
    /**
     * 系统启动时，初始化所有启用的定时任务
     */
    @PostConstruct
    public void initScheduledTasks() {
        List<ScheduledTask> enabledTasks = taskRepository.findByEnabledTrue();
        for (ScheduledTask task : enabledTasks) {
            scheduleTask(task);
        }
    }
    
    /**
     * 获取所有定时任务（分页）
     */
    public Page<ScheduledTask> getAllTasks(Pageable pageable) {
        return taskRepository.findAll(pageable);
    }
    
    /**
     * 根据ID获取定时任务
     */
    public Optional<ScheduledTask> getTaskById(Long id) {
        return taskRepository.findById(id);
    }
    
    /**
     * 搜索定时任务
     */
    public Page<ScheduledTask> searchTasks(String name, String description, Boolean enabled, 
                                        String jobClass, Pageable pageable) {
        return taskRepository.searchTasks(name, description, enabled, jobClass, pageable);
    }
    
    /**
     * 创建定时任务
     */
    @Transactional
    @AuditOperation(operation = "创建定时任务", operationType = "CREATE", resourceType = "SCHEDULED_TASK")
    public ScheduledTask createTask(ScheduledTask task, String username) {
        task.setCreatedBy(username);
        ScheduledTask savedTask = taskRepository.save(task);
        
        if (task.isEnabled()) {
            scheduleTask(savedTask);
        }
        
        return savedTask;
    }
    
    /**
     * 更新定时任务
     */
    @Transactional
    @AuditOperation(operation = "更新定时任务", operationType = "UPDATE", resourceType = "SCHEDULED_TASK", resourceIdIndex = 0)
    public ScheduledTask updateTask(Long id, ScheduledTask taskDetails, String username) {
        Optional<ScheduledTask> optionalTask = taskRepository.findById(id);
        
        if (optionalTask.isPresent()) {
            ScheduledTask existingTask = optionalTask.get();
            boolean wasEnabled = existingTask.isEnabled();
            
            // 更新任务属性
            existingTask.setName(taskDetails.getName());
            existingTask.setDescription(taskDetails.getDescription());
            existingTask.setCronExpression(taskDetails.getCronExpression());
            existingTask.setJobClass(taskDetails.getJobClass());
            existingTask.setJobData(taskDetails.getJobData());
            existingTask.setEnabled(taskDetails.isEnabled());
            existingTask.setUpdatedBy(username);
            
            ScheduledTask updatedTask = taskRepository.save(existingTask);
            
            // 处理任务调度
            if (scheduledTasks.containsKey(id)) {
                // 取消现有任务
                scheduledTasks.get(id).cancel(false);
                scheduledTasks.remove(id);
            }
            
            if (updatedTask.isEnabled()) {
                // 如果启用，重新调度
                scheduleTask(updatedTask);
            }
            
            return updatedTask;
        } else {
            throw new IllegalArgumentException("Task not found with id: " + id);
        }
    }
    
    /**
     * 启用或禁用定时任务
     */
    @Transactional
    @AuditOperation(operation = "修改定时任务状态", operationType = "UPDATE", resourceType = "SCHEDULED_TASK", resourceIdIndex = 0)
    public ScheduledTask toggleTaskStatus(Long id, boolean enabled, String username) {
        Optional<ScheduledTask> optionalTask = taskRepository.findById(id);
        
        if (optionalTask.isPresent()) {
            ScheduledTask task = optionalTask.get();
            
            if (task.isEnabled() != enabled) {
                task.setEnabled(enabled);
                task.setUpdatedBy(username);
                
                if (enabled) {
                    // 启用任务
                    scheduleTask(task);
                } else {
                    // 禁用任务
                    if (scheduledTasks.containsKey(id)) {
                        scheduledTasks.get(id).cancel(false);
                        scheduledTasks.remove(id);
                    }
                }
            }
            
            return taskRepository.save(task);
        } else {
            throw new IllegalArgumentException("Task not found with id: " + id);
        }
    }
    
    /**
     * 删除定时任务
     */
    @Transactional
    @AuditOperation(operation = "删除定时任务", operationType = "DELETE", resourceType = "SCHEDULED_TASK", resourceIdIndex = 0)
    public void deleteTask(Long id) {
        // 取消正在运行的任务
        if (scheduledTasks.containsKey(id)) {
            scheduledTasks.get(id).cancel(false);
            scheduledTasks.remove(id);
        }
        
        // 从数据库删除任务
        taskRepository.deleteById(id);
    }
    
    /**
     * 立即执行一次任务
     */
    @AuditOperation(operation = "立即执行定时任务", operationType = "EXECUTE", resourceType = "SCHEDULED_TASK", resourceIdIndex = 0)
    public void executeTaskNow(Long id) {
        Optional<ScheduledTask> optionalTask = taskRepository.findById(id);
        
        if (optionalTask.isPresent()) {
            ScheduledTask task = optionalTask.get();
            
            try {
                // 创建并执行任务
                Runnable jobInstance = createJobInstance(task);
                if (jobInstance != null) {
                    Thread jobThread = new Thread(jobInstance);
                    jobThread.start();
                    
                    // 更新上次执行时间和状态
                    task.setLastExecutedAt(LocalDateTime.now());
                    task.setLastExecutionStatus("EXECUTED");
                    taskRepository.save(task);
                }
            } catch (Exception e) {
                // 更新执行状态为失败
                task.setLastExecutedAt(LocalDateTime.now());
                task.setLastExecutionStatus("ERROR: " + e.getMessage());
                taskRepository.save(task);
                
                throw new RuntimeException("Error executing task: " + e.getMessage(), e);
            }
        } else {
            throw new IllegalArgumentException("Task not found with id: " + id);
        }
    }
    
    /**
     * 调度定时任务
     */
    private void scheduleTask(ScheduledTask task) {
        try {
            Runnable jobInstance = createJobInstance(task);
            if (jobInstance != null) {
                // 使用cron表达式调度任务
                ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(
                    new TaskWrapper(task, jobInstance, taskRepository),
                    new CronTrigger(task.getCronExpression())
                );
                
                // 存储调度的任务
                scheduledTasks.put(task.getId(), scheduledFuture);
            }
        } catch (Exception e) {
            // 记录错误并更新任务状态
            task.setLastExecutionStatus("SCHEDULE_ERROR: " + e.getMessage());
            taskRepository.save(task);
            
            throw new RuntimeException("Error scheduling task: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建任务实例
     */
    private Runnable createJobInstance(ScheduledTask task) throws Exception {
        try {
            Class<?> jobClass = Class.forName(task.getJobClass());
            Object jobObject = jobClass.getDeclaredConstructor().newInstance();
            
            if (jobObject instanceof Runnable) {
                return (Runnable) jobObject;
            } else {
                throw new IllegalArgumentException("Job class must implement Runnable interface: " + task.getJobClass());
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Job class not found: " + task.getJobClass(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error creating job instance: " + e.getMessage(), e);
        }
    }
    
    /**
     * 任务包装器，用于在执行任务前后更新任务状态
     */
    private static class TaskWrapper implements Runnable {
        private final ScheduledTask task;
        private final Runnable jobInstance;
        private final ScheduledTaskRepository repository;
        
        public TaskWrapper(ScheduledTask task, Runnable jobInstance, ScheduledTaskRepository repository) {
            this.task = task;
            this.jobInstance = jobInstance;
            this.repository = repository;
        }
        
        @Override
        public void run() {
            try {
                // 更新执行开始时间
                task.setLastExecutedAt(LocalDateTime.now());
                task.setLastExecutionStatus("RUNNING");
                repository.save(task);
                
                // 执行实际任务
                jobInstance.run();
                
                // 更新执行成功状态
                task.setLastExecutionStatus("SUCCESS");
                repository.save(task);
            } catch (Exception e) {
                // 更新执行失败状态
                task.setLastExecutionStatus("ERROR: " + e.getMessage());
                repository.save(task);
            }
        }
    }
} 