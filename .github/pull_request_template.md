## Qué cambia

<!-- Una o dos frases. Si añades un algoritmo: familia/variante y la idea principal. -->

## Tipo

<!-- Una PR es de un solo tipo: el workflow "Pull request" falla si mezcla knapsack-algorithms/src con el resto. -->

- [ ] Algoritmo (solo `knapsack-algorithms/src`)
- [ ] Ecosistema (core, baselines, bench, instancias, CI, documentación)

## Declaración de autoría

<!-- Obligatoria si la PR toca knapsack-algorithms/src: el workflow comprueba que está marcada. -->

- [ ] Declaro que el código de `knapsack-algorithms` de esta PR lo he escrito yo, sin herramientas de IA generativa (generación, autocompletado con IA, corrección ni sugerencias de código).

## Comprobaciones

- [ ] `./mvnw verify` pasa en local, incluidos `ArchitectureTest` y `SolverContractTest`.
- [ ] Si cambia el comportamiento de una variante con resultados publicados, he creado una variante nueva en lugar de modificarla.
