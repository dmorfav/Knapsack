# Instancias

## Formato

```json
{
  "name": "ejemplo",
  "capacity": 10,
  "optimum": 9,
  "items": [ { "profit": 9, "weight": 7 }, { "profit": 4, "weight": 5 } ]
}
```

Beneficios, pesos y capacidad son enteros no negativos. `optimum` es opcional. Un campo desconocido o ausente hace que la instancia se rechace, para que una errata no pase desapercibida.

Los óptimos declarados nunca se escriben a mano. Salen de `generate` o de `optimum --write`, y el CI los recalcula todos en cada build: si uno no coincide, el build falla.

## Instancias incluidas

| Carpeta | Contenido |
|---|---|
| `instances/suite-v1/` | La [suite v1](#suite-v1): 54 instancias de Pisinger con óptimo, sobre la que corre el experimento semanal |
| `instances/pisinger/` | Una instancia de cada clase de Pisinger, con 100 objetos y $R = 1000$, para el experimento rápido `smoke` |
| `instances/legacy-*` | Las del proyecto original, con la capacidad de 100 que usaba su programa principal |

## Clases de Pisinger

Siguen a D. Pisinger, *Where are the hard knapsack problems?*, Computers & Operations Research, 2005. $U[a, b]$ es un entero uniforme en $[a, b]$ y $R$ el rango de coeficientes:

| Clase | Pesos | Beneficios |
|---|---|---|
| `uncorrelated` | $U[1, R]$ | $U[1, R]$ |
| `weakly-correlated` | $U[1, R]$ | $U[\max(1, w - R/10),\; w + R/10]$ |
| `strongly-correlated` | $U[1, R]$ | $w + R/10$ |
| `inverse-strongly-correlated` | $p + R/10$ | $U[1, R]$ |
| `almost-strongly-correlated` | $U[1, R]$ | $U[w + R/10 - R/500,\; w + R/10 + R/500]$ |
| `subset-sum` | $U[1, R]$ | $w$ |
| `uncorrelated-similar-weights` | $U[100000, 100100]$ | $U[1, 1000]$ |

La capacidad de la instancia $h$ de una serie de $H$ es $C = \left\lfloor \dfrac{h \cdot \sum w_i}{H + 1} \right\rfloor$. Por defecto, $h = 50$ y $H = 100$, que da algo menos de la mitad del peso total.

El generador es determinista por semilla. Reproduce las distribuciones de Pisinger, pero no sus ficheros publicados, porque usa otro generador aleatorio.

```bash
./mvnw -q compile exec:java -Dexec.args="generate --class=weakly-correlated --items=500 --range=10000 --seed=4"
```

La instancia se guarda en `instances/pisinger/<nombre>.json` con su óptimo, si la programación dinámica es viable para ella (ver [Referencias](referencias.md)).

## Suites

Una suite es un conjunto de instancias definido en YAML como producto cartesiano de parámetros del generador. Sus ficheros son siempre la salida del generador para esa definición:

```bash
./mvnw -q compile exec:java -Dexec.args="suite instances/suite-v1/suite.yaml"          # genera junto al YAML
./mvnw -q compile exec:java -Dexec.args="suite --check instances/suite-v1/suite.yaml"  # lo que ejecuta el CI
```

`--check` falla si falta una instancia, si alguna no coincide con la salida del generador (por ejemplo, porque alguien la editó a mano) o si sobra algún `.json` en la carpeta. `suite` exige óptimo exacto en todas las instancias: una combinación en la que la programación dinámica no es viable hace fallar la generación.

### Suite v1

| Factor | Niveles |
|---|---|
| Clase | `uncorrelated`, `weakly-correlated`, `strongly-correlated`, `inverse-strongly-correlated`, `almost-strongly-correlated`, `subset-sum` |
| Objetos $n$ | 50, 200 y 1000 |
| Capacidad | $h \in \{25, 50, 75\}$ de $H = 100$: aproximadamente el 25 %, el 50 % y el 75 % del peso total |
| Rango $R$ | 1000 |
| Semilla | 1 |

Son $6 \times 3 \times 3 = 54$ instancias. Las decisiones de diseño:

- **Factorial completo.** Cada combinación de clase, tamaño y capacidad aparece una vez, así que el efecto de cada factor se puede analizar por separado.
- **Óptimo en todas.** El GAP se mide siempre contra el óptimo y no contra una cota, y el solver exacto no falla en ninguna instancia.
- **Sin `uncorrelated-similar-weights`.** Con pesos cercanos a 100 000, la programación dinámica solo es viable con unos 50 objetos; incluir la clase rompería el diseño factorial o el requisito de óptimo.
- **Potencia.** Con 54 bloques, la prueba de Wilcoxon puede detectar diferencias entre pares tras el ajuste de Holm, lo que con 8 instancias era imposible.
- **Límite de tamaño.** $R = 1000$ y $n \le 1000$ mantienen el óptimo por debajo de un segundo por instancia.
