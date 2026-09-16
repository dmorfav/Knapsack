# Uso de IA

Este es un **proyecto académico de investigación**. Su objetivo es ejercitar el cerebro: entender, diseñar e implementar metaheurísticas a mano, equivocarse y corregir.

- **Los algoritmos** (`knapsack-algorithms`) se escriben **sin herramientas de IA generativa**. Nada de generación, corrección ni sugerencias de código.
- **El ecosistema** que los rodea se desarrolla con ayuda de [Claude](https://claude.com/claude-code): el contrato, las referencias, la generación de instancias, el runner de experimentos, la estadística, esta documentación, el CI y la arquitectura.

Sabemos que una IA nos superaría con creces implementando estos algoritmos. Justo por eso no se usa ahí: el valor está en el proceso de pensarlos, no en tenerlos hechos.

## Qué se garantiza y qué no

| Medida | Qué cubre |
|---|---|
| Hook de Claude Code (`.claude/hooks/guard.sh`) | Impide que Claude escriba en `knapsack-algorithms/src` y que haga commits, push o PRs en este repositorio |
| `CLAUDE.md` | Deja por escrito la regla para cualquier sesión de Claude Code |
| Revisión del autor | Todo cambio entra por un commit del autor, y `CODEOWNERS` exige su aprobación en cada PR |
| Plantilla y workflow de PR | Una PR de algoritmo solo toca `knapsack-algorithms/src` y lleva marcada la declaración de autoría |
| `ArchitectureTest` y `SolverContractTest` | Reglas y contrato que hacen justa la comparación; ver [Contribuir](guia/contribuir.md) |

Ninguna herramienta detecta de forma fiable código generado con IA. La autoría humana de los algoritmos de colaboradores se basa en su declaración y en la revisión, no en una comprobación automática.

Cuando el banco de pruebas descubre un problema en un algoritmo, lo describe en texto; la corrección la decide y la escribe su autor.
