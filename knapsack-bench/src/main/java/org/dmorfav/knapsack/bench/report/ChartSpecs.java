package org.dmorfav.knapsack.bench.report;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Especificaciones Vega-Lite con los datos incluidos; la página las carga con vega-embed. */
final class ChartSpecs {

    private static final String SCHEMA = "https://vega.github.io/schema/vega-lite/v6.json";

    private ChartSpecs() {
    }

    /** {@code rows}: {solver, gap} por ejecución correcta. */
    static Map<String, Object> gapBoxplot(String title, List<Map<String, Object>> rows) {
        return obj(
                "$schema", SCHEMA,
                "title", title,
                "width", "container",
                "data", obj("values", rows),
                "mark", obj("type", "boxplot", "extent", "min-max"),
                "encoding", obj(
                        "y", obj("field", "solver", "type", "nominal", "title", null,
                                "sort", obj("op", "median", "field", "gap")),
                        "x", obj("field", "gap", "type", "quantitative", "title", "GAP (%)",
                                "scale", obj("zero", true)),
                        "color", obj("field", "solver", "type", "nominal", "legend", null)));
    }

    /** {@code rows}: {solver, evaluations, gap, runs} por punto de control. */
    static Map<String, Object> convergence(String title, List<Map<String, Object>> rows) {
        return obj(
                "$schema", SCHEMA,
                "title", title,
                "width", "container",
                "height", 280,
                "data", obj("values", rows),
                "mark", obj("type", "line", "point", true, "interpolate", "step-after"),
                "encoding", obj(
                        "x", obj("field", "evaluations", "type", "quantitative", "title", "Evaluaciones",
                                "scale", obj("type", "log")),
                        "y", obj("field", "gap", "type", "quantitative", "title", "GAP mediano (%)"),
                        "color", obj("field", "solver", "type", "nominal", "title", "Solver"),
                        "tooltip", List.of(
                                obj("field", "solver", "type", "nominal"),
                                obj("field", "evaluations", "type", "quantitative", "title", "Evaluaciones"),
                                obj("field", "gap", "type", "quantitative", "title", "GAP (%)", "format", ".2f"),
                                obj("field", "runs", "type", "quantitative", "title", "Ejecuciones"))));
    }

    /** {@code rows}: {solver, tau, fraction}. */
    static Map<String, Object> performanceProfile(List<Map<String, Object>> rows) {
        return obj(
                "$schema", SCHEMA,
                "title", "Perfil de rendimiento (Dolan-Moré)",
                "width", "container",
                "height", 280,
                "data", obj("values", rows),
                "mark", obj("type", "line", "interpolate", "step-after", "point", true),
                "encoding", obj(
                        "x", obj("field", "tau", "type", "quantitative", "title", "τ (mejor beneficio / beneficio)",
                                "scale", obj("type", "log")),
                        "y", obj("field", "fraction", "type", "quantitative", "title", "Fracción de instancias",
                                "scale", obj("domain", List.of(0, 1))),
                        "color", obj("field", "solver", "type", "nominal", "title", "Solver")));
    }

    /** {@code rows}: {date, solver, gap, suite}. */
    static Map<String, Object> evolution(String title, List<Map<String, Object>> rows) {
        return obj(
                "$schema", SCHEMA,
                "title", title,
                "width", "container",
                "height", 300,
                "data", obj("values", rows),
                "mark", obj("type", "line", "point", true),
                "encoding", obj(
                        "x", obj("field", "date", "type", "temporal", "title", "Ejecución"),
                        "y", obj("field", "gap", "type", "quantitative", "title", "Media de GAP medianos (%)"),
                        "color", obj("field", "solver", "type", "nominal", "title", "Solver"),
                        "shape", obj("field", "suite", "type", "nominal", "title", "Conjunto de instancias"),
                        "tooltip", List.of(
                                obj("field", "solver", "type", "nominal"),
                                obj("field", "date", "type", "temporal", "title", "Fecha", "format", "%Y-%m-%d %H:%M"),
                                obj("field", "gap", "type", "quantitative", "title", "GAP (%)", "format", ".2f"),
                                obj("field", "suite", "type", "nominal", "title", "Conjunto"))));
    }

    /** Mapa ordenado que admite null: Vega-Lite usa null para ocultar títulos y leyendas. */
    static Map<String, Object> obj(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }
}
