# Handoff: fase de colaboradores (a medias)

Documento de traspaso para continuar con otro agente (opencode + API de Claude). No es documentación del proyecto: bórralo al terminar la fase, sin commitearlo.

Lee primero `CLAUDE.md`: las reglas del proyecto viven ahí y siguen vigentes.

## Reglas que el agente debe cumplir

1. **`knapsack-algorithms/src` es autoría humana.** El agente no crea, edita, mueve ni borra nada ahí, y eso incluye `META-INF/services`. Si descubre un bug en un algoritmo, lo describe en texto con fichero y línea.
2. **Git solo de lectura.** Nada de `commit`, `push` ni PRs: los cambios se quedan en el working tree, sin stage, y el agente enumera los ficheros tocados.
3. **Java 25:** `export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home` antes de `./mvnw`.
4. **Toda prueba estadística nueva** se valida contra valores de scipy citados en el test.

> **Los guardarraíles no viajan con el agente.** `.claude/hooks/guard.sh` solo actúa en Claude Code. En opencode hay que configurar permisos equivalentes en `opencode.json`. Comprueba la sintaxis exacta en la documentación de opencode antes de confiar en ella. La idea es esta:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "permission": {
    "edit": "ask",
    "bash": {
      "git commit*": "deny",
      "git push*": "deny",
      "gh pr *": "deny",
      "*": "ask"
    }
  }
}
```

opencode lee `AGENTS.md` y, si no existe, `CLAUDE.md`. No hace falta duplicar las reglas.

Comprobación de que los algoritmos no han cambiado, desde la raíz:

```bash
git diff --stat -- knapsack-algorithms/ && git status --short -- knapsack-algorithms/   # ambos deben salir vacíos
```

## Dónde estamos en el plan

| Fase | Estado |
|---|---|
| 0. Guardarraíles, Java 25, multimódulo | Hecha y commiteada |
| 1. Core: modelo, contrato `KnapsackSolver`, instancias JSON | Hecha y commiteada |
| 2. Referencias (voraz, voraz + mejor objeto, DP exacta, cota de Dantzig), generador de Pisinger, CLI picocli | Hecha y commiteada |
| 3. Runner de experimentos YAML, `runs.jsonl`, `traces.jsonl`, `summary.csv` | Hecha y commiteada |
| 4. Estadística (Friedman, Nemenyi, Wilcoxon, Holm, perfiles), MkDocs, workflows `docs.yml` y `benchmark.yml` | Hecha y commiteada |
| **5. Colaboradores: garantías de calidad** | **En curso (≈20 %)** |

## Fase 5: objetivo

El autor quiere poder invitar a colaboradores sin que el proyecto "se convierta en un churro". Las garantías tienen que ser automáticas donde se pueda y declaradas donde no. Ninguna herramienta detecta código escrito con IA: la autoría humana de los colaboradores se basa en una declaración y en la revisión.

Piezas, en orden:

### 5.1 Aislamiento por proceso con límite duro (EN CURSO)

**Problema:** todo corre en una JVM. Un algoritmo que entra en bucle sin llamar a `context.evaluate` bloquea el benchmark semanal hasta el timeout de 180 min y se pierden todos los resultados. Un `System.exit`, un `StackOverflowError` o un `OutOfMemoryError` tumban el experimento.

**Diseño:**

- **Una JVM hija por clase de solver**, reutilizada entre ejecuciones para conservar el calentamiento del JIT. El orden barajado de tareas no cambia.
- **Protocolo JSON Lines** por stdin/stdout. Cada instancia se envía una vez por proceso y después se cita por id. La JVM hija redirige `System.out` a stderr para que lo que imprima un solver no corrompa el protocolo.
- **Límite duro:** `budget.maxTime + margen`, con 5 s de margen por defecto. Si se supera o el proceso muere:
  - se destruye el proceso con `destroyForcibly`;
  - la ejecución queda como fallida con un mensaje claro;
  - la siguiente ejecución arranca un proceso nuevo, que ya no está calentado.
- **Descalificación:** tras 3 fallos de proceso de un mismo solver, sus ejecuciones restantes fallan sin lanzarse.
- **Validación en el padre:** tamaño, factibilidad y óptimo conocido. El hijo solo devuelve el intento sin validar (`SolverRun.Attempt`).
- **Classpath del hijo:** bajo `exec:java` el proyecto se carga en un `URLClassLoader`, así que se toman sus URLs. En surefire se usa `java.class.path`, que es un jar solo con manifiesto y sirve igual.
- **Requisito:** el solver tiene que ser una clase pública con constructor público sin argumentos, lo mismo que exige `ServiceLoader`.

**Hecho (compila, `./mvnw -o verify` en verde con 64 + 80 + 59 tests, sin tests nuevos todavía):**

| Fichero | Qué |
|---|---|
| `knapsack-bench/.../bench/SolverRun.java` (modificado) | Separa `attempt()`, que ejecuta y captura también `StackOverflowError` y `OutOfMemoryError`, de `of()`, que valida. Añade el record `Attempt`. `execute()` mantiene su firma |
| `knapsack-bench/.../bench/execution/SolverExecutor.java` (nuevo) | Interfaz con `execute`, `description` y `close`. `create(inProcess)` y la implementación `InProcessExecutor` |
| `knapsack-bench/.../bench/execution/IsolatedExecutor.java` (nuevo) | Procesos, límite duro, reinicio, descalificación y detección del classpath |
| `knapsack-bench/.../bench/execution/WorkerProtocol.java` (nuevo) | Records `Request`, `InstancePayload` y `Response` |
| `knapsack-bench/.../bench/execution/SolverWorker.java` (nuevo) | Bucle del hijo (`serve`) e `instantiate(className)` |
| `knapsack-bench/.../bench/cli/WorkerCommand.java` (nuevo) | Subcomando oculto `worker --solver-class=<fqcn>` |
| `knapsack-bench/.../bench/Main.java` (modificado) | Registra `WorkerCommand` |

**Falta:**

1. **`ExperimentRunner`:** recibir un `SolverExecutor` en el constructor y usarlo en `warmUp` y en el bucle de tareas, en lugar de `SolverRun.execute`. El ejecutor lo crea y lo cierra quien llama, con try-with-resources.
2. **`Manifest`:** añadir `String execution` con `executor.description()` justo después de `runOrder`. Los manifiestos antiguos sin ese campo tienen que seguir leyéndose (quedará null). `ReportGenerator` debe mostrarlo en la tabla de detalles: `row(md, "Ejecución", ...)`, junto a `row(md, "Orden", ...)`.
3. **`ExperimentCommand` y `RunCommand`:** opción `--in-process` (booleana, "para depurar") y `try (SolverExecutor executor = SolverExecutor.create(inProcess))`. `RunCommand` imprime la línea del ejecutor bajo la del presupuesto.
4. **Tests:**
   - `ExperimentRunnerTest`: pasar `new SolverExecutor.InProcessExecutor()`, porque sus solvers falsos son lambdas y no se pueden aislar.
   - `execution/WorkerProtocolTest` o `SolverWorkerTest`: `serve` en proceso con `ByteArrayInputStream`/`ByteArrayOutputStream`. Comprobar la instancia enviada una sola vez, el viaje de ida y vuelta de solución, traza y fallo, y el error si se cita una instancia no enviada. Así cuenta para JaCoCo.
   - `execution/IsolatedExecutorTest`, con procesos reales y solvers de test como clases públicas de primer nivel en `src/test/java/.../execution/`:
     - `HangingSolver`: `while (true)` sin evaluar. Con presupuesto de 300 ms y margen de 1 s, falla con "límite duro".
     - `ExitingSolver`: `Runtime.getRuntime().halt(3)`. Falla con "código 3".
     - `NoisySolver`: imprime en `System.out` y devuelve la mochila vacía. Tiene que salir bien, sin corromper el protocolo.
     - Tras `maxProcessFailures` fallos (usar `IsolatedExecutor.forCurrentJvm(grace, 2)`), el mensaje es "descalificado" y la ejecución es instantánea.
     - Equivalencia: `DynamicProgrammingSolver` aislado y en proceso dan la misma solución, evaluaciones y tamaño de traza.
     - Un solver no aislable (anónimo) lanza `IllegalArgumentException`.
   - `CommandsTest`: el `run` ya usa procesos. Comprobar que sigue en verde y que su tiempo es razonable, unos pocos segundos por arrancar una JVM por solver.
5. **Probar en real:** `./mvnw -q compile exec:java -Dexec.args="experiment experiments/smoke.yaml"`. Es la prueba de que el classpath del `URLClassLoader` de `exec:java` funciona; los tests no pasan por ahí. Después, `report` y `mkdocs build --strict`.

### 5.2 Tests de arquitectura con ArchUnit (pendiente)

- **Dónde:** en `knapsack-bench/src/test` (`ArchitectureTest`), porque el agente no puede escribir en `knapsack-algorithms/src`, ni siquiera tests.
- **Dependencia:** `com.tngtech.archunit:archunit:1.5.0`, con scope test y versión en el POM padre. Usar el artefacto `archunit` sin el motor JUnit 5, con `@Test` normal y `rule.check(classes)`, porque el proyecto usa JUnit 6.
- **Importar solo `knapsack-algorithms`:** `new ClassFileImporter().withImportOption(location -> location.contains("knapsack-algorithms")).importClasspath()`. Afirmar que importa algo (por ejemplo `algorithms.HarmonySearch`) para que la regla no pase en vacío.

Reglas para las clases de `knapsack-algorithms`:

| Regla | Motivo |
|---|---|
| No depender de `org.dmorfav.knapsack.bench..`, `..baselines..`, `..core.io..`, `..core.generator..`, `RunContext`, Jackson ni picocli | Un algoritmo no puede ver el runner, ni el óptimo exacto, ni fabricarse su propio contexto |
| No llamar a `Instance.evaluate` | Evaluaría sin contar: se salta el presupuesto de evaluaciones |
| Sin `java.io..`, `java.nio..`, `java.net..` ni `java.lang.reflect..` | Sin E/S en bucles cronometrados y sin trampas |
| Sin `java.util.concurrent..`, `Thread`, `Runtime` ni `System.exit` | Un solo hilo: la comparación de tiempos es justa |
| Sin `Math.random`, `StrictMath.random`, `java.util.Random`, `SplittableRandom`, `RandomGeneratorFactory`, `RandomGenerator.of/getDefault`, `Collections.shuffle(List)` de un argumento, `UUID.randomUUID` ni `SecureRandom` | Toda la aleatoriedad sale de `context.random()`: reproducibilidad |
| Sin `System.currentTimeMillis` ni `System.nanoTime` | El tiempo se consulta en `context.elapsed()` |
| Campos estáticos siempre `final` | Sin estado compartido entre ejecuciones en la misma JVM |
| Toda clase que implemente `KnapsackSolver` es pública, tiene constructor público sin argumentos y aparece en `SolverCatalog.discover()` | Si no está registrada, no se ejecuta nunca y nadie se entera |

- **Excepción explícita:** los 5 algoritmos originales usan `Math.random` y `System.currentTimeMillis`, así que quedan fuera **solo** de las reglas de aleatoriedad y reloj. Van en una lista con sus nombres completos (`algorithms.HarmonySearch`, etc.) y un comentario: cuando el autor migre uno al contrato, sale de la lista.
- Cada regla lleva un `.because("...")` en español que explique el motivo al colaborador.

### 5.3 Tests de contrato para todo solver registrado (pendiente)

`SolverContractTest` en `knapsack-bench/src/test`, parametrizado sobre `SolverCatalog.discover()`. Se ejecuta con un `IsolatedExecutor` compartido (`@BeforeAll`/`@AfterAll`), así que un bucle infinito falla el test en lugar de colgar el build.

| Contrato | Legacy (`org.dmorfav.knapsack.bench.legacy`) |
|---|---|
| Ids únicos en el catálogo; autor no vacío; solo las clases de `knapsack-baselines` usan el autor `baseline` | Aplica |
| Instancias límite (un objeto que cabe, uno que no cabe, todos caben, Pisinger pequeñas de varias clases con `PisingerGenerator.generate(new GeneratorSpec(...))`): solución factible, del tamaño correcto y sin superar el óptimo | Solo se exige que termine sin fallo de proceso; el resto se registra |
| Reproducible: la misma semilla da la misma solución y las mismas evaluaciones | Exento: ignoran la semilla |
| Respeta el presupuesto de evaluaciones: con `Budget.ofEvaluations(50)` termina bien y usa ≤ 50 | Exento: solo paran cuando una evaluación lanza la excepción |
| Respeta el tiempo: con 200 ms termina bien en menos de 2 s | Exento, por lo mismo |

Antes de fijar las expectativas, conviene ejecutar el test y ver qué hacen hoy las referencias y los legacy. Si una referencia no cumple, se arregla la referencia (es ecosistema). Si falla un legacy, se describe en texto.

### 5.4 Gobierno del repositorio (pendiente)

- **`.github/CODEOWNERS`:** `* @dmorfav`.
- **`.github/pull_request_template.md`:**
  - Tipo de PR: algoritmo o ecosistema.
  - Casilla de declaración: "Declaro que el código de `knapsack-algorithms` de esta PR lo he escrito yo, sin herramientas de IA generativa (generación, autocompletado con IA, corrección ni sugerencias)".
  - Variante nueva si cambia el comportamiento de una con resultados publicados.
  - Tests en verde.
- **Workflow `.github/workflows/pull-request.yml`:**
  - Falla si una PR mezcla cambios en `knapsack-algorithms/**` con cambios fuera de esa carpeta. Una PR de algoritmo solo toca algoritmos.
  - Si toca `knapsack-algorithms/**`, exige la casilla de declaración marcada (`- [x] Declaro`) en el cuerpo de la PR, leído por `env` desde `github.event.pull_request.body` y nunca interpolado en el script.
- **`CONTRIBUTING.md`:** mínimo, con enlace a una página nueva `docs/guia/contribuir.md` (en el nav de `mkdocs.yml`, bajo Guía). La página explica:
  - el flujo;
  - qué se comprueba automáticamente: ArchUnit, contrato, aislamiento y límite de la PR;
  - qué no se puede comprobar: la autoría sin IA, que se declara;
  - la revisión.
- **`build.yml`:** `timeout-minutes: 30` en el job.

### 5.5 Documentación de la fase (pendiente)

- `docs/metodologia/experimentos.md`:
  - la sección "Ejecución" dice "en una sola JVM": describir los procesos, el límite duro y la descalificación;
  - en "Limitaciones", sustituir la viñeta "Todo corre en la misma JVM".
- `docs/guia/anadir-un-algoritmo.md`: enlazar las reglas de ArchUnit y el contrato. Convención de paquete para algoritmos nuevos: `org.dmorfav.knapsack.algorithms.<familia>`.
- `docs/guia/ejecutar.md`: opción `--in-process`.
- `docs/arquitectura.md`: paquete `…bench.execution` en la tabla y el proceso hijo en el diagrama de flujo.
- `docs/uso-de-ia.md`: añadir a "Qué se garantiza y qué no" las filas de ArchUnit, contrato, plantilla y workflow de PR.
- `.devin/wiki.json`: mencionar `execution`, `ArchitectureTest` y `SolverContractTest` en las notas de "Bench CLI", "Experiments" y "Development Workflow".
- `CLAUDE.md` (y `AGENTS.md` si se crea): "Si una regla de ArchUnit o de contrato falla por un algoritmo, no la relajes: describe el fallo al autor".
- Verificar con `mkdocs build --strict` (entorno en `.venv`: `.venv/bin/mkdocs`).

### Ideas aplazadas (no empezar sin que el autor lo pida)

- **Autor verificado desde git:** guardar en el manifiesto los autores de `git log` del fichero fuente de cada solver, además del autor que declara `SolverInfo`.
- **Versionado de variantes por hash** del bytecode o del fuente, para detectar que una variante con resultados publicados cambió.
- **Suite de instancias definitiva ("suite v1"):** con 8 instancias ningún par sale significativo tras Holm. Es una decisión de investigación del autor.

## Pasos manuales del autor (siguen pendientes)

1. Settings → Pages → fuente **GitHub Actions**. Sin eso, la web actual se rompe, porque `docs/` ya no contiene el site de Maven.
2. Lanzar a mano el workflow **Benchmark** la primera vez. El paso que crea la rama `results` y hace commit con el bot no se pudo probar en local. Si falla el push, revisar Settings → Actions → Workflow permissions.
3. Secreto `CODECOV_TOKEN`.
4. Tras la fase 5: protección de rama en `main` con revisión obligatoria de CODEOWNERS y los checks `Java CI` y `pull-request`.

## Hallazgos sobre los algoritmos (solo para el autor; no tocar el código)

- `HillClimbingFirstAscent.java:47-52` no termina en instancias ajustadas: unos 2,2 M de evaluaciones en 5 s sobre `legacy-100`. Solo lo para el presupuesto.
- `HillClimbingBestAscent`: GAP de ~94 % y siempre 2022 evaluaciones. Parte de la mochila vacía (`:33`); el vecindario está en `:59-66`.
- Los Harmony Search llegan muy pronto a su resultado final, con `kp_hs.Constants.Iterations = 10`.
- `sortHM` hace una sola pasada y hay copias de `NVAR-1` elementos.
- Ninguno es reproducible: usan `Math.random`.

## Comandos de verificación

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
./mvnw verify                                                                   # tests de los 4 módulos
./mvnw -q compile exec:java -Dexec.args="experiment experiments/smoke.yaml"     # experimento real (~5 s)
./mvnw -q compile exec:java -Dexec.args="report"                                # páginas de resultados
.venv/bin/mkdocs build --strict                                                 # web
bash .claude/hooks/guard_test.sh                                                # solo si se toca el hook
git status --short                                                              # enumerar ficheros tocados al terminar
```
