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
| `instances/legacy-*` | Las del proyecto original, con la capacidad de 100 que usaba su programa principal |
| `instances/pisinger/` | Una instancia de cada clase de Pisinger, con 100 objetos y $R = 1000$ |

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
