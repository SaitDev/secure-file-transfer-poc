#!/bin/sh

set -eu

if [ "$#" -ne 2 ]; then
  exit 1
fi

file_path="$1"
delay_seconds="$2"
hooks_root="${SFTPGO_HOOKS_ROOT:-/srv/sftpgo}"
log_dir="${hooks_root}/hooks-log"
log_file="${log_dir}/ack-cleanup.log"

case "${file_path}" in
  "${hooks_root}"/exchange/a2b/outbox/*)
    ack_dir="${hooks_root}/exchange/a2b/download-ack"
    flow="a2b"
    ;;
  "${hooks_root}"/exchange/b2a/outbox/*)
    ack_dir="${hooks_root}/exchange/b2a/download-ack"
    flow="b2a"
    ;;
  *)
    exit 0
    ;;
esac

/bin/sleep "${delay_seconds}"

file_name=$(/usr/bin/basename "${file_path}")
base_name="${file_name}"

strip_suffix() {
  suffix="$1"
  case "${base_name}" in
    *"${suffix}")
      base_name=${base_name%"${suffix}"}
      return 0
      ;;
  esac
  return 1
}

strip_suffix ".result.ack.pgp" || \
strip_suffix ".csv.pgp" || \
strip_suffix ".result.ack" || \
strip_suffix ".csv" || true

ack_path="${ack_dir}/${base_name}.download.ack"

/bin/mkdir -p "${log_dir}"

if [ ! -f "${ack_path}" ]; then
  printf '%s action=skip flow=%s file=%s reason=ack_missing ack=%s\n' \
    "$(/bin/date '+%Y-%m-%dT%H:%M:%S%z')" \
    "${flow}" \
    "${file_path}" \
    "${ack_path}" >> "${log_file}"
  exit 0
fi

/bin/rm -f -- "${file_path}" "${ack_path}"

printf '%s action=delete flow=%s file=%s ack=%s\n' \
  "$(/bin/date '+%Y-%m-%dT%H:%M:%S%z')" \
  "${flow}" \
  "${file_path}" \
  "${ack_path}" >> "${log_file}"
