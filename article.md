# JobRunr 9: Background Jobs in Java, Without the Pain

Some work should not run while a user waits. Sending emails, building reports, calling a slow partner API, cleaning up old data. You want to hand that work off, and be sure it still gets done if the server restarts.

That is what JobRunr does. It is an open-source Java library for background jobs. Version 9 came out on 30 September, and 9.0.2 is the latest release I found.

## What it looks like

You write a normal method call inside a lambda. JobRunr saves it and runs it later:

```java
jobScheduler.enqueue(() -> emailService.sendWelcome(userId));                 // now, in background
jobScheduler.schedule(Instant.now().plusSeconds(3600), () -> ...);            // later
```

Recurring jobs are one annotation: `@Recurring(id = "nightly-cleanup", cron = "0 2 * * *")`.

## Why use it

- **Jobs are saved in your database.** Not in memory. If the app restarts, the jobs are still there. You do not need a message broker.
- **Retries are built in.** A failed job is retried with growing delays. After the last retry it stays visible as failed, and you can requeue it from the dashboard.
- **A dashboard comes with it.** You can see queued, running and failed jobs, with logs and progress bars.
- **It works with Spring Boot 4.** One starter dependency and a few properties.

## What is new in 9

- A job history view in the dashboard, shown as a Gantt chart.
- Micronaut 5 and Quarkus 3.40 LTS support.
- A warning when a job argument is too large.
- It works with a v8 database, so a v8 server and a v9 server can run side by side.
- It is the last open-source major version that supports Java 8.

Most apps only need to change the version number. Some old APIs were removed, so read the official migration guide first.

## Is it scalable?

Yes, by adding more instances. Every instance points at the same database and joins the same cluster. One is chosen as master for housekeeping, and the others share the work. If a server dies in the middle of a job, another one picks the job up.

I tried this with a small demo: two instances, one shared database.

- Work was split between both instances.
- A job that failed twice was retried and then succeeded.
- I killed one instance while it was running jobs. Within about a minute they went back to the queue and finished on the other one.
- After a full restart, old jobs were still there.

On speed, JobRunr's own benchmark shows its Pro edition at about 2,700 jobs per second on PostgreSQL, against about 145 for Quartz. That number comes from the vendor, and it is for tiny jobs on one setup. I found no public numbers for a large cluster. The shared database is the part that will limit you first, so test with your own load.

## When to use it

Use it when:
- work is slow, can fail, or must survive a restart,
- you run more than one instance of your app,
- you want retries and a dashboard without running a new system.

Skip it when:
- you only need a simple timer in one small app (Spring's `@Scheduled` is enough),
- you need heavy multi-step batch processing with restart per step (look at Spring Batch),
- you need very high-volume streaming (use a message broker).

## Two things to remember

1. A job can run twice after a crash. Write jobs so a repeat does no harm.
2. All instances must run the same code version, or jobs can fail with a missing-method error.

## Sources

- [JobRunr documentation](https://jobrunr.io/en/documentation/)
- [JobRunr v9 release notes and migration guide](https://www.jobrunr.io/en/guides/migration/v9/)
- [JobRunr v9 release blog](https://www.jobrunr.io/en/blog/v9-release/)
- [JobRunr vs Quartz benchmark](https://www.jobrunr.io/en/blog/quartz-vs-jobrunr/)
