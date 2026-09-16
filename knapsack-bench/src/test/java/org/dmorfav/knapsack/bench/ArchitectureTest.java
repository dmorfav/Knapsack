package org.dmorfav.knapsack.bench;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.RunContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Reglas que todo algoritmo de knapsack-algorithms cumple para que las comparaciones sean justas y reproducibles.
 * Viven aquí porque knapsack-algorithms es autoría humana. Si una regla falla por un algoritmo, se corrige el
 * algoritmo, no la regla.
 */
class ArchitectureTest {

    /**
     * Algoritmos del proyecto original: usan Math.random, así que solo se eximen de las reglas de aleatoriedad y
     * reloj. Cuando su autor migre uno al contrato KnapsackSolver, sale de esta lista.
     */
    private static final Set<String> ORIGINAL_ALGORITHMS = Set.of(
            "algorithms.DiscreteBinaryHarmonySearch",
            "algorithms.HarmonySearch",
            "algorithms.HillClimbingBestAscent",
            "algorithms.HillClimbingFirstAscent",
            "algorithms.ImprovedHarmonySearch");

    private static final DescribedPredicate<JavaClass> NOT_ORIGINAL = DescribedPredicate.describe(
            "no son algoritmos originales",
            javaClass -> !ORIGINAL_ALGORITHMS.contains(javaClass.getName().replaceFirst("\\$.*", "")));

    private static JavaClasses algorithms;

    @BeforeAll
    static void importAlgorithms() {
        algorithms = new ClassFileImporter()
                .withImportOption(location -> location.contains("knapsack-algorithms"))
                .importClasspath();
    }

    @Test
    void importsTheAlgorithmsModuleAndOnlyIt() {
        assertThat(algorithms).as("clases importadas de knapsack-algorithms").isNotEmpty();
        assertThat(algorithms.stream().map(JavaClass::getPackageName))
                .noneMatch(name -> name.startsWith("org.dmorfav.knapsack.bench")
                        || name.startsWith("org.dmorfav.knapsack.baselines")
                        || name.startsWith("org.dmorfav.knapsack.core"));
    }

    @Test
    void originalAlgorithmsListIsUpToDate() {
        Set<String> imported = algorithms.stream().map(JavaClass::getName).collect(Collectors.toSet());

        assertThat(imported)
                .as("un algoritmo de ORIGINAL_ALGORITHMS ya no existe: sácalo de la lista")
                .containsAll(ORIGINAL_ALGORITHMS);
    }

    @Test
    void algorithmsSeeOnlyTheProblemAndTheContract() {
        check(noClasses().should().dependOnClassesThat().resideInAnyPackage(
                        "org.dmorfav.knapsack.bench..", "org.dmorfav.knapsack.baselines..",
                        "org.dmorfav.knapsack.core.io..", "org.dmorfav.knapsack.core.generator..",
                        "tools.jackson..", "com.fasterxml..", "picocli..")
                .orShould().dependOnClassesThat(belongToAnyOf(RunContext.class))
                .because("un algoritmo no puede ver el banco de pruebas, el óptimo exacto ni fabricarse su contexto"));
    }

    @Test
    void algorithmsEvaluateOnlyThroughTheContext() {
        check(noClasses().should().callMethod(Instance.class, "evaluate", Solution.class)
                .because("cada evaluación pasa por context.evaluate: es la medida de esfuerzo y el presupuesto"));
    }

    @Test
    void algorithmsDoNoInputOutputReflectionOrConcurrency() {
        check(noClasses().should().dependOnClassesThat().resideInAnyPackage(
                        "java.io..", "java.nio..", "java.net..", "java.lang.reflect..", "java.util.concurrent..",
                        "java.security..")
                .orShould().dependOnClassesThat(belongToAnyOf(Thread.class, ThreadLocal.class, Runtime.class,
                        ProcessBuilder.class))
                .orShould().callMethod(System.class, "exit", int.class)
                .because("los tiempos solo son comparables con un único hilo y sin E/S dentro del bucle"));
    }

    @Test
    void algorithmsTakeAllRandomnessFromTheContext() {
        check(randomnessRule(noClasses().that(NOT_ORIGINAL).should()).allowEmptyShould(true));
    }

    @Test
    void randomnessRuleDetectsTheOriginalAlgorithms() {
        // Canario: sin la excepción, las reglas tienen que ver el Math.random de los originales
        assertThatThrownBy(() -> check(randomnessRule(noClasses().should())))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Math.random()");
    }

    private static ArchRule randomnessRule(ClassesShould should) {
        return should.callMethod(Math.class, "random")
                .orShould().callMethod(StrictMath.class, "random")
                .orShould().dependOnClassesThat(belongToAnyOf(Random.class, SplittableRandom.class,
                        RandomGeneratorFactory.class))
                .orShould().callMethod(RandomGenerator.class, "getDefault")
                .orShould().callMethod(RandomGenerator.class, "of", String.class)
                .orShould().callMethod(Collections.class, "shuffle", List.class)
                .orShould().callMethod(UUID.class, "randomUUID")
                .because("toda la aleatoriedad sale de context.random(): con la misma semilla, la misma ejecución");
    }

    @Test
    void algorithmsReadTimeOnlyFromTheContext() {
        check(noClasses().that(NOT_ORIGINAL)
                .should().callMethod(System.class, "currentTimeMillis")
                .orShould().callMethod(System.class, "nanoTime")
                .orShould().callMethod(Instant.class, "now")
                .because("el tiempo transcurrido se consulta con context.elapsed()")
                .allowEmptyShould(true));
    }

    @Test
    void algorithmsKeepNoMutableStaticState() {
        check(fields().that().areStatic().should().beFinal().allowEmptyShould(true)
                .because("las ejecuciones comparten JVM: un campo estático mutable filtraría estado entre ellas"));
    }

    @Test
    void everySolverImplementationIsInstantiableAndRegistered() {
        Set<String> registered = SolverCatalog.discover().stream()
                .map(solver -> solver.getClass().getName())
                .collect(Collectors.toSet());

        List<JavaClass> implementations = algorithms.stream()
                .filter(javaClass -> javaClass.isAssignableTo(KnapsackSolver.class))
                .filter(javaClass -> !javaClass.isInterface() && !javaClass.getModifiers().contains(JavaModifier.ABSTRACT))
                .toList();

        assertThat(implementations).allSatisfy(javaClass -> {
            Class<?> type = javaClass.reflect();
            assertThat(Modifier.isPublic(type.getModifiers()))
                    .as("%s debe ser pública para que ServiceLoader la instancie", type.getName()).isTrue();
            assertThat(type.getConstructors())
                    .as("%s necesita un constructor público sin argumentos", type.getName())
                    .anyMatch(constructor -> constructor.getParameterCount() == 0);
            assertThat(registered)
                    .as("%s no está registrada en META-INF/services/%s: nunca se ejecutaría", type.getName(),
                            KnapsackSolver.class.getName())
                    .contains(type.getName());
        });
    }

    private static void check(ArchRule rule) {
        rule.check(algorithms);
    }
}
