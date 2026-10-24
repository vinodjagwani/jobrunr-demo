package demo.jobrunr.dto;

import java.util.UUID;

public record JobResponse(UUID jobId, String submittedBy) {
}
