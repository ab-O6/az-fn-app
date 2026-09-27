#!/usr/bin/env bash
# Re-exec with Bash when invoked as `sh scripts/run-local.sh`.
if [ -z "${BASH_VERSION:-}" ]; then
  exec bash "$0" "$@"
fi
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
[[ -f .env ]] || { echo 'Missing .env: copy .env.example to .env and configure it.' >&2; exit 1; }
# Parse literal values instead of sourcing executable code; preserve $Default and semicolons.
while IFS= read -r line || [[ -n "$line" ]]; do
  line="${line%$'\r'}"
  [[ "$line" =~ ^[[:space:]]*(#|$) ]] && continue
  [[ "$line" =~ ^[A-Za-z_][A-Za-z0-9_]*= ]] || { echo 'Invalid .env assignment' >&2; exit 1; }
  key="${line%%=*}"
  value="${line#*=}"
  if [[ "$value" == \"*\" && "$value" == *\" ]] || [[ "$value" == \'*\' && "$value" == *\' ]]; then
    value="${value:1:${#value}-2}"
  fi
  export "$key=$value"
done < .env
# Maven can find java on PATH, but the Functions Java worker needs JAVA_HOME.
if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ "$(uname -s)" == Darwin ]]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 25 2>/dev/null)" || {
      echo 'JDK 25 not found. Install it or set JAVA_HOME in .env.' >&2; exit 1;
    }
  else
    java_command="$(command -v java || true)"
    [[ -n "$java_command" ]] || { echo 'Java not found. Set JAVA_HOME to JDK 25.' >&2; exit 1; }
    java_settings="$("$java_command" -XshowSettings:properties -version 2>&1)"
    JAVA_HOME="$(printf '%s\n' "$java_settings" | sed -n 's/^[[:space:]]*java.home = //p')"
  fi
fi
[[ -x "$JAVA_HOME/bin/java" && -x "$JAVA_HOME/bin/javac" ]] || {
  echo 'JAVA_HOME must point to a JDK directory containing bin/java and bin/javac.' >&2; exit 1;
}
java_settings="$("$JAVA_HOME/bin/java" -XshowSettings:properties -version 2>&1)"
java_version="$(printf '%s\n' "$java_settings" | sed -n 's/^[[:space:]]*java.specification.version = //p')"
[[ "$java_version" == 25 ]] || {
  echo "Local Azure runtime requires JDK 25; JAVA_HOME selects Java $java_version." >&2; exit 1;
}
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
echo "Using JDK 25: $JAVA_HOME"
command -v func >/dev/null || { echo 'Azure Functions Core Tools 4.x (func) is required.' >&2; exit 1; }
if [[ "${AzureWebJobsStorage:-}" == 'UseDevelopmentStorage=true' ]]; then
  echo 'Local storage requires Azurite running on ports 10000-10002 (see README).'
fi
[[ -f local.settings.json ]] || cp local.settings.json.example local.settings.json
exec ./mvnw clean package azure-functions:run "$@"
