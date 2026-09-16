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

Las ejecuciones corren en secuencia en una sola JVM:

1. **Calentamiento:** cada solver hace `warmupRuns` ejecuciones que no se guardan, para que el compilador JIT no penalice al primero.
2. **Orden barajado:** las combinaciones de instancia, solver y semilla se ejecutan en un orden aleatorio reproducible con `firstSeed`. Así ningún solver se beneficia siempre de ir primero o después.
3. **Validación:** cada ejecución se valida. Es fallida si lanza una excepción, agota el presupuesto o devuelve una solución nula, de otro tamaño, no factible o mejor que el óptimo conocido.

Cada experimento escribe en `results/<nombre>/<fecha UTC>/`:

| Fichero | Contenido |
|---|---|
| `manifest.json` | Configuración, instancias con su SHA-256, solvers con autor, entorno (JDK, SO, CPU, núcleos, memoria) y commit de git, indicando si había cambios sin commitear |
| `runs.jsonl` | Una línea por ejecución: estado, beneficio, peso, GAP, evaluaciones, tiempo, evaluaciones y tiempo hasta alcanzar su resultado final, y la solución. **Es la fuente de verdad**: todo lo demás se deriva de ella |
| `traces.jsonl` | Curva de convergencia de cada ejecución: `[evaluación, nanosegundos, beneficio]` en cada mejora del mejor beneficio factible evaluado |
| `summary.csv` | Resumen por instancia y solver |

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
    - **Todo corre en la misma JVM.** Un algoritmo que no termina solo se detiene si evalúa. Uno que no evalúa nunca bloquearía el experimento.
