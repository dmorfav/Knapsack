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
./mvnw compile exec:java    # ejecuta todos los solvers sobre instances/legacy-20.json
```

La línea de comandos tiene tres subcomandos, que se pasan con `-Dexec.args="..."`:

| Subcomando | Qué hace | Ejemplo |
|---|---|---|
| `run` | Ejecuta una vez cada solver registrado y muestra beneficio, GAP, evaluaciones y tiempo | `run --instance=instances/pisinger/subset-sum-n100-r1000-h50of100-s1.json --seed=7 --max-seconds=5` |
| `generate` | Genera una instancia de Pisinger y calcula su óptimo | `generate --class=strongly-correlated --items=200 --range=1000 --seed=3` |
| `optimum` | Calcula el óptimo exacto de instancias existentes y lo contrasta con el declarado | `optimum --write instances/legacy-20.json` |

`help <subcomando>` muestra todas las opciones.

## Estructura
| Módulo | Contenido | Autoría |
|---|---|---|
| `knapsack-core` | Modelo del problema, formato de instancia, contrato `KnapsackSolver` y generador de instancias | Ecosistema |
| `knapsack-algorithms` | Implementaciones metaheurísticas | Humana, sin IA |
| `knapsack-baselines` | Referencias: voraz, óptimo exacto y cota superior | Ecosistema |
| `knapsack-bench` | Línea de comandos: ejecución, validación y comparación | Ecosistema |
| `instances/` | Instancias del problema en JSON | — |

## Referencias para medir
- **`greedy/ratio`**: recorre los objetos por ratio beneficio/peso decreciente y mete cada uno que quepa.
- **`greedy/ratio-best-item`**: el mejor entre el voraz y el objeto más valioso que cabe solo. Garantiza al menos la mitad del óptimo.
- **`exact/dynamic-programming`**: óptimo exacto en O(n·C). Solo es viable si n·(C+1) ≤ 10⁹.
- **Cota de Dantzig** (relajación lineal): ninguna solución la supera. Cuando no se conoce el óptimo, el GAP se da contra ella y se muestra como `gap ≤`.

El **GAP** es `(referencia − beneficio) / referencia`, donde la referencia es el óptimo conocido o, si falta, la cota.

## Instancias
- **`instances/legacy-*`**: las del proyecto original, con la capacidad de 100 que usaba su `main` y el óptimo ya calculado.
- **`instances/pisinger/`**: una instancia de cada clase de Pisinger (*Where are the hard knapsack problems?*, 2005), con 100 objetos y R = 1000. Clases: `uncorrelated`, `weakly-correlated`, `strongly-correlated`, `inverse-strongly-correlated`, `almost-strongly-correlated`, `subset-sum` y `uncorrelated-similar-weights`. La capacidad de la instancia h de una serie de H es `⌊h·Σw/(H+1)⌋`. Se reproducen las distribuciones, no los ficheros exactos de Pisinger, porque el generador aleatorio es otro.

## Formato de instancia
```json
{
  "name": "ejemplo",
  "capacity": 10,
  "optimum": 9,
  "items": [ { "profit": 9, "weight": 7 }, { "profit": 4, "weight": 5 } ]
}
```
`optimum` es opcional; si está presente, `run` marca como fallo cualquier beneficio que lo supere.

## Añadir un algoritmo
1. Implementa `org.dmorfav.knapsack.core.solver.KnapsackSolver` en `knapsack-algorithms`, con un constructor público sin argumentos.
2. Regístralo añadiendo su nombre completo a `knapsack-algorithms/src/main/resources/META-INF/services/org.dmorfav.knapsack.core.solver.KnapsackSolver`.
3. Respeta el contrato del `SolverContext`:
   - toda la aleatoriedad sale de `context.random()`, para que la semilla haga reproducible la ejecución;
   - cada solución se evalúa con `context.evaluate(...)`, que cuenta evaluaciones;
   - el algoritmo termina en cuanto `context.budgetExhausted()` es cierto.

`SolverInfo` identifica la implementación como `familia/variante` (p. ej. `harmony-search/improved-v2`), así que puede haber varias implementaciones del mismo algoritmo. Los algoritmos originales se ejecutan sin modificar a través de adaptadores en `knapsack-bench` y aparecen con la variante `legacy`. Como usan `Math.random`, sus resultados no son reproducibles.
