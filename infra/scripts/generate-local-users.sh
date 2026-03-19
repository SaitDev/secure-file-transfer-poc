#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
infra_dir="$(cd "${script_dir}/.." && pwd)"
generated_dir="${infra_dir}/generated"
runtime_dir="${infra_dir}/runtime"
bootstrap_file="${generated_dir}/sftpgo-bootstrap.json"

admin_username="local-admin"
admin_password="LocalAdmin123!"
system_a_username="system-a"
system_a_password="SystemA123!"
system_b_username="system-b"
system_b_password="SystemB123!"
system_a_postgres_db="system_a_transfer"
system_a_postgres_user="system_a_user"
system_a_postgres_password="system_a_pass_local"
system_b_postgres_db="system_b_transfer"
system_b_postgres_user="system_b_user"
system_b_postgres_password="system_b_pass_local"
postgres_host="localhost"
postgres_port="5432"
postgres_admin_db="postgres"
postgres_admin_user="postgres"
postgres_admin_password="postgres_local"

mkdir -p "${generated_dir}"
mkdir -p "${runtime_dir}/sftpgo-data/exchange/a2b/outbox"
mkdir -p "${runtime_dir}/sftpgo-data/exchange/a2b/download-ack"
mkdir -p "${runtime_dir}/sftpgo-data/exchange/b2a/outbox"
mkdir -p "${runtime_dir}/sftpgo-data/exchange/b2a/download-ack"
mkdir -p "${runtime_dir}/sftpgo-data/hooks-log"
mkdir -p "${runtime_dir}/sftpgo-home"
mkdir -p "${runtime_dir}/sftpgo-home/bootstrap"
mkdir -p "${runtime_dir}/postgres-data"
mkdir -p "${runtime_dir}/postgres-init"

cat > "${runtime_dir}/postgres-init/01-init-app-databases.sql" <<EOF
CREATE USER ${system_a_postgres_user} WITH ENCRYPTED PASSWORD '${system_a_postgres_password}';
CREATE DATABASE ${system_a_postgres_db} OWNER ${system_a_postgres_user};
GRANT ALL PRIVILEGES ON DATABASE ${system_a_postgres_db} TO ${system_a_postgres_user};

CREATE USER ${system_b_postgres_user} WITH ENCRYPTED PASSWORD '${system_b_postgres_password}';
CREATE DATABASE ${system_b_postgres_db} OWNER ${system_b_postgres_user};
GRANT ALL PRIVILEGES ON DATABASE ${system_b_postgres_db} TO ${system_b_postgres_user};
EOF

cat > "${generated_dir}/local-users.env" <<EOF
SFTPGO_ADMIN_USERNAME=${admin_username}
SFTPGO_ADMIN_PASSWORD=${admin_password}
SYSTEM_A_SFTP_USERNAME=${system_a_username}
SYSTEM_A_SFTP_PASSWORD=${system_a_password}
SYSTEM_B_SFTP_USERNAME=${system_b_username}
SYSTEM_B_SFTP_PASSWORD=${system_b_password}
POSTGRES_ADMIN_HOST=${postgres_host}
POSTGRES_ADMIN_PORT=${postgres_port}
POSTGRES_ADMIN_DB=${postgres_admin_db}
POSTGRES_ADMIN_USER=${postgres_admin_user}
POSTGRES_ADMIN_PASSWORD=${postgres_admin_password}
SYSTEM_A_POSTGRES_HOST=${postgres_host}
SYSTEM_A_POSTGRES_PORT=${postgres_port}
SYSTEM_A_POSTGRES_DB=${system_a_postgres_db}
SYSTEM_A_POSTGRES_USER=${system_a_postgres_user}
SYSTEM_A_POSTGRES_PASSWORD=${system_a_postgres_password}
SYSTEM_B_POSTGRES_HOST=${postgres_host}
SYSTEM_B_POSTGRES_PORT=${postgres_port}
SYSTEM_B_POSTGRES_DB=${system_b_postgres_db}
SYSTEM_B_POSTGRES_USER=${system_b_postgres_user}
SYSTEM_B_POSTGRES_PASSWORD=${system_b_postgres_password}
EOF

chmod 600 "${generated_dir}/local-users.env"

cat > "${bootstrap_file}" <<EOF
{
  "admins": [
    {
      "status": 1,
      "username": "${admin_username}",
      "password": "${admin_password}",
      "permissions": ["*"]
    }
  ],
  "folders": [
    {
      "name": "system-a-send",
      "mapped_path": "/srv/sftpgo/exchange/a2b/outbox",
      "description": "System A outbound to System B",
      "filesystem": {
        "provider": 0
      }
    },
    {
      "name": "system-a-recv",
      "mapped_path": "/srv/sftpgo/exchange/b2a/outbox",
      "description": "System A inbound from System B",
      "filesystem": {
        "provider": 0
      }
    },
    {
      "name": "system-a-ack",
      "mapped_path": "/srv/sftpgo/exchange/b2a/download-ack",
      "description": "System A ACK files",
      "filesystem": {
        "provider": 0
      }
    },
    {
      "name": "system-b-send",
      "mapped_path": "/srv/sftpgo/exchange/b2a/outbox",
      "description": "System B outbound to System A",
      "filesystem": {
        "provider": 0
      }
    },
    {
      "name": "system-b-recv",
      "mapped_path": "/srv/sftpgo/exchange/a2b/outbox",
      "description": "System B inbound from System A",
      "filesystem": {
        "provider": 0
      }
    },
    {
      "name": "system-b-ack",
      "mapped_path": "/srv/sftpgo/exchange/a2b/download-ack",
      "description": "System B ACK files",
      "filesystem": {
        "provider": 0
      }
    }
  ],
  "users": [
    {
      "status": 1,
      "username": "${system_a_username}",
      "password": "${system_a_password}",
      "home_dir": "/srv/sftpgo/data/${system_a_username}",
      "permissions": {
        "/": ["list"],
        "/send": ["list", "upload", "rename"],
        "/recv": ["list", "download"],
        "/ack": ["list", "upload"]
      },
      "filesystem": {
        "provider": 0
      },
      "virtual_folders": [
        {
          "name": "system-a-send",
          "virtual_path": "/send",
          "quota_size": 0,
          "quota_files": 0
        },
        {
          "name": "system-a-recv",
          "virtual_path": "/recv",
          "quota_size": 0,
          "quota_files": 0
        },
        {
          "name": "system-a-ack",
          "virtual_path": "/ack",
          "quota_size": 0,
          "quota_files": 0
        }
      ]
    },
    {
      "status": 1,
      "username": "${system_b_username}",
      "password": "${system_b_password}",
      "home_dir": "/srv/sftpgo/data/${system_b_username}",
      "permissions": {
        "/": ["list"],
        "/send": ["list", "upload", "rename"],
        "/recv": ["list", "download"],
        "/ack": ["list", "upload"]
      },
      "filesystem": {
        "provider": 0
      },
      "virtual_folders": [
        {
          "name": "system-b-send",
          "virtual_path": "/send",
          "quota_size": 0,
          "quota_files": 0
        },
        {
          "name": "system-b-recv",
          "virtual_path": "/recv",
          "quota_size": 0,
          "quota_files": 0
        },
        {
          "name": "system-b-ack",
          "virtual_path": "/ack",
          "quota_size": 0,
          "quota_files": 0
        }
      ]
    }
  ]
}
EOF

cat > "${generated_dir}/local-users.txt" <<EOF
Local SFTPGo credentials
========================

Admin UI
--------
username: ${admin_username}
password: ${admin_password}

System A
--------
username: ${system_a_username}
password: ${system_a_password}

System B
--------
username: ${system_b_username}
password: ${system_b_password}

PostgreSQL - System A
---------------------
host: ${postgres_host}
port: ${postgres_port}
database: ${system_a_postgres_db}
username: ${system_a_postgres_user}
password: ${system_a_postgres_password}

PostgreSQL - System B
---------------------
host: ${postgres_host}
port: ${postgres_port}
database: ${system_b_postgres_db}
username: ${system_b_postgres_user}
password: ${system_b_postgres_password}

PostgreSQL - Admin
------------------
host: ${postgres_host}
port: ${postgres_port}
database: ${postgres_admin_db}
username: ${postgres_admin_user}
password: ${postgres_admin_password}
EOF

printf 'Generated %s\n' "${generated_dir}/local-users.env"
printf 'Generated %s\n' "${generated_dir}/local-users.txt"
printf 'Generated %s\n' "${bootstrap_file}"
printf 'Prepared runtime directories under %s\n' "${runtime_dir}"
