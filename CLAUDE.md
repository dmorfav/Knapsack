# Knapsack

Proyecto académico de investigación: metaheurísticas para el problema de la mochila 0/1. Responde en español.

## Autoría humana

`knapsack-algorithms/src` es **autoría humana**: el autor lo escribe sin IA generativa, y ese es el propósito del proyecto (ver README).

- Tu terreno es el ecosistema: `knapsack-core`, `knapsack-bench`, POMs, instancias, CI, docs y arquitectura.
- Ante un bug dentro de un algoritmo, descríbelo en texto con fichero y línea; el autor decide y lo corrige.
- Los baselines de referencia (voraz, exacto) son ecosistema, no autoría humana.
- El registro `META-INF/services` de `knapsack-algorithms` también es autoría humana: el autor da de alta sus propios algoritmos.
- Los algoritmos originales importan `model.*` y `kp_hs.Constants`: esas clases viven en `knapsack-core` y conservan paquete y firma públicos, porque renombrarlas obligaría a editar autoría humana. Se ejecutan mediante los adaptadores de `knapsack-bench/.../legacy`.

El hook `.claude/hooks/guard.sh` bloquea la escritura en esa ruta; `.claude/hooks/guard_test.sh` es su batería de pruebas y se ejecuta tras cualquier cambio del hook.

## Git

El autor hace todos los commits. Deja los cambios en el working tree, sin stage, y enumera los ficheros tocados. Usa git solo para leer (`status`, `diff`, `log`); el mismo hook bloquea commit, push y PRs.

## Build

- Java 25 obligatorio (el enforcer falla con otra versión). El `JAVA_HOME` por defecto de esta máquina es JDK 21; exporta `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home` antes de `./mvnw`.
- Lanza `exec:java` desde la raíz: las rutas de `--instance` son relativas a ella. Los tests leen sus instancias del classpath.
- Todo solver se ejecuta con presupuesto de tiempo: los algoritmos originales pueden no terminar en instancias ajustadas.
