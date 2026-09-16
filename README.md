# Knapsack

[![codecov](https://codecov.io/gh/dmorfav/Knapsack/branch/main/graph/badge.svg?token=NTCYS1ATLB)](https://codecov.io/gh/dmorfav/Knapsack)
![Testing Suite](https://github.com/dmorfav/knapsack/actions/workflows/build.yml/badge.svg)
![Testing Suite](https://github.com/dmorfav/knapsack/actions/workflows/qa.yml/badge.svg)
[![Open Source? Siii!](https://badgen.net/badge/Open%20Source%20%3F/Siii%21/blue?icon=github)](https://github.com/dmorfav/Knapsack)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/dmorfav/Knapsack)

## Suite de prueba de algoritmos metaheurísticos basando en el problema de la [mochila](https://en.wikipedia.org/wiki/Knapsack_problem)

>El problema de la mochila es uno de los 21 problemas NP-completos de Richard Karp, establecidos por el informático teórico en un famoso artículo de 1972.1. Ha sido intensamente estudiado desde mediados del siglo XX y se hace referencia a él en el año 1897, en un artículo de George Mathews Ballard.2
Si bien la formulación del problema es sencilla, su resolución es más compleja.

Comúnmente abreviado por KP (del inglés Knapsack problem) es un problema de optimización combinatoria que intenta encontrar la mejor solución entre un conjunto finito de posibles soluciones a un problema. Modela la situación de llenar una mochila que puede soportar un peso máximo, cada uno de los objetos introducidos en la mochila cuentan con un peso y valor específicos. Los objetos colocados en la mochila deben maximizar el valor total sin exceder el peso máximo.

## Sobre el uso de IA en este proyecto

Este es un **proyecto académico de investigación**. Su objetivo es ejercitar el cerebro: entender, diseñar e implementar metaheurísticas a mano, equivocarse y corregir.

- **Los algoritmos** (paquete `algorithms`) se escriben **sin herramientas de IA generativa**. Nada de generación, corrección ni sugerencias de código.
- **El ecosistema** que los rodea, es decir, el runner de experimentos, las métricas, los baselines (voraz y exacto), la generación de instancias, la documentación, el CI y la arquitectura, se desarrolla con ayuda de [Claude](https://claude.com/claude-code).

Sabemos que una IA nos superaría con creces implementando estos algoritmos. Justo por eso no se usa ahí: el valor está en el proceso de pensarlos, no en tenerlos hechos.

## Formulación matemática
![image](https://user-images.githubusercontent.com/10134910/172630156-2a734b26-367c-4206-9976-a511c33a3057.png)

## Ejecutando el proyecto
Requiere **Java 25**. Desde la raíz del repositorio:

```bash
./mvnw verify               # compila y ejecuta los tests
./mvnw compile exec:java    # ejecuta todos los algoritmos sobre instances/legacy-20.json
./mvnw compile exec:java -Dexec.args="--instance=instances/legacy-100.json --seed=7 --max-seconds=5"
```

Opciones: `--instance`, `--seed`, `--max-evaluations` y `--max-seconds` (60 s por defecto, por algoritmo).

## Estructura
| Módulo | Contenido | Autoría |
|---|---|---|
| `knapsack-core` | Modelo del problema, formato de instancia y contrato `KnapsackSolver` | Ecosistema |
| `knapsack-algorithms` | Implementaciones metaheurísticas | Humana, sin IA |
| `knapsack-bench` | Ejecución, validación y comparación | Ecosistema |
| `instances/` | Instancias del problema en JSON | — |

## Formato de instancia
```json
{
  "name": "ejemplo",
  "capacity": 10,
  "optimum": 9,
  "items": [ { "profit": 9, "weight": 7 }, { "profit": 4, "weight": 5 } ]
}
```
`optimum` es opcional. Las instancias `legacy-*` son las del proyecto original, con la capacidad de 100 que usaba su `main`.

## Añadir un algoritmo
1. Implementa `org.dmorfav.knapsack.core.solver.KnapsackSolver` en `knapsack-algorithms`, con un constructor público sin argumentos.
2. Regístralo añadiendo su nombre completo a `knapsack-algorithms/src/main/resources/META-INF/services/org.dmorfav.knapsack.core.solver.KnapsackSolver`.
3. Respeta el contrato del `SolverContext`:
   - toda la aleatoriedad sale de `context.random()`, para que la semilla haga reproducible la ejecución;
   - cada solución se evalúa con `context.evaluate(...)`, que cuenta evaluaciones;
   - el algoritmo termina en cuanto `context.budgetExhausted()` es cierto.

`SolverInfo` identifica la implementación como `familia/variante` (p. ej. `harmony-search/improved-v2`), así que puede haber varias implementaciones del mismo algoritmo. Los algoritmos originales se ejecutan sin modificar a través de adaptadores en `knapsack-bench` y aparecen con la variante `legacy`. Como usan `Math.random`, sus resultados no son reproducibles.
