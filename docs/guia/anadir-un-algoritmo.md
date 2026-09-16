# Añadir un algoritmo

Un algoritmo es una implementación de `org.dmorfav.knapsack.core.solver.KnapsackSolver` dentro de `knapsack-algorithms`, el único módulo de [autoría humana](../uso-de-ia.md).

```java
public interface KnapsackSolver {
    SolverInfo info();
    Solution solve(Instance instance, SolverContext context);
}
```

## Pasos

1. **Implementa `KnapsackSolver`** en `knapsack-algorithms`, en una clase pública con constructor público sin argumentos. Por convención, en el paquete `org.dmorfav.knapsack.algorithms.<familia>`.
2. **Regístralo** añadiendo su nombre completo de clase, una por línea, a `knapsack-algorithms/src/main/resources/META-INF/services/org.dmorfav.knapsack.core.solver.KnapsackSolver`. El banco de pruebas lo descubre con `ServiceLoader`.
3. **Compruébalo** con `./mvnw verify`, que le aplica las [reglas de arquitectura y el contrato](contribuir.md#que-se-comprueba-automaticamente), y con `./mvnw -q compile exec:java`, que lo ejecuta junto a las referencias.

## Identidad: `SolverInfo`

`SolverInfo(family, variant, author)` identifica la implementación como `familia/variante`, por ejemplo `harmony-search/improved-v2`. Ambos campos van en kebab-case y en minúsculas. Varias variantes de la misma familia conviven y se comparan entre sí.

Si cambias el comportamiento de una implementación que ya tiene resultados publicados, crea una variante nueva: así los resultados antiguos siguen describiendo el código que los produjo.

## El contrato de `SolverContext`

| Regla | Por qué |
|---|---|
| Toda la aleatoriedad sale de `context.random()` | Con la misma semilla, la misma ejecución: los resultados se pueden reproducir |
| Cada solución se evalúa con `context.evaluate(solution)` | Cuenta las evaluaciones, que es la medida de esfuerzo independiente de la máquina, y registra la curva de convergencia |
| El algoritmo termina en cuanto `context.budgetExhausted()` es cierto | Todos compiten con el mismo presupuesto. Evaluar con el presupuesto agotado lanza `BudgetExhaustedException` y la ejecución cuenta como fallida |
| Devuelve una `Solution` de `instance.size()` posiciones y factible | El banco de pruebas marca como fallo una solución nula, de otro tamaño, no factible o con más beneficio que el óptimo conocido |

`context.evaluations()`, `context.elapsed()` y `context.budget()` permiten al algoritmo adaptar su comportamiento al presupuesto.

## Algoritmos originales

Los algoritmos del proyecto original no implementan el contrato. Se ejecutan sin modificar a través de adaptadores en `knapsack-bench`, con la variante `legacy`. Estas son sus diferencias con uno que sí lo implementa:

- **Evaluaciones:** los adaptadores cuentan como evaluación cada consulta al modelo antiguo (`evaluateOF`, `evaluateRestriction` y `totalWeight`).
- **Semilla:** usan `Math.random`, así que la ignoran y sus resultados no son reproducibles.
- **Presupuesto:** no lo consultan. Solo se detienen cuando una evaluación lo encuentra agotado.
