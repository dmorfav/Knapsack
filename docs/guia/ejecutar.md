# Ejecutar el proyecto

Requiere **Java 25**. Todos los comandos se lanzan desde la raíz del repositorio con el Maven Wrapper:

```bash
./mvnw verify               # compila y ejecuta los tests
./mvnw compile exec:java    # ejecuta todos los solvers sobre instances/legacy-20.json
```

!!! tip "Si Maven dice que la versión de Java no es válida"
    El build exige Java 25. Apunta `JAVA_HOME` a un JDK 25 antes de lanzar `./mvnw`.

## Línea de comandos

Los subcomandos se pasan con `-Dexec.args="..."`, por ejemplo `./mvnw -q compile exec:java -Dexec.args="experiment experiments/smoke.yaml"`. `help <subcomando>` muestra todas sus opciones.

`run` y `experiment` ejecutan cada solver en su propia JVM, con un [límite duro](../metodologia/experimentos.md#limite-duro). Para depurar un algoritmo en la misma JVM, por ejemplo con puntos de interrupción, añade `--in-process`.

| Subcomando | Qué hace | Ejemplo |
|---|---|---|
| `run` | Ejecuta una vez cada solver registrado y muestra beneficio, GAP, evaluaciones y tiempo | `run --instance=instances/pisinger/subset-sum-n100-r1000-h50of100-s1.json --seed=7 --max-seconds=5` |
| `experiment` | Ejecuta un [experimento](../metodologia/experimentos.md) definido en YAML y guarda sus resultados | `experiment experiments/smoke.yaml` |
| `summarize` | Regenera el resumen y la comparación estadística de una carpeta de resultados | `summarize results/smoke/20260916-180840` |
| `report` | Genera las páginas de resultados de esta web a partir de `results/` | `report --results=results --output=docs/resultados` |
| `generate` | Genera una [instancia](../metodologia/instancias.md) de Pisinger y calcula su óptimo | `generate --class=strongly-correlated --items=200 --range=1000 --seed=3` |
| `suite` | Genera o comprueba (`--check`) las instancias de una [suite](../metodologia/instancias.md#suites) | `suite --check instances/suite-v1/suite.yaml` |
| `optimum` | Calcula el óptimo exacto de instancias existentes y lo contrasta con el declarado | `optimum --write instances/legacy-20.json` |

## Documentación en local

La web se construye con [MkDocs Material](https://squidfunk.github.io/mkdocs-material/). Las páginas de resultados no se guardan en git: las genera `report`.

```bash
python3 -m venv .venv && .venv/bin/pip install -r docs/requirements.txt
./mvnw -q compile exec:java -Dexec.args="report"
.venv/bin/mkdocs serve
```
