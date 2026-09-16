#!/usr/bin/env bash
# Casos de prueba para .claude/hooks/guard.sh. Uso: guard_test.sh <raíz del repo>
cd "$1"
fails=0
t() {
  local exp="$1" tool="$2" field="$3" value="$4"
  local json out got
  json="$(jq -nc --arg t "$tool" --arg f "$field" --arg v "$value" '{tool_name:$t, tool_input:{($f):$v}}')"
  out="$(printf '%s' "$json" | .claude/hooks/guard.sh)"
  if [[ "$out" == *'"deny"'* ]]; then got=DENY; else got=ALLOW; fi
  if [[ $got == "$exp" ]]; then r=ok; else r=FALLO; fails=$((fails+1)); fi
  printf '%-5s %-5s %s\n' "$r" "$got" "$value"
}
ALG="$PWD/knapsack-algorithms/src/main/java/algorithms"

t DENY  Edit  file_path "$ALG/HarmonySearch.java"
t DENY  Write file_path "$ALG/New.java"
t DENY  Write file_path "$PWD/knapsack-algorithms/src/main/resources/META-INF/services/X"
t ALLOW Edit  file_path "$PWD/knapsack-algorithms/pom.xml"
t ALLOW Edit  file_path "$PWD/knapsack-core/src/main/java/model/Knapsack.java"

t DENY  Bash command 'git commit -m x'
t DENY  Bash command 'git add . && git commit -m "x"'
t DENY  Bash command 'git -C /tmp/r push origin main'
t DENY  Bash command 'gh pr create --fill'
t ALLOW Bash command 'git status --short && git log --oneline -5'
t ALLOW Bash command 'git diff HEAD'
t ALLOW Bash command './mvnw -q verify'

t DENY  Bash command 'sed -i "" s/a/b/ knapsack-algorithms/src/main/java/algorithms/HarmonySearch.java'
t DENY  Bash command "sed -i '' 's/a/b/' knapsack-algorithms/src/main/java/algorithms/HarmonySearch.java"
t DENY  Bash command 'echo x > knapsack-algorithms/src/main/java/algorithms/X.java'
t DENY  Bash command 'echo x > "knapsack-algorithms/src/main/java/algorithms/X.java"'
t DENY  Bash command 'cat a | tee knapsack-algorithms/src/main/java/algorithms/X.java'
t DENY  Bash command 'rm knapsack-algorithms/src/main/java/algorithms/X.java'
t DENY  Bash command 'git checkout -- knapsack-algorithms/src'
t ALLOW Bash command 'grep -rn sortHM knapsack-algorithms/src 2>/dev/null'
t ALLOW Bash command 'grep -n "a > b" knapsack-algorithms/src/main/java/algorithms/HarmonySearch.java'
t ALLOW Bash command "for f in knapsack-algorithms/src/main/java/algorithms/*.java; do git log --format=%an -- \$f | sed 's/<[^>]*>//g'; done"
t ALLOW Bash command 'cd knapsack-algorithms/src/main/java/algorithms && shasum -a 256 *.java 2>&1'

echo "fallos: $fails"
exit "$fails"
