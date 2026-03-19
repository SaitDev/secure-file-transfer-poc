## Overview

This document describes the technical design for the Secure File Transfer POC between System A and System B.

The goal of the POC is to validate a secure file exchange process for sensitive CSV data using an SFTP server, acknowledgment files, and automated cleanup after successful download confirmation.

## Scope

This document covers the technical design for secure file exchange between System A and System B through the SFTP server.

In scope:

- outbound file upload from System A
- inbound file download by System B
- download acknowledgment handling
- result file upload from System B back to System A
- automated file cleanup after acknowledgment
- high-level application, storage, and infrastructure design

Out of scope:

- detailed business processing rules inside the CSV data
- production-grade high availability and disaster recovery

## High-Level Architecture

The solution consists of main components:

- System A service, which generates, encrypts, and uploads outbound CSV files
- SFTP server, which acts as the secure exchange point between both systems
- System B service, which downloads files, stores them durably, processes them, and uploads result files
- PostgreSQL DB, which stores transfer state, audit data, staged file data and extracted sensitive perfonal infos for the POC

[![](https://mermaid.ink/img/pako:eNp9klFPwjAUhf9Kc58gAQIdo1vfGAZfMKLyJOOhbJexyNql64xI-O92G4IStE-995zvtDftASIVI3AQOtqmBiNTauyu0YhQErsSrcqc5CpqRTtVxu3lC0bWQabpDslCC1lsUJP542QVyoYoUL-nEZJiXxjMxq2qRm3BuibjFUllFXjl3pg8URfzdDG_V39Y66DgOji47c5VYRKNxaAVCyPWosD2ct70Xp5mg_8hehOiF-iENaPyZ9LtkhlvZjlJ9f6sNFf_TQWVtuDni_5Ug2uVQgcSncbAjS6xAxnqTFQlHCouBLPFDEPgdhsL_RZCKI-WyYV8VSr7xuyrJlvgG7ErbFXmdky8S0WiRXbuapQx6okqpQHu-XUG8AN8AKee0xu5Q5fRvud7Q8Y6sAc-YE5vSH3GXDbyHeq67NiBz_rUfm9Eqec6FvEpY33HxmGcGqUfmu9X_8LjF_qP1aA?type=png)](https://mermaid.ai/live/edit#pako:eNp9klFPwjAUhf9Kc58gAQIdo1vfGAZfMKLyJOOhbJexyNql64xI-O92G4IStE-995zvtDftASIVI3AQOtqmBiNTauyu0YhQErsSrcqc5CpqRTtVxu3lC0bWQabpDslCC1lsUJP542QVyoYoUL-nEZJiXxjMxq2qRm3BuibjFUllFXjl3pg8URfzdDG_V39Y66DgOji47c5VYRKNxaAVCyPWosD2ct70Xp5mg_8hehOiF-iENaPyZ9LtkhlvZjlJ9f6sNFf_TQWVtuDni_5Ug2uVQgcSncbAjS6xAxnqTFQlHCouBLPFDEPgdhsL_RZCKI-WyYV8VSr7xuyrJlvgG7ErbFXmdky8S0WiRXbuapQx6okqpQHu-XUG8AN8AKee0xu5Q5fRvud7Q8Y6sAc-YE5vSH3GXDbyHeq67NiBz_rUfm9Eqec6FvEpY33HxmGcGqUfmu9X_8LjF_qP1aA).

## Detailed Process Flow

The detailed process flow starts when the scheduler in System A triggers a batch job. System A then generates and encrypts a CSV file, uploads it to SFTPGo, and persists the transfer details in the database for tracking and audit purposes.

System B polls SFTPGo, downloads the encrypted file, stores it durably, and uploads a download acknowledgment file. Once the download is completed, the SFTPGo hook starts a delayed cleanup check. If the matching acknowledgment arrives later, the ACK upload also starts the same delayed cleanup check. After waiting 2 minutes from either trigger, SFTPGo deletes the transferred file only when both the original file and the matching acknowledgment file are present.

System B then decrypts and processes the file, generates a result file, and uploads the result file back to SFTPGo.

System A polls SFTPGo for the result file, downloads it, stores it locally, and uploads a response download acknowledgment. The same delayed cleanup approach applies to the response flow: the download event starts a delayed cleanup check, and a late ACK upload starts another delayed cleanup check for the same file pair.

[![](https://mermaid.ink/img/pako:eNqtVE1z2jAQ_SsanUmmkGCMD5kx0ObQj0nrpp3pcBHWYlRkyZXkNITJf-_awrENNNNDffHHvn37dt9ae5pqDjSiFn6VoFJYCJYZli8VwatgxolUFEw5EhNmSbKzDnISn4aTOvzu692tPg3OOrmz0_Cijt9p6zIDyecPS-Ux8cXNTRyRJN0ALyUY4ozIMjCWrJhLN-SnXvWAt6DAMAdknnzrBd6q1OwK1_-eROS-kJpxAj4MvAKQtZDQohYzFOC0ASzOlF2jCg6OCWkJU5yUnsE6LNvIlloX2I2UZK0NEWqlS0S2tNU18wLmG0i3LURLDsaDQPGGL7lA8Oyli45Mwh5QCVs1zAfWhf6tXm0s8bjEoQnYjmQ7hKQSmCoLstF629K1_bdkvjQKfhnJof-OhsNoeSMlnr9v-vmk0SL9gGmI-86EIyOSC1U6sD15fjh5ZbVQ2RFVB7cACchodWlSaMUhjIh1fYNHYZ1t6s_8OBfgl6LCFkanYG1nRE3v9wWvNuoAqHQYsKV055r1kQPJuV3oAZpViLvdIqLQysKru4Ab_aVlOl6C-GgJTor-o_v97e-wdN2Ojwfg1f9H2__Ceep_V-FZ_-mAZkZwGjlTwoDmYHJWvdJ9RbekbgM5LGmEj5yZ7ZIu1TPm4BH1Q-u8STO6zDY0WjNp8a2s1-NwaL58NegYmDn-1I5GwaTmoNGePtJoOJ1chsFkEk5GYRiMx-MB3dHo-jIIhsFVeB2OxuNwGlw9D-hTXfTN5XQ4nGLWZBSMrqfBFBOAC_Tkoz-46_P7-Q-_Odfn?type=png)](https://mermaid.ai/live/edit#pako:eNqtVE1z2jAQ_SsanUmmkGCMD5kx0ObQj0nrpp3pcBHWYlRkyZXkNITJf-_awrENNNNDffHHvn37dt9ae5pqDjSiFn6VoFJYCJYZli8VwatgxolUFEw5EhNmSbKzDnISn4aTOvzu692tPg3OOrmz0_Cijt9p6zIDyecPS-Ux8cXNTRyRJN0ALyUY4ozIMjCWrJhLN-SnXvWAt6DAMAdknnzrBd6q1OwK1_-eROS-kJpxAj4MvAKQtZDQohYzFOC0ASzOlF2jCg6OCWkJU5yUnsE6LNvIlloX2I2UZK0NEWqlS0S2tNU18wLmG0i3LURLDsaDQPGGL7lA8Oyli45Mwh5QCVs1zAfWhf6tXm0s8bjEoQnYjmQ7hKQSmCoLstF629K1_bdkvjQKfhnJof-OhsNoeSMlnr9v-vmk0SL9gGmI-86EIyOSC1U6sD15fjh5ZbVQ2RFVB7cACchodWlSaMUhjIh1fYNHYZ1t6s_8OBfgl6LCFkanYG1nRE3v9wWvNuoAqHQYsKV055r1kQPJuV3oAZpViLvdIqLQysKru4Ab_aVlOl6C-GgJTor-o_v97e-wdN2Ojwfg1f9H2__Ceep_V-FZ_-mAZkZwGjlTwoDmYHJWvdJ9RbekbgM5LGmEj5yZ7ZIu1TPm4BH1Q-u8STO6zDY0WjNp8a2s1-NwaL58NegYmDn-1I5GwaTmoNGePtJoOJ1chsFkEk5GYRiMx-MB3dHo-jIIhsFVeB2OxuNwGlw9D-hTXfTN5XQ4nGLWZBSMrqfBFBOAC_Tkoz-46_P7-Q-_Odfn)

## Security Design

This POC handles sensitive personal information, so the file transfer design must keep data protected during transfer and processing.

### Transport

Files are exchanged through `SFTP` over SSH using `SFTPGo`. System A and System B use separate accounts with only the required folder permissions.

### Encryption

Outbound files from System A must be encrypted before upload. For this POC, encryption uses an OpenPGP-compatible approach. Response file is not encrypted.

### Data Handling

Only encrypted business files should be stored on `SFTPGo`. System B may store the encrypted inbound file for staging, but decrypted plaintext should only exist for the minimum time required for processing.

### Secrets

Secrets such as SFTP credentials, SSH keys, encryption passphrases, and database passwords must not be hardcoded in source code or committed to the repository.

## Edge case and Exception Handling

### Upload Failure

If System A cannot upload the encrypted file to SFTPGo, the transfer should be marked as failed for investigation or manual re-run. Automatic upload retry is not included in the current POC implementation.

### Download or Storage Failure

If System B downloads a file but cannot store it durably, it must not upload the download acknowledgment file. This prevents SFTPGo from deleting the source file and allows the file to be retried later.

### Missing ACK File

If the matching download acknowledgment file is not available when the SFTPGo hook checks after 2 minutes, no file is deleted. The source file remains on the server for later handling.

### Late ACK File

If System B uploads the download acknowledgment file after the original 2-minute download-triggered check has already run, the ACK upload must start another delayed cleanup check. After waiting 2 minutes from the ACK upload event, SFTPGo checks again for the matching source file and deletes both files if they are still present.

This allows the design to handle temporary delays such as database slowness, network delay, transient SFTP reconnect issues, or short operational backlog without leaving the file permanently orphaned on the server.

### Processing Failure

If System B cannot decrypt or process the file successfully, it retries processing based on the current POC configuration. The current implementation allows up to `10` total processing attempts with a `5` minute delay between retries. If processing still fails after the final attempt, the transfer is marked as failed and a failure result file can be returned to System A.

### Duplicate File

If the same file is detected more than once, the system should use the file identifier to avoid duplicate processing.

## Risks and Limitations

### POC Scope

This design is intended for a POC. It does not cover production-grade high availability, disaster recovery, or full operational support processes.

### Secret Management

The current POC uses local or application-level configuration for SFTP credentials and encryption or decryption secrets. This can be improved in a production-ready design by storing these secrets in a centralized secret management solution such as AWS Secrets Manager.

### Mock Data Only

The current POC uses mock CSV data only. It does not represent a final business schema, real customer information, or a finalized partner data contract.

### Scalability and High Availability

The current design uses SFTPGo with custom hooks, which is suitable for the POC and keeps the solution simple. If higher availability, higher bandwidth, more concurrent connections, or lower ongoing DevOps effort are required, the file exchange design can be replaced by a more managed AWS-based approach using Amazon S3, AWS Transfer Family, Amazon EventBridge, and AWS Lambda for storage events and automatic cleanup.

