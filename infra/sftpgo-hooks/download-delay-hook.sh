#!/bin/sh

set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
worker="${script_dir}/delayed-delete-worker.sh"
hooks_root="${SFTPGO_HOOKS_ROOT:-/srv/sftpgo}"
action="${SFTPGO_ACTION:-}"
status="${SFTPGO_ACTION_STATUS:-0}"
path="${SFTPGO_ACTION_PATH:-}"

if [ "${action}" != "download" ]; then
  exit 0
fi

if [ "${status}" != "1" ]; then
  exit 0
fi

case "${path}" in
  "${hooks_root}"/exchange/a2b/outbox/*|"${hooks_root}"/exchange/b2a/outbox/*)
    ;;
  *)
    exit 0
    ;;
esac

(
  SFTPGO_HOOKS_ROOT="${hooks_root}" exec "${worker}" "${path}" "120"
) >/dev/null 2>&1 &

exit 0
