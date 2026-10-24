package demo.jobrunr.controller;

import java.util.UUID;

import demo.jobrunr.dto.JobResponse;
import demo.jobrunr.dto.JobStatusResponse;
import demo.jobrunr.service.DemoJobService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.jobrunr.storage.JobNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class DemoController {

    private final DemoJobService demoJobService;

    public DemoController(final DemoJobService demoJobService) {
        this.demoJobService = demoJobService;
    }

    @PostMapping("/email")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public JobResponse enqueueEmail(@RequestParam @NotBlank @Size(max = 100) final String recipient) {
        return demoJobService.enqueueWelcomeEmail(recipient);
    }

    @PostMapping("/email/delayed")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public JobResponse scheduleEmail(
            @RequestParam @NotBlank @Size(max = 100) final String recipient,
            @RequestParam(defaultValue = "30") @Min(1) @Max(3600) final int delaySeconds) {
        return demoJobService.scheduleWelcomeEmail(recipient, delaySeconds);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public JobResponse enqueueBatch(@RequestParam(defaultValue = "20") @Min(1) @Max(200) final int items) {
        return demoJobService.enqueueBatch(items);
    }

    @PostMapping("/flaky")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public JobResponse enqueueFlakyCall() {
        return demoJobService.enqueueFlakyCall();
    }

    @PostMapping("/failing")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public JobResponse enqueueFailingJob() {
        return demoJobService.enqueueFailingJob();
    }

    @GetMapping("/jobs/{jobId}")
    public JobStatusResponse getStatus(@PathVariable final UUID jobId) {
        return demoJobService.getStatus(jobId);
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ProblemDetail handleJobNotFound(final JobNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Job not found");
    }
}
