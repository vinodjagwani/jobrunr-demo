package demo.jobrunr.job;

import java.time.Duration;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobDashboardProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** The job bodies. JobRunr serializes the call, then runs it on whichever instance claims the job. */
@Service
public class DemoJobs {

    private static final Logger log = LoggerFactory.getLogger(DemoJobs.class);
    private static final int FLAKY_FAILURES = 2;

    private final String instanceName;

    public DemoJobs(@Value("${demo.instance-name}") final String instanceName) {
        this.instanceName = instanceName;
    }

    @Job(name = "Welcome email to %0")
    public void sendWelcomeEmail(final String recipient, final JobContext jobContext) {
        logOnInstance(jobContext, "Sending welcome email to " + recipient);
        simulateWork(Duration.ofSeconds(1));
        logOnInstance(jobContext, "Welcome email sent to " + recipient);
    }

    @Job(name = "Batch of %0 items")
    public void processBatch(final int items, final JobContext jobContext) {
        final JobDashboardProgressBar progressBar = jobContext.progressBar(items);
        processItems(items, progressBar, jobContext);
    }

    @Job(name = "Flaky external call")
    public void callFlakyService(final JobContext jobContext) {
        failWhileRetriesRemain(jobContext);
        logOnInstance(jobContext, "Flaky call succeeded on retry " + jobContext.currentRetry());
    }

    @Job(name = "Always failing job", retries = 1)
    public void alwaysFail(final JobContext jobContext) {
        logOnInstance(jobContext, "About to fail on purpose");
        throw new IllegalStateException("Boom: this job always fails");
    }

    @Recurring(id = "cluster-heartbeat", interval = "PT30S")
    @Job(name = "Cluster heartbeat")
    public void heartbeat() {
        log.info("Heartbeat ran on {}", instanceName);
    }

    private void processItems(final int items, final JobDashboardProgressBar progressBar, final JobContext jobContext) {
        for (int item = 1; item <= items; item++) {
            simulateWork(Duration.ofSeconds(1));
            progressBar.incrementSucceeded();
            logOnInstance(jobContext, "Processed item " + item + " of " + items);
        }
    }

    private void failWhileRetriesRemain(final JobContext jobContext) {
        if (jobContext.currentRetry() < FLAKY_FAILURES) {
            logOnInstance(jobContext, "Flaky call failing, retry " + jobContext.currentRetry());
            throw new IllegalStateException("Simulated outage, retry " + jobContext.currentRetry());
        }
    }

    private void logOnInstance(final JobContext jobContext, final String message) {
        log.info("[{}] {}", instanceName, message);
        jobContext.logger().info("[" + instanceName + "] " + message);
    }

    private void simulateWork(final Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Job interrupted, JobRunr will retry it", e);
        }
    }
}
