package demo.jobrunr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoJobsIntegrationTest {

    private static final Duration JOB_TIMEOUT = Duration.ofSeconds(45);

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Enqueued welcome email is persisted and processed by the background server")
    void testEnqueuedEmailSucceeds() throws Exception {
        final UUID jobId = submit("/demo/email?recipient=ada@example.com");

        awaitState(jobId, "SUCCEEDED");

        mockMvc.perform(get("/demo/jobs/{id}", jobId))
                .andExpect(jsonPath("$.name").value("Welcome email to ada@example.com"));
    }

    @Test
    @DisplayName("Flaky job fails twice, is retried automatically, then succeeds")
    void testFlakyJobSucceedsAfterRetries() throws Exception {
        final UUID jobId = submit("/demo/flaky");

        awaitState(jobId, "SUCCEEDED");
    }

    @Test
    @DisplayName("Delayed email stays SCHEDULED until its run time")
    void testDelayedEmailIsScheduled() throws Exception {
        final UUID jobId = submit("/demo/email/delayed?recipient=bob@example.com&delaySeconds=3600");

        mockMvc.perform(get("/demo/jobs/{id}", jobId))
                .andExpect(jsonPath("$.state").value("SCHEDULED"));
    }

    @Test
    @DisplayName("Out-of-range batch size is rejected at the API boundary")
    void testBatchSizeIsValidated() throws Exception {
        mockMvc.perform(post("/demo/batch?items=100000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unknown job id returns 404")
    void testUnknownJobIsNotFound() throws Exception {
        mockMvc.perform(get("/demo/jobs/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private UUID submit(final String url) throws Exception {
        final String body = mockMvc.perform(post(url))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.jobId"));
    }

    private void awaitState(final UUID jobId, final String expectedState) {
        await().atMost(JOB_TIMEOUT).untilAsserted(() -> assertThat(stateOf(jobId)).isEqualTo(expectedState));
    }

    private String stateOf(final UUID jobId) throws Exception {
        final String body = mockMvc.perform(get("/demo/jobs/{id}", jobId))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.state");
    }
}
