package demo.jobrunr.service;

import java.time.Instant;
import java.util.UUID;

import demo.jobrunr.dto.JobResponse;
import demo.jobrunr.dto.JobStatusResponse;
import demo.jobrunr.job.DemoJobs;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Submits jobs to JobRunr. The lambdas are serialized into the database, not run here. */
@Service
public class DemoJobService {

    private static final Logger log = LoggerFactory.getLogger(DemoJobService.class);

    private final JobScheduler jobScheduler;
    private final StorageProvider storageProvider;
    private final DemoJobs demoJobs;
    private final String instanceName;

    public DemoJobService(
            final JobScheduler jobScheduler,
            final StorageProvider storageProvider,
            final DemoJobs demoJobs,
            @Value("${demo.instance-name}") final String instanceName) {
        this.jobScheduler = jobScheduler;
        this.storageProvider = storageProvider;
        this.demoJobs = demoJobs;
        this.instanceName = instanceName;
    }

    public JobResponse enqueueWelcomeEmail(final String recipient) {
        final JobId jobId = jobScheduler.enqueue(() -> demoJobs.sendWelcomeEmail(recipient, JobContext.Null));
        return toJobResponse(jobId, "welcome email");
    }

    public JobResponse scheduleWelcomeEmail(final String recipient, final int delaySeconds) {
        final Instant runAt = Instant.now().plusSeconds(delaySeconds);
        final JobId jobId = jobScheduler.schedule(runAt, () -> demoJobs.sendWelcomeEmail(recipient, JobContext.Null));
        return toJobResponse(jobId, "delayed welcome email");
    }

    public JobResponse enqueueBatch(final int items) {
        final JobId jobId = jobScheduler.enqueue(() -> demoJobs.processBatch(items, JobContext.Null));
        return toJobResponse(jobId, "batch");
    }

    public JobResponse enqueueFlakyCall() {
        final JobId jobId = jobScheduler.enqueue(() -> demoJobs.callFlakyService(JobContext.Null));
        return toJobResponse(jobId, "flaky call");
    }

    public JobResponse enqueueFailingJob() {
        final JobId jobId = jobScheduler.enqueue(() -> demoJobs.alwaysFail(JobContext.Null));
        return toJobResponse(jobId, "failing job");
    }

    /** @throws JobNotFoundException when no job has this id */
    public JobStatusResponse getStatus(final UUID jobId) {
        final Job job = storageProvider.getJobById(jobId);
        return toStatusResponse(job);
    }

    private JobResponse toJobResponse(final JobId jobId, final String kind) {
        log.info("[{}] Submitted {} as job {}", instanceName, kind, jobId);
        return new JobResponse(jobId.asUUID(), instanceName);
    }

    private JobStatusResponse toStatusResponse(final Job job) {
        return new JobStatusResponse(job.getId(), job.getJobName(), job.getState().name());
    }
}
