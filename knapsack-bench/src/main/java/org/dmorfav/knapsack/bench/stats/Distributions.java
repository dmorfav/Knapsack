package org.dmorfav.knapsack.bench.stats;

/** Colas de las distribuciones normal y chi-cuadrado, sin dependencias externas. */
final class Distributions {

    private static final double EPSILON = 1e-15;
    private static final int MAX_ITERATIONS = 10_000;

    private Distributions() {
    }

    /** P(Z > z) para una normal estándar. */
    static double normalSurvival(double z) {
        return 0.5 * erfc(z / Math.sqrt(2));
    }

    /** P(X > x) para una chi-cuadrado con {@code degreesOfFreedom} grados de libertad. */
    static double chiSquareSurvival(double x, double degreesOfFreedom) {
        if (x <= 0) {
            return 1;
        }
        return regularizedGammaQ(degreesOfFreedom / 2, x / 2);
    }

    /** ln Γ(x) para x > 0: desplaza con Γ(x) = Γ(x + 1) / x hasta x ≥ 15 y aplica la serie de Stirling. */
    static double logGamma(double x) {
        if (x <= 0) {
            throw new IllegalArgumentException("logGamma requiere x > 0, recibió " + x);
        }
        double shift = 0;
        while (x < 15) {
            shift += Math.log(x);
            x += 1;
        }
        double inverse = 1 / x;
        double inverseSquared = inverse * inverse;
        double series = inverse * (1.0 / 12 - inverseSquared * (1.0 / 360 - inverseSquared * (1.0 / 1260
                - inverseSquared * (1.0 / 1680 - inverseSquared * (1.0 / 1188)))));
        return (x - 0.5) * Math.log(x) - x + 0.5 * Math.log(2 * Math.PI) + series - shift;
    }

    /** Q(a, x) = Γ(a, x) / Γ(a): serie para x < a + 1 y fracción continua de Lentz en otro caso. */
    static double regularizedGammaQ(double a, double x) {
        if (x < a + 1) {
            return 1 - lowerSeries(a, x);
        }
        double b = x + 1 - a;
        double c = 1 / Double.MIN_NORMAL;
        double d = 1 / b;
        double h = d;
        for (int i = 1; i <= MAX_ITERATIONS; i++) {
            double an = -i * (i - a);
            b += 2;
            d = an * d + b;
            if (Math.abs(d) < Double.MIN_NORMAL) {
                d = Double.MIN_NORMAL;
            }
            c = b + an / c;
            if (Math.abs(c) < Double.MIN_NORMAL) {
                c = Double.MIN_NORMAL;
            }
            d = 1 / d;
            double delta = d * c;
            h *= delta;
            if (Math.abs(delta - 1) < EPSILON) {
                break;
            }
        }
        return Math.exp(-x + a * Math.log(x) - logGamma(a)) * h;
    }

    private static double lowerSeries(double a, double x) {
        double term = 1 / a;
        double sum = term;
        for (int n = 1; n <= MAX_ITERATIONS; n++) {
            term *= x / (a + n);
            sum += term;
            if (Math.abs(term) < Math.abs(sum) * EPSILON) {
                break;
            }
        }
        return sum * Math.exp(-x + a * Math.log(x) - logGamma(a));
    }

    /** erfc(x) = Q(1/2, x²) para x ≥ 0, con la simetría erfc(−x) = 2 − erfc(x). */
    private static double erfc(double x) {
        if (x < 0) {
            return 2 - erfc(-x);
        }
        return x == 0 ? 1 : regularizedGammaQ(0.5, x * x);
    }
}
