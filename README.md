# Knapsack

[![codecov](https://codecov.io/gh/dmorfav/Knapsack/branch/main/graph/badge.svg?token=NTCYS1ATLB)](https://codecov.io/gh/dmorfav/Knapsack)
![Testing Suite](https://github.com/dmorfav/knapsack/actions/workflows/build.yml/badge.svg)
![Testing Suite](https://github.com/dmorfav/knapsack/actions/workflows/qa.yml/badge.svg)
[![Open Source? Siii!](https://badgen.net/badge/Open%20Source%20%3F/Siii%21/blue?icon=github)](https://github.com/dmorfav/Knapsack)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/dmorfav/Knapsack)

## Suite de prueba de algoritmos metaheurísticos basada en el problema de la [mochila](https://en.wikipedia.org/wiki/Knapsack_problem)

>El problema de la mochila es uno de los 21 problemas NP-completos de Richard Karp, establecidos por el informático teórico en un famoso artículo de 1972.1. Ha sido intensamente estudiado desde mediados del siglo XX y se hace referencia a él en el año 1897, en un artículo de George Mathews Ballard.2
Si bien la formulación del problema es sencilla, su resolución es más compleja.

Comúnmente abreviado por KP (del inglés Knapsack problem) es un problema de optimización combinatoria que intenta encontrar la mejor solución entre un conjunto finito de posibles soluciones a un problema. Modela la situación de llenar una mochila que puede soportar un peso máximo, cada uno de los objetos introducidos en la mochila cuentan con un peso y valor específicos. Los objetos colocados en la mochila deben maximizar el valor total sin exceder el peso máximo.

**Documentación y resultados: https://dmorfav.github.io/Knapsack/**

## Sobre el uso de IA en este proyecto

Este es un **proyecto académico de investigación**. Su objetivo es ejercitar el cerebro: entender, diseñar e implementar metaheurísticas a mano, equivocarse y corregir.

- **Los algoritmos** (módulo `knapsack-algorithms`) se escriben **sin herramientas de IA generativa**. Nada de generación, corrección ni sugerencias de código.
- **El ecosistema** que los rodea, es decir, el runner de experimentos, las métricas, los baselines (voraz y exacto), la generación de instancias, la documentación, el CI y la arquitectura, se desarrolla con ayuda de [Claude](https://claude.com/claude-code).

Sabemos que una IA nos superaría con creces implementando estos algoritmos. Justo por eso no se usa ahí: el valor está en el proceso de pensarlos, no en tenerlos hechos.

## Inicio rápido

Requiere **Java 25**. Desde la raíz del repositorio:

```bash
./mvnw verify                                                            # compila y ejecuta los tests
./mvnw compile exec:java                                                 # ejecuta todos los solvers sobre instances/legacy-20.json
./mvnw -q compile exec:java -Dexec.args="experiment experiments/smoke.yaml"   # experimento con estadística
```

| Módulo | Contenido | Autoría |
|---|---|---|
| `knapsack-core` | Modelo del problema, contrato `KnapsackSolver`, formato y generador de instancias | Ecosistema |
| `knapsack-algorithms` | Implementaciones metaheurísticas | Humana, sin IA |
| `knapsack-baselines` | Referencias: voraz, óptimo exacto y cota superior | Ecosistema |
| `knapsack-bench` | Línea de comandos, experimentos, estadística e informes | Ecosistema |

Todo lo demás, desde cómo añadir un algoritmo hasta cómo se mide y qué limitaciones tienen las métricas, está en la [documentación](https://dmorfav.github.io/Knapsack/).
