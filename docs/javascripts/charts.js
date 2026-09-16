// Dibuja cada <div class="vega-chart" data-src="..."> con vega-embed, también tras la navegación instantánea.
function renderVegaCharts() {
  const dark = document.body.getAttribute("data-md-color-scheme") === "slate";
  document.querySelectorAll(".vega-chart").forEach((element) => {
    if (element.dataset.renderedScheme === String(dark)) {
      return;
    }
    element.dataset.renderedScheme = String(dark);
    element.replaceChildren();
    vegaEmbed(element, element.dataset.src, {
      actions: false,
      renderer: "svg",
      theme: dark ? "dark" : undefined,
      config: { background: null, autosize: { type: "fit-x", contains: "padding" } },
    }).catch((error) => {
      element.textContent = "No se pudo dibujar la gráfica: " + error.message;
    });
  });
}

document$.subscribe(renderVegaCharts);
new MutationObserver(renderVegaCharts).observe(document.body, {
  attributes: true,
  attributeFilter: ["data-md-color-scheme"],
});
