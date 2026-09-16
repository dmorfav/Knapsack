# Experimentos y estadística

## Definición

Un experimento ejecuta varias veces cada solver sobre cada instancia, con semillas distintas y el mismo presupuesto. Se define en YAML dentro de `experiments/`:

```yaml
name: smoke                        # kebab-case; da nombre a la carpeta de resultados
description: Comprobación rápida
instances:                         # rutas desde la raíz; * no cruza carpetas, ** sí
  - instances/legacy-20.json
  - instances/pisinger/*.json
solvers: ["*"]                     # ids familia/variante; p. ej. "greedy/*" o "*/legacy"
runs: 5                            # semillas firstSeed, firstSeed + 1, ...
firstSeed: 1
warmupRuns: 1                      # ejecuciones de calentamiento por solver, no se guardan
budget:
  maxEvaluations: 100000           # opcional
  maxSeconds: 5                    # obligatorio: ningún solver puede bloquear el experimento
```

## Ejecución

Las ejecuciones corren en secuencia, de una en una:

1. **Aislamiento:** cada solver corre en **su propia JVM**, que se reutiliza entre sus ejecuciones para conservar el calentamiento. El banco solo le envía la instancia, la semilla y el presupuesto, y valida lo que devuelve.
2. **Calentamiento:** cada solver hace `warmupRuns` ejecuciones que no se guardan, para que el compilador JIT no penalice al primero.
3. **Orden barajado:** las combinaciones de instancia, solver y semilla se ejecutan en un orden aleatorio reproducible con `firstSeed`. Así ningún solver se beneficia siempre de ir primero o después.
4. **Validación:** cada ejecución se valida. Es fallida si lanza una excepción, agota el presupuesto o devuelve una solución nula, de otro tamaño, no factible o mejor que el óptimo conocido.

### Límite duro

El presupuesto solo detiene a un solver cuando consulta el contexto. Para que un solver que no termina no bloquee el experimento, el banco aplica un **límite duro de `maxSeconds` + 5 s** a cada ejecución:

| Qué pasa | Resultado |
|---|---|
| La ejecución supera el límite duro | Se destruye el proceso y la ejecución falla con "no terminó en el límite duro" |
| El proceso muere (`System.exit`, fallo de la JVM) | La ejecución falla indicando el código de salida |
| `StackOverflowError` u `OutOfMemoryError` | La ejecución falla como una excepción y el proceso sigue |
| Tercer fallo de proceso del mismo solver | El solver queda **descalificado**: sus ejecuciones restantes fallan sin lanzarse |

Tras un fallo de proceso, la siguiente ejecución arranca una JVM nueva, sin calentar. El manifiesto registra el modo de ejecución en `execution`. `run` y `experiment` aceptan `--in-process` para depurar en la misma JVM, sin límite duro.

Cada experimento escribe en `results/<nombre>/<fecha UTC>/`:

| Fichero | Contenido |
|---|---|
| `manifest.json` | Configuración, instancias con su SHA-256, solvers con su [procedencia](#procedencia-del-codigo), entorno (JDK, SO, CPU, núcleos, memoria) y commit de git, indicando si había cambios sin commitear |
| `runs.jsonl` | Una línea por ejecución: estado, beneficio, peso, GAP, evaluaciones, tiempo, evaluaciones y tiempo hasta alcanzar su resultado final, y la solución. **Es la fuente de verdad**: todo lo demás se deriva de ella |
| `traces.jsonl` | Curva de convergencia de cada ejecución: `[evaluación, nanosegundos, beneficio]` en cada mejora del mejor beneficio factible evaluado |
| `summary.csv` | Resumen por instancia y solver |

### Procedencia del código

Para cada solver, el manifiesto guarda de qué código salieron sus resultados:

| Campo | Qué es |
|---|---|
| `sources` | Ficheros fuente del proyecto de los que depende: se recorre el bytecode desde la clase del solver siguiendo las clases que usa de verdad (herencia, campos, firmas e instrucciones) |
| `codeHash` | SHA-256 de esas fuentes. No cambia con el compilador ni con los finales de línea |
| `gitAuthors` | Autores en git de esas fuentes, sin contar `knapsack-core` ni `knapsack-bench` |
| `uncommittedChanges` | Si alguna de esas fuentes tenía cambios sin commitear. `experiment` lo avisa al empezar |

Las páginas de resultados muestran la tabla de solvers con autor declarado, autores en git y huella. Si la huella de una variante cambia respecto a la ejecución anterior del mismo experimento, lo avisan, y la página de evolución lo lista: los puntos de antes y después no son comparables.

La huella de un algoritmo original incluye su adaptador (`LegacySolvers.java`), así que tocar el adaptador cambia la de los cinco.

## Métricas del resumen

Las métricas de calidad y esfuerzo usan solo las ejecuciones correctas; las fallidas se cuentan aparte.

| Métrica | Definición |
|---|---|
| Correctas | Ejecuciones válidas sobre el total |
| Mejor, media, mediana, desviación y peor beneficio | Sobre las ejecuciones correctas; la desviación es muestral ($n - 1$) |
| GAP mínimo, medio y mediano | Ver [GAP](referencias.md#gap) |
| Aciertos del óptimo | Ejecuciones que alcanzaron el óptimo; solo si se conoce |
| Evaluaciones medianas | Esfuerzo total, independiente de la máquina |
| Evaluaciones hasta el mejor | Primera evaluación en la que se alcanzó el beneficio finalmente devuelto |
| ms medianos | Tiempo de reloj; ver las limitaciones |

## Comparación estadística

La comparación entre solvers sigue a J. Demšar, *Statistical comparisons of classifiers over multiple data sets*, JMLR, 2006. Cada **instancia es un bloque** y la medida es el **GAP mediano** de cada solver en ella. Un solver sin ninguna ejecución correcta en una instancia cuenta como el peor en ella.

- **Friedman**, con corrección de empates, contrasta si todos los solvers rinden igual. Se acompaña de los **rangos medios**: 1 es el mejor.
- **Diferencia crítica de Nemenyi** ($\alpha = 0.05$): dos solvers cuyos rangos medios difieren menos que ella no se distinguen.
- **Wilcoxon** de rangos con signo por pares de solvers, bilateral. Es exacto sin empates y con 50 instancias o menos; si no, usa la aproximación normal con corrección de empates y de continuidad. Los p-valores se ajustan con **Holm** por el número de pares.
- **Perfil de rendimiento** de Dolan-Moré: para cada $\tau \ge 1$, la fracción de instancias en las que el beneficio mediano del solver está a un factor $\le \tau$ del mejor.
- **Curva de convergencia:** en puntos de control logarítmicos, la mediana entre ejecuciones del GAP del mejor valor encontrado hasta ese número de evaluaciones. Una ejecución que aún no encontró nada factible cuenta como la mochila vacía.

Las pruebas están validadas contra scipy en los tests.

## Limitaciones

!!! warning "Léelo antes de sacar conclusiones"
    - **Pocas instancias dan poca potencia.** Con 8 instancias y 28 pares, ningún par puede salir significativo tras Holm aunque las diferencias sean claras. Las conclusiones necesitan más instancias.
    - **Los tiempos dependen de la máquina.** Solo son comparables dentro de una misma ejecución. Los runners de GitHub Actions son compartidos y cambian de CPU, así que la medida principal de esfuerzo son las evaluaciones.
    - **Las evaluaciones se pueden eludir.** Un algoritmo puede leer pesos y beneficios y sumar por su cuenta sin llamar a `evaluate`. El contador mide lo que declara el algoritmo, no lo que hace.
    - **Los algoritmos `legacy` no son reproducibles**, porque ignoran la semilla. Además cuentan evaluaciones de otra forma: ver [Añadir un algoritmo](../guia/anadir-un-algoritmo.md#algoritmos-originales).
    - **Un solver que no respeta el presupuesto de tiempo consume hasta 5 s de más** por ejecución antes de que lo detenga el límite duro. Tras tres fallos de proceso queda descalificado, así que sus resultados en ese experimento están incompletos.
