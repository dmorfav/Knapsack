# Contribuir

Knapsack acepta algoritmos de otras personas con una condición: que los escriban a mano, como el resto. Esta página explica qué se comprueba automáticamente, qué no se puede comprobar y cómo se revisa.

## Flujo

1. **Implementa el algoritmo** siguiendo [Añadir un algoritmo](anadir-un-algoritmo.md), en `knapsack-algorithms`.
2. **Ejecuta `./mvnw verify`** en local. Incluye las reglas de arquitectura y el contrato de solvers.
3. **Abre una PR solo de algoritmo** y marca la declaración de autoría de la plantilla.
4. **El autor del proyecto la revisa.** Cuando entra en `main`, el algoritmo participa en el siguiente [experimento semanal](../metodologia/experimentos.md).

Los cambios de ecosistema (core, baselines, bench, instancias, CI o documentación) van en PRs aparte.

Toda contribución se publica bajo la [licencia del proyecto](../index.md#licencia-y-cita), la GNU GPL v3 o posterior: enviar una PR implica aceptarlo.

## Qué se comprueba automáticamente

| Comprobación | Dónde | Qué garantiza |
|---|---|---|
| Alcance de la PR | Workflow `Pull request` | Una PR que toca `knapsack-algorithms/src` no toca nada más: un algoritmo no puede venir con cambios en el banco que lo mide |
| Declaración de autoría | Workflow `Pull request` | La casilla de la plantilla está marcada en toda PR de algoritmo |
| Reglas de arquitectura | `ArchitectureTest` | Ver la tabla siguiente |
| Contrato de solvers | `SolverContractTest` | Ver [Contrato](#contrato) |
| Aislamiento | Ejecutor de experimentos | Un algoritmo que no termina, tumba su JVM o agota la memoria solo falla sus propias ejecuciones; ver [Ejecución](../metodologia/experimentos.md#ejecucion) |
| Procedencia | Manifiesto de cada experimento | Huella de las fuentes de cada solver y sus autores en git; el informe avisa si una variante publicada cambia de código |
| Revisión | `CODEOWNERS` y protección de rama | Ningún cambio entra en `main` sin aprobación del autor del proyecto |

### Reglas de arquitectura

`ArchitectureTest` analiza el bytecode de `knapsack-algorithms`:

| Regla | Motivo |
|---|---|
| No depende del banco de pruebas, de las referencias, de la lectura o generación de instancias ni de `RunContext` | Un algoritmo no puede ver el óptimo exacto, las instancias en disco ni fabricarse su propio contexto |
| No llama a `Instance.evaluate` | Toda evaluación pasa por `context.evaluate`, que la cuenta y aplica el presupuesto |
| Sin E/S, red, reflexión, hilos ni `System.exit` | Los tiempos solo son comparables con un único hilo y sin E/S dentro del bucle |
| Sin `Math.random`, `Random`, `SplittableRandom`, `RandomGenerator.of`/`getDefault` ni `Collections.shuffle` sin generador | Toda la aleatoriedad sale de `context.random()`: con la misma semilla, la misma ejecución |
| Sin `System.currentTimeMillis`, `System.nanoTime` ni `Instant.now` | El tiempo se consulta con `context.elapsed()` |
| Campos estáticos siempre `final` | Las ejecuciones de un solver comparten JVM: un campo estático mutable filtraría estado entre ellas |
| Toda implementación de `KnapsackSolver` es pública, tiene constructor sin argumentos y está registrada | Si no está registrada, nunca se ejecuta y nadie se entera |

Los cinco algoritmos originales están exentos solo de las reglas de aleatoriedad y reloj, en una lista explícita dentro del test.

### Contrato

`SolverContractTest` ejecuta cada solver registrado en su propio proceso, como en un experimento:

| Contrato | Algoritmos originales |
|---|---|
| Ids `familia/variante` únicos. El autor `baseline` está reservado a `knapsack-baselines` | Aplica |
| Soluciones válidas en casos límite (un objeto que cabe o no cabe, todos caben, ninguno cabe) y en instancias de Pisinger pequeñas con óptimo conocido | Aplica |
| La misma semilla con presupuesto de evaluaciones da la misma solución y las mismas evaluaciones | Exentos: ignoran la semilla |
| Con 50 evaluaciones termina bien y no usa más | Exentos: no consultan el presupuesto |
| Con 200 ms termina bien en menos de 2 s | Exentos, por lo mismo |

## Qué no se puede comprobar

!!! warning "La autoría humana se declara, no se demuestra"
    Ninguna herramienta distingue de forma fiable código escrito a mano de código generado con IA. La declaración de la PR es un compromiso, y la revisión es la única defensa.

Tampoco se comprueba automáticamente:

- **Que el algoritmo sea el que dice ser.** Un "recocido simulado" que en realidad es una búsqueda aleatoria pasa todas las reglas. Eso lo valora la revisión.
- **Que las evaluaciones sean honestas.** Un algoritmo puede sumar pesos y beneficios por su cuenta sin llamar a `evaluate`. Por eso las comparaciones de esfuerzo también muestran tiempos.
- **Que una variante publicada no cambie.** Nada impide modificarla, pero la [huella del código](../metodologia/experimentos.md#procedencia-del-codigo) de cada solver delata el cambio en las páginas de resultados. Si cambias el comportamiento de una variante con resultados, crea una variante nueva: la revisión lo exige.

## Revisión

La revisión se centra en lo que las máquinas no ven:

- La PR corresponde a un algoritmo con nombre y a la variante que declara.
- Quien la envía sabe explicar las decisiones del código cuando se le pregunta. No se busca perfección, sino proceso.
- Los resultados en la PR, si los hay, son reproducibles con `run` o con un experimento.
