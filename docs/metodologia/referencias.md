# Referencias y GAP

Las referencias son solvers y cotas que no compiten: sirven para saber cómo de buenos son los algoritmos. Todas tienen autor `baseline`.

| Referencia | Qué hace | Garantía |
|---|---|---|
| `greedy/ratio` | Recorre los objetos por ratio $p_i / w_i$ decreciente y mete cada uno que quepa | Ninguna: puede quedar arbitrariamente lejos del óptimo |
| `greedy/ratio-best-item` | El mejor entre el voraz anterior y el objeto más valioso que cabe solo | Al menos la mitad del óptimo |
| `exact/dynamic-programming` | Programación dinámica sobre la capacidad, en $O(n \cdot C)$ | Óptimo exacto, si $n \cdot (C + 1) \le 10^9$ |
| Cota de Dantzig | Óptimo de la relajación lineal: el prefijo por ratio más la fracción del primer objeto que no cabe | Ningún beneficio 0/1 la supera |

Los ratios se comparan multiplicando en enteros de 128 bits, sin coma flotante, para que el orden sea exacto.

## GAP

$$
\text{GAP} = 100 \cdot \frac{\text{referencia} - \text{beneficio}}{\text{referencia}}
$$

La referencia es el **óptimo conocido** de la instancia. Si no se conoce, es la **cota de Dantzig**: entonces el GAP real es igual o menor, y se muestra con `≤`.

Un GAP del 0 % contra el óptimo significa que se alcanzó el óptimo. Un GAP del 100 % equivale a la mochila vacía.
