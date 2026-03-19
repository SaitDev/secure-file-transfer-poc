# Local Setup Guide

## Prerequisites

- Java `21`
- Docker and Docker Compose
- `curl`

## 1. Start Local Infrastructure

From [infra](/Users/Sait/Works/VMO/BID/secure-file-transfer-poc/infra):

```bash
./scripts/generate-local-users.sh
docker compose up -d
```

This starts:

- PostgreSQL on `localhost:5432`
- SFTPGo on `localhost:2022`
- SFTPGo Admin UI on `http://localhost:8888/web/admin`

The generator also creates SSH keypairs for `system-a` and `system-b` under `infra/generated/keys/`, and the applications default to those private key paths during local `bootRun`.

## 2. Run System A

From [system-a](/Users/Sait/Works/VMO/BID/secure-file-transfer-poc/system-a):

```bash
./gradlew bootRun
```

System A runs on `http://localhost:8080`.

## 3. Run System B

From [system-b](/Users/Sait/Works/VMO/BID/secure-file-transfer-poc/system-b):

```bash
./gradlew bootRun
```

System B runs on `http://localhost:8081`.

## 4. Trigger a Simple End-to-End Flow

Trigger outbound file generation and upload from System A:

```bash
curl -X POST http://localhost:8080/internal/system-a/jobs/outbound
```

Trigger inbound polling on System B:

```bash
curl -X POST http://localhost:8081/internal/system-b/jobs/inbound-polling
```

Trigger file processing on System B:

```bash
curl -X POST http://localhost:8081/internal/system-b/jobs/inbound-processing
```

Trigger response polling on System A:

```bash
curl -X POST http://localhost:8080/internal/system-a/jobs/response-polling
```

## 5. Notes

- Default local database and SFTP key settings are already configured in the application properties.
- The local setup uses generated local access details, SSH keys, and mock data only.
- SFTPGo cleanup is handled by the local hook after download completion and a 2-minute delay.
