package com.huace.trace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步码生成服务
 * 大批量码包生成通过后台线程池执行，避免阻塞 HTTP 请求
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGenerationAsyncService {

    private final CodeGenerationService codeGenerationService;

    // 简单的内存任务状态跟踪（生产环境可替换为 Redis / 数据库任务表）
    private final Map<Long, GenerationTask> taskMap = new ConcurrentHashMap<>();

    @Async("codeGenerationExecutor")
    public void generateAsync(Long taskId, Map<String, Object> params) {
        taskMap.put(taskId, new GenerationTask(taskId, "RUNNING", null, null));
        long start = System.currentTimeMillis();
        try {
            codeGenerationService.generate(params);
            long cost = System.currentTimeMillis() - start;
            taskMap.put(taskId, new GenerationTask(taskId, "SUCCESS", null, cost));
            log.info("异步码生成任务完成: taskId={}, cost={}ms", taskId, cost);
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            taskMap.put(taskId, new GenerationTask(taskId, "FAILED", e.getMessage(), cost));
            log.error("异步码生成任务失败: taskId={}, cost={}ms", taskId, cost, e);
        }
    }

    public GenerationTask getTask(Long taskId) {
        return taskMap.get(taskId);
    }

    public record GenerationTask(Long taskId, String status, String errorMsg, Long costMs) {}
}
