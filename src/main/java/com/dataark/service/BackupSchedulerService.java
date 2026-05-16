package com.dataark.service;

import com.dataark.model.BackupJob;
import com.dataark.model.JobStatus;
import com.dataark.repository.BackupJobRepository;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class BackupSchedulerService implements SchedulingConfigurer {
    private final BackupJobRepository backupJobRepository;
    private final BackupExecutionService backupExecutionService;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<Long, ScheduledFuture<?>>();

    public BackupSchedulerService(BackupJobRepository backupJobRepository,
                                  BackupExecutionService backupExecutionService) {
        this.backupJobRepository = backupJobRepository;
        this.backupExecutionService = backupExecutionService;
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(4);
        this.taskScheduler.setThreadNamePrefix("dataark-scheduler-");
        this.taskScheduler.initialize();
    }

    @PostConstruct
    public void loadEnabledJobs() {
        List<BackupJob> jobs = backupJobRepository.findByStatus(JobStatus.ENABLED);
        for (BackupJob job : jobs) {
            schedule(job);
        }
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler);
    }

    public synchronized void reschedule(BackupJob job) {
        cancel(job.getId());
        if (job.getStatus() == JobStatus.ENABLED) {
            schedule(job);
        }
    }

    public synchronized void cancel(Long jobId) {
        ScheduledFuture<?> future = scheduledTasks.remove(jobId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void schedule(final BackupJob job) {
        CronTrigger cronTrigger = new CronTrigger(job.getCronExpression());
        ScheduledFuture<?> future = taskScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                backupExecutionService.runAsync(job.getId());
            }
        }, new Trigger() {
            @Override
            public Date nextExecutionTime(TriggerContext triggerContext) {
                return cronTrigger.nextExecutionTime(triggerContext);
            }
        });
        scheduledTasks.put(job.getId(), future);

        Date next = cronTrigger.nextExecutionTime(new EmptyTriggerContext());
        if (next != null) {
            job.setNextRunAt(LocalDateTime.ofInstant(next.toInstant(), ZoneId.systemDefault()));
            backupJobRepository.save(job);
        }
    }

    private static class EmptyTriggerContext implements TriggerContext {
        @Override
        public Date lastScheduledExecutionTime() {
            return null;
        }

        @Override
        public Date lastActualExecutionTime() {
            return null;
        }

        @Override
        public Date lastCompletionTime() {
            return null;
        }
    }
}
