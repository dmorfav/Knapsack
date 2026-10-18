# Knapsack

Proyecto académico de investigación: metaheurísticas para el problema de la mochila 0/1. Responde en español.

## Autoría humana

`knapsack-algorithms/src` es **autoría humana**: el autor lo escribe sin IA generativa, y ese es el propósito del proyecto (ver README).

- Tu terreno es el ecosistema: `knapsack-core`, `knapsack-baselines`, `knapsack-bench`, POMs, instancias, CI, docs y arquitectura.
- Ante un bug dentro de un algoritmo, descríbelo en texto con fichero y línea; el autor decide y lo corrige.
- Las baselines (voraz, exacto, cotas) son ecosistema: referencias para medir, con autor `baseline`.
- El registro `META-INF/services` de `knapsack-algorithms` también es autoría humana: el autor da de alta sus propios algoritmos.
- Los algoritmos originales importan `model.*` y `kp_hs.Constants`: esas clases viven en `knapsack-core` y conservan paquete y firma públicos, porque renombrarlas obligaría a editar autoría humana. Se ejecutan mediante los adaptadores de `knapsack-bench/.../legacy`.

El hook `.claude/hooks/guard.sh` bloquea la escritura en esa ruta; `.claude/hooks/guard_test.sh` es su batería de pruebas y se ejecuta tras cualquier cambio del hook.

## Git

El autor hace todos los commits. Deja los cambios en el working tree, sin stage, y enumera los ficheros tocados. Usa git solo para leer (`status`, `diff`, `log`); el mismo hook bloquea commit, push y PRs.

## Build

- Java 25 o superior (el enforcer falla con menos; el CI usa 25). El `JAVA_HOME` por defecto de esta máquina es JDK 21; exporta `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home` (JDK de Homebrew) antes de `./mvnw`.
- Lanza la CLI desde la raíz con `./mvnw -q compile exec:java -Dexec.args="<subcomando> ..."` (`run`, `experiment`, `summarize`, `report`, `generate`, `suite`, `optimum`): las rutas son relativas a la raíz y el reactor compila todos los módulos. Los tests leen sus instancias del classpath.
- `runs.jsonl` es la fuente de verdad de un experimento; todo resumen, comparación o informe se deriva de ella. `results/` está ignorado: los resultados oficiales viven en la rama `results`, que escribe el workflow `benchmark.yml`.
- Toda prueba estadística nueva se valida en tests contra valores de referencia de scipy, citados en el test.
- `ArchitectureTest` y `SolverContractTest` (en `knapsack-bench`) son las garantías para colaboradores. Si fallan por un algoritmo, no relajes la regla ni el contrato: describe el fallo al autor. `run` y `experiment` aíslan cada solver en su JVM; `--in-process` solo para depurar.

## Documentación

- La documentación de uso y metodología vive en `docs/` (MkDocs Material) y el README es solo portada: cambia la página de `docs/` y enlázala, sin duplicar contenido en el README.
- `docs/resultados/` lo genera `report` y está ignorado; `report` se niega a borrar una carpeta que no generó.
- Comprueba cada cambio visible con `mkdocs build --strict` y revisa en el navegador las páginas con gráficas: las especificaciones Vega-Lite pueden ser JSON válido y aun así no dibujarse.
- Todo óptimo que se escriba en una instancia sale de `optimum --write`, `generate` o `suite`, nunca a mano: `run` da por buenos los óptimos declarados. Las carpetas con `suite.yaml` solo se regeneran con `suite`; el CI las comprueba con `--check`.
- Todo solver se ejecuta con presupuesto de tiempo: los algoritmos originales pueden no terminar en instancias ajustadas.
