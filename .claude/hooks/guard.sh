#!/usr/bin/env bash
# PreToolUse: los algoritmos se escriben sin IA y los commits los hace solo el autor humano.
set -euo pipefail

input="$(cat)"
tool="$(jq -r '.tool_name // ""' <<<"$input")"

deny() {
  jq -n --arg reason "$1" '{
    hookSpecificOutput: {
      hookEventName: "PreToolUse",
      permissionDecision: "deny",
      permissionDecisionReason: $reason
    }
  }'
  exit 0
}

ALGORITHMS_MSG="knapsack-algorithms/src es código escrito a mano sin IA generativa. Claude no puede crearlo ni modificarlo: describe el problema en texto y deja que lo haga el autor."
GIT_MSG="En este repo Claude no hace commits, push ni PRs. Deja los cambios en el working tree e indica qué ficheros cambiaron."

case "$tool" in
  Edit|Write|MultiEdit|NotebookEdit)
    path="$(jq -r '.tool_input.file_path // .tool_input.notebook_path // ""' <<<"$input")"
    if [[ "$path" == *knapsack-algorithms/src* ]]; then
      deny "$ALGORITHMS_MSG"
    fi
    ;;
  Bash)
    cmd="$(jq -r '.tool_input.command // ""' <<<"$input")"
    if grep -Eq '(^|[^[:alnum:]_-])git([[:space:]]+[^;&|]*)?[[:space:]](commit|push)([[:space:]]|$)' <<<"$cmd" \
       || grep -Eq '(^|[^[:alnum:]_-])gh[[:space:]]+pr[[:space:]]+(create|merge)' <<<"$cmd"; then
      deny "$GIT_MSG"
    fi
    if grep -q 'knapsack-algorithms/src' <<<"$cmd"; then
      # Sin literales entre comillas: un '>' dentro de un patrón de sed/grep no es una redirección
      stripped="$(sed -E "s/'[^']*'//g; s/\"[^\"]*\"//g; s#[0-9]?>{1,2}[[:space:]]*/dev/null##g; s#[0-9]>&[0-9]##g" <<<"$cmd")"
      if grep -Eq '>|(^|[^[:alnum:]_-])(tee|mv|cp|rm|touch|ln|truncate|patch|install|rsync)([[:space:]]|$)|sed[[:space:]]+(-[[:alnum:]]*[[:space:]]+)*-i|perl[[:space:]]+-[[:alnum:]]*i|git[[:space:]]+(mv|rm|checkout|restore|apply|stash|reset|clean)' <<<"$stripped"; then
        deny "$ALGORITHMS_MSG"
      fi
    fi
    ;;
esac

exit 0
