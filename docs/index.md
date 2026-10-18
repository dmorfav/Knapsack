# Knapsack

Banco de pruebas de metaheurísticas para el [problema de la mochila](https://en.wikipedia.org/wiki/Knapsack_problem) 0/1. Es un **proyecto académico de investigación**: los algoritmos se escriben a mano, sin IA generativa, y todo lo que los rodea sirve para medirlos con rigor. Lo explica [Uso de IA](uso-de-ia.md).

## El problema

El problema de la mochila es uno de los 21 problemas NP-completos de Richard Karp (1972). Dados $n$ objetos, cada uno con un beneficio $p_i$ y un peso $w_i$, y una mochila de capacidad $C$, se trata de elegir qué objetos meter para maximizar el beneficio sin superar la capacidad:

$$
\max \sum_{i=1}^{n} p_i x_i \quad \text{sujeto a} \quad \sum_{i=1}^{n} w_i x_i \le C, \qquad x_i \in \{0, 1\}
$$

La formulación es sencilla, pero resolverlo no lo es. Por eso es un buen terreno para comparar metaheurísticas.

## Qué ofrece el banco de pruebas

- **Un contrato común** para cualquier algoritmo, con aleatoriedad reproducible por semilla, conteo de evaluaciones y presupuesto de evaluaciones y de tiempo. Admite varias implementaciones del mismo algoritmo.
- **Referencias para medir:** un voraz, el óptimo exacto por programación dinámica y la cota de Dantzig, con las que se calcula el **GAP**.
- **Instancias reproducibles** con las distribuciones de Pisinger y el óptimo verificado en CI.
- **Experimentos** definidos en YAML que guardan cada ejecución, su curva de convergencia y el entorno en que corrió.
- **Estadística comparativa:** Friedman con Nemenyi, Wilcoxon con corrección de Holm y perfiles de rendimiento.
- **[Resultados](resultados/index.md)** publicados en esta web, con su [evolución](resultados/evolucion/index.md) entre ejecuciones.

## Por dónde empezar

- [Ejecutar el proyecto](guia/ejecutar.md)
- [Añadir un algoritmo](guia/anadir-un-algoritmo.md)
- [Cómo se mide](metodologia/experimentos.md)
- [Arquitectura](arquitectura.md)

## Licencia y cita

El código se distribuye bajo la [GNU GPL v3 o posterior](https://github.com/dmorfav/Knapsack/blob/main/LICENSE): puedes usarlo, modificarlo y redistribuirlo, siempre que lo que distribuyas a partir de él mantenga la misma licencia y su código fuente.

Si usas el banco de pruebas o sus resultados en un trabajo académico, cítalo con los metadatos de [`CITATION.cff`](https://github.com/dmorfav/Knapsack/blob/main/CITATION.cff). GitHub los ofrece en BibTeX y APA con el botón *Cite this repository*.
