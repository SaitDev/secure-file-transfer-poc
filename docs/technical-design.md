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

System B polls SFTPGo, downloads the encrypted file, stores it durably, and uploads a download acknowledgment file. Once the download is completed, the SFTPGo hook starts immediately. After waiting 2 minutes, SFTPGo checks whether the matching download acknowledgment file has been received and deletes the transferred file only when the acknowledgment is present.

System B then decrypts and processes the file, generates a result file, and uploads the result file back to SFTPGo.

System A polls SFTPGo for the result file, downloads it, stores it locally, and uploads a response download acknowledgment. Once the result file download is completed, the same SFTPGo hook starts immediately and performs the delayed cleanup check after 2 minutes.

[![](https://mermaid.ink/img/pako:eNqVVF1vmzAU_SuWn9No-SKEh0qQbH3Yh7qxbtLEi4NviFewmW26tlH--y5fAZKo0njC3HPPPcfH-EBjxYF61MCfAmQMG8ESzbJIEnxypq2IRc6kJT5hhoQvxkJG_MtyWJU_fL-_U5fFoNcbXJY3Vf1eGZtoCL9-imSN8W9ub32PhPEeeJGCJlaLJAFtyJbZeE9-q-0AeAcSNLNA1uGPQeG9jPVLboffQ4885KlinEBdBl4CyE6k0KE2AQqwSgMOZ9LsUAUHy0RqCJOcFDWDsTi2lZ0qlaObNCU7pYmQW1UgsqMtn6AWsN5D_NhBVMpB1yCQvOULbxAcnFz0ZBL2hErYtmVuWDfqr3zTWDAw1qFqTlRy8toY65E3e8bbGf76Yyv0i8K9V0_YhrifTFgyJZmQhQXTOqn1QQqINKrQMXRDkYmIHcnKbIVMqjU8C2NNOyCoN2IDdZxlU65VDMZcMfeQ8_IsNICSUIMpUnvNTV1pSK6lOAC0Ifr9EBGRK2ngzRTxLH7rmM7j88_iuxg6PJC9cj8n_9xZLes_A6MjmmjBqWd1ASOagc5YuaSHsjWidg8ZRNTDV870Y0QjecQe_J9_KZW1bVoVyZ56O5YaXBVVIs0Nc_qqcZNAr_EPsNRzZhUH9Q70mXqT1XLsOsulu5y6rrNYLEb0hXrzseNMnJk7d6eLhbtyZscRfa2GvhuvJpMVdi2nznS-clbYAFzgbn2ub7nqsjv-A3qMlDA?type=png)](https://mermaid.ai/live/edit#pako:eNqVVF1vmzAU_SuWn9No-SKEh0qQbH3Yh7qxbtLEi4NviFewmW26tlH--y5fAZKo0njC3HPPPcfH-EBjxYF61MCfAmQMG8ESzbJIEnxypq2IRc6kJT5hhoQvxkJG_MtyWJU_fL-_U5fFoNcbXJY3Vf1eGZtoCL9-imSN8W9ub32PhPEeeJGCJlaLJAFtyJbZeE9-q-0AeAcSNLNA1uGPQeG9jPVLboffQ4885KlinEBdBl4CyE6k0KE2AQqwSgMOZ9LsUAUHy0RqCJOcFDWDsTi2lZ0qlaObNCU7pYmQW1UgsqMtn6AWsN5D_NhBVMpB1yCQvOULbxAcnFz0ZBL2hErYtmVuWDfqr3zTWDAw1qFqTlRy8toY65E3e8bbGf76Yyv0i8K9V0_YhrifTFgyJZmQhQXTOqn1QQqINKrQMXRDkYmIHcnKbIVMqjU8C2NNOyCoN2IDdZxlU65VDMZcMfeQ8_IsNICSUIMpUnvNTV1pSK6lOAC0Ifr9EBGRK2ngzRTxLH7rmM7j88_iuxg6PJC9cj8n_9xZLes_A6MjmmjBqWd1ASOagc5YuaSHsjWidg8ZRNTDV870Y0QjecQe_J9_KZW1bVoVyZ56O5YaXBVVIs0Nc_qqcZNAr_EPsNRzZhUH9Q70mXqT1XLsOsulu5y6rrNYLEb0hXrzseNMnJk7d6eLhbtyZscRfa2GvhuvJpMVdi2nznS-clbYAFzgbn2ub7nqsjv-A3qMlDA)

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

## Error and Exception Handling

### Upload Failure

If System A cannot upload the encrypted file to SFTPGo, the transfer should be marked as failed for investigation or manual re-run. Automatic upload retry is not included in the current POC implementation.

### Download or Storage Failure

If System B downloads a file but cannot store it durably, it must not upload the download acknowledgment file. This prevents SFTPGo from deleting the source file and allows the file to be retried later.

### Missing ACK File

If the matching download acknowledgment file is not available when the SFTPGo hook checks after 2 minutes, no file is deleted. The source file remains on the server for later handling.

### Processing Failure

If System B cannot decrypt or process the file successfully, the processing result should be recorded and a failure result file can be returned to System A if required by the agreed flow.

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

