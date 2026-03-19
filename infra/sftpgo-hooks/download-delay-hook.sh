#!/bin/sh

set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
worker="${script_dir}/delayed-delete-worker.sh"
hooks_root="${SFTPGO_HOOKS_ROOT:-/srv/sftpgo}"
action="${SFTPGO_ACTION:-}"
status="${SFTPGO_ACTION_STATUS:-0}"
path="${SFTPGO_ACTION_PATH:-}"

if [ "${status}" != "1" ]; then
  exit 0
fi

should_run=0

case "${action}:${path}" in
  download:"${hooks_root}"/exchange/a2b/outbox/*|download:"${hooks_root}"/exchange/b2a/outbox/*)
    should_run=1
    ;;
  upload:"${hooks_root}"/exchange/a2b/download-ack/*.download.ack|upload:"${hooks_root}"/exchange/b2a/download-ack/*.download.ack)
    should_run=1
    ;;
esac

if [ "${should_run}" != "1" ]; then
    exit 0
fi

(
  SFTPGO_HOOKS_ROOT="${hooks_root}" exec "${worker}" "${path}" "120"
) >/dev/null 2>&1 &

exit 0
