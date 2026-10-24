package demo.jobrunr.dto;

import java.util.UUID;

public record JobStatusResponse(UUID jobId, String name, String state) {
}
