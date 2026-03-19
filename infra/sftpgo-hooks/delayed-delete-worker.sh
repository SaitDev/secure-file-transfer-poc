#!/bin/sh

set -eu

if [ "$#" -ne 2 ]; then
  exit 1
fi

trigger_path="$1"
delay_seconds="$2"
hooks_root="${SFTPGO_HOOKS_ROOT:-/srv/sftpgo}"
log_dir="${hooks_root}/hooks-log"
log_file="${log_dir}/ack-cleanup.log"

file_path=""
ack_path=""

case "${trigger_path}" in
  "${hooks_root}"/exchange/a2b/outbox/*)
    flow="a2b"
    file_path="${trigger_path}"
    ;;
  "${hooks_root}"/exchange/b2a/outbox/*)
    flow="b2a"
    file_path="${trigger_path}"
    ;;
  "${hooks_root}"/exchange/a2b/download-ack/*.download.ack)
    flow="a2b"
    ack_path="${trigger_path}"
    ;;
  "${hooks_root}"/exchange/b2a/download-ack/*.download.ack)
    flow="b2a"
    ack_path="${trigger_path}"
    ;;
  *)
    exit 0
    ;;
esac

/bin/sleep "${delay_seconds}"

resolve_base_name() {
  name="$1"
  case "${name}" in
    *.download.ack)
      printf '%s\n' "${name%.download.ack}"
      return 0
      ;;
    *.result.ack.pgp)
      printf '%s\n' "${name%.result.ack.pgp}"
      return 0
      ;;
    *.csv.pgp)
      printf '%s\n' "${name%.csv.pgp}"
      return 0
      ;;
    *.result.ack)
      printf '%s\n' "${name%.result.ack}"
      return 0
      ;;
    *.csv)
      printf '%s\n' "${name%.csv}"
      return 0
      ;;
  esac
  printf '%s\n' "${name}"
}

if [ -n "${file_path}" ]; then
  file_name=$(/usr/bin/basename "${file_path}")
  base_name="$(resolve_base_name "${file_name}")"
  ack_path="${hooks_root}/exchange/${flow}/download-ack/${base_name}.download.ack"
else
  ack_name=$(/usr/bin/basename "${ack_path}")
  base_name="$(resolve_base_name "${ack_name}")"
fi

if [ -z "${file_path}" ]; then
  find_matching_file() {
    dir="$1"
    for candidate in \
      "${dir}/${base_name}.csv.pgp" \
      "${dir}/${base_name}.result.ack.pgp" \
      "${dir}/${base_name}.result.ack" \
      "${dir}/${base_name}.csv"
    do
      if [ -f "${candidate}" ]; then
        printf '%s\n' "${candidate}"
        return 0
      fi
    done
    return 1
  }

  file_path="$(find_matching_file "${hooks_root}/exchange/${flow}/outbox" || true)"
fi

/bin/mkdir -p "${log_dir}"

if [ -z "${file_path}" ]; then
  printf '%s action=skip flow=%s trigger=%s reason=file_missing ack=%s\n' \
    "$(/bin/date '+%Y-%m-%dT%H:%M:%S%z')" \
    "${flow}" \
    "${trigger_path}" \
    "${ack_path}" >> "${log_file}"
  exit 0
fi

if [ ! -f "${ack_path}" ]; then
  printf '%s action=skip flow=%s trigger=%s file=%s reason=ack_missing ack=%s\n' \
    "$(/bin/date '+%Y-%m-%dT%H:%M:%S%z')" \
    "${flow}" \
    "${trigger_path}" \
    "${file_path}" \
    "${ack_path}" >> "${log_file}"
  exit 0
fi

/bin/rm -f -- "${file_path}" "${ack_path}"

printf '%s action=delete flow=%s trigger=%s file=%s ack=%s\n' \
  "$(/bin/date '+%Y-%m-%dT%H:%M:%S%z')" \
  "${flow}" \
  "${trigger_path}" \
  "${file_path}" \
  "${ack_path}" >> "${log_file}"
