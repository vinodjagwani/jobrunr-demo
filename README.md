# JobRunr demo

Spring Boot 4.1.2, Java 25, JobRunr 9.0.2 (`jobrunr-spring-boot-4-starter`), H2 file database.

Jobs are stored in the database (`./data/jobrunr-demo`), not in memory. Several instances share that file
(`AUTO_SERVER=TRUE`) and form one JobRunr cluster.

## Run

```bash
./gradlew bootJar
java -jar build/libs/jobrunr-demo-0.0.1.jar --server.port=8080 --jobrunr.dashboard.port=8000
java -jar build/libs/jobrunr-demo-0.0.1.jar --server.port=8081 --jobrunr.dashboard.port=8001
```

Dashboard: http://localhost:8000/dashboard (no auth, local demo only).

## Endpoints

| Call | Shows |
|------|-------|
| `POST /demo/email?recipient=ada@x.com` | fire-and-forget |
| `POST /demo/email/delayed?recipient=ada@x.com&delaySeconds=30` | delayed job |
| `POST /demo/batch?items=20` | long job, dashboard progress bar and logs |
| `POST /demo/flaky` | fails twice, auto-retried, then succeeds |
| `POST /demo/failing` | exhausts retries, ends FAILED, requeue from dashboard |
| `GET /demo/jobs/{jobId}` | job state |

A recurring job (`cluster-heartbeat`, every 30s) is registered by `@Recurring`. It runs once per interval across
the whole cluster, not once per instance.

## Demo script

1. Start two instances. One logs "is master", the other "is master (another BackgroundJobServer)".
2. Submit four batches to port 8080. Both instances process them (see `[instance-8080]` / `[instance-8081]` in job logs).
3. Submit batches, then kill one instance (`Stop-Process -Force`). Its `PROCESSING` jobs go back to `ENQUEUED`
   after the server timeout and finish on the surviving instance.
4. Stop all instances, start one. Old jobs are still in the dashboard.

## Notes

- Jobs can run more than once after a crash, so job bodies should be idempotent.
- All instances must run the same code version.
- H2 `AUTO_SERVER` is for local demos only. If the instance that opened the file dies, the others may lose the database (not tested here; the crash test killed the second instance only).
  Use PostgreSQL or similar for anything real. Only `spring.datasource.*` changes.
- Tests: `./gradlew test`.
