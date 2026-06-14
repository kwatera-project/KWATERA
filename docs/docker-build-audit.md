# Docker build and startup audit

## Current bottlenecks found

- All Java service Dockerfiles copy the full repository before Maven runs. Any source, frontend, docs, or local-file change can invalidate the Maven dependency/build layers for every Java service.
- Java service Dockerfiles run `mvn install -N` and then `mvn clean package`. In a fresh Docker build layer, `clean` removes generated output that does not need to exist yet.
- Maven dependencies are downloaded independently in each Java service image build unless the builder cache is reused.
- The frontend Dockerfile already copies `package.json` and `bun.lock` before source files, but it does not use a BuildKit cache mount for Bun's package cache.
- The OCR Dockerfile already copies `requirements.txt` before application/model files, but pip caching is disabled entirely, so repeated local builds have to redownload packages.
- The Docker build context is the repository root for every service. The existing `.dockerignore` excludes major generated directories, but docs, local storage, git metadata helpers, and other non-build files can still enter the context.
- Compose uses `latest` tags for infrastructure images. Pinning those could improve reproducibility, but changing database, Kafka, or Mailpit versions without a current compatibility pass would be a behavior risk.

## Proposed safe changes

- Copy the parent `pom.xml` and module `pom.xml` files before copying full sources in Java Dockerfiles.
- Add BuildKit Maven cache mounts for `/root/.m2` during dependency warmup and package steps.
- Replace `mvn clean package` with `mvn package` in Docker builds while keeping `-DskipTests`.
- Add BuildKit cache mounts for Bun and pip package caches without copying cache contents into runtime images.
- Expand `.dockerignore` for non-build files and directories that commonly change.
- Keep the existing full-stack command and ports unchanged, and document a separate build-only warmup command for faster iteration.

## Rejected risky changes

- Do not remove any Compose services, including Kafka, Mailpit, Eureka, Config Server, or database migrations.
- Do not replace the Spring Cloud startup model or Compose dependency graph.
- Do not change published ports or user-facing URLs.
- Do not pin `latest` infrastructure image tags in this pass without a compatibility check against the currently used local images.
- Do not split the project into many separate build contexts; the Maven parent/module layout currently relies on repository-root context.

## Manual verification

Run these commands from the repository root:

```bash
docker compose -f infra/compose/docker-compose.yml config
docker compose -f infra/compose/docker-compose.yml build
docker compose -f infra/compose/docker-compose.yml up
```

For cache verification, run the build command twice. The second build should reuse dependency/cache layers unless relevant `pom.xml`, `package.json`, `bun.lock`, or `requirements.txt` files changed.
