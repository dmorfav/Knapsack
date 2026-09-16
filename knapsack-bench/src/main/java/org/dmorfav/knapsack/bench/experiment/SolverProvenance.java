package org.dmorfav.knapsack.bench.experiment;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.ExceptionCatch;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * De qué código sale un solver. Se recorre el bytecode desde su clase siguiendo las clases que usa de verdad
 * (herencia, campos, firmas e instrucciones; no las clases anidadas vecinas) y se quedan las del proyecto.
 *
 * <p>{@code codeHash} es el SHA-256 de esos ficheros fuente, así que no cambia con el compilador; una clase sin
 * fuente localizable (por ejemplo, desde un jar) entra con su bytecode y aparece en {@code sources} como
 * {@code bytecode:<clase>}. {@code gitAuthors} son los autores en git de las fuentes propias del solver, sin
 * knapsack-core ni knapsack-bench, y {@code uncommittedChanges} indica si alguna tenía cambios sin commitear.
 * Los campos de git quedan en null si git no está disponible.
 */
public record SolverProvenance(String codeHash, List<String> sources, List<String> gitAuthors,
                               Boolean uncommittedChanges) {

    private static final String PROJECT_JAR_PREFIX = "knapsack-";
    private static final Set<String> FRAMEWORK_MODULES = Set.of("knapsack-core/", "knapsack-bench/");

    public static SolverProvenance of(Class<?> solverClass, Path root) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        TreeMap<String, byte[]> contents = new TreeMap<>();
        Deque<String> pending = new ArrayDeque<>(List.of(internalName(solverClass.getName())));
        Set<String> visited = new TreeSet<>();
        ClassLoader loader = solverClass.getClassLoader();
        while (!pending.isEmpty()) {
            String name = pending.pop();
            if (!visited.add(name)) {
                continue;
            }
            URL url = loader == null ? null : loader.getResource(name + ".class");
            if (url == null || !isProjectClass(url)) {
                continue;
            }
            byte[] bytecode = read(url);
            addContent(contents, name, url, bytecode, normalizedRoot);
            referencedClasses(bytecode, pending::push);
        }

        List<String> sources = List.copyOf(contents.keySet());
        List<String> ownSources = sources.stream()
                .filter(source -> !source.startsWith("bytecode:"))
                .filter(source -> FRAMEWORK_MODULES.stream().noneMatch(source::startsWith))
                .toList();
        return new SolverProvenance(hash(contents), sources, gitAuthors(normalizedRoot, ownSources),
                uncommittedChanges(normalizedRoot, ownSources));
    }

    /** SHA-256 de cada entrada (nombre y contenido), en orden de nombre. Los finales de línea se normalizan. */
    static String hash(TreeMap<String, byte[]> contents) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            contents.forEach((name, content) -> {
                digest.update(name.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                byte[] normalized = name.startsWith("bytecode:") ? content
                        : new String(content, StandardCharsets.UTF_8).replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8);
                digest.update(normalized);
                digest.update((byte) 0);
            });
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("la JVM no ofrece SHA-256", e);
        }
    }

    private static boolean isProjectClass(URL url) {
        String location = url.toString();
        return switch (url.getProtocol()) {
            case "file" -> location.contains("/target/classes/") || location.contains("/target/test-classes/");
            case "jar" -> {
                String jar = location.substring(0, location.indexOf("!/"));
                yield jar.substring(jar.lastIndexOf('/') + 1).startsWith(PROJECT_JAR_PREFIX);
            }
            default -> false;
        };
    }

    /** Fuente en {@code <módulo>/src/main/java} o {@code src/test/java}; si no existe, el bytecode. */
    private static void addContent(TreeMap<String, byte[]> contents, String name, URL url, byte[] bytecode, Path root) {
        Path source = sourceFile(name, url, root);
        if (source != null && Files.isRegularFile(source) && source.startsWith(root)) {
            String relative = root.relativize(source).toString().replace('\\', '/');
            if (!contents.containsKey(relative)) {
                try {
                    contents.put(relative, Files.readAllBytes(source));
                } catch (IOException e) {
                    throw new UncheckedIOException("no se pudo leer " + source, e);
                }
            }
        } else {
            contents.put("bytecode:" + name.replace('/', '.'), bytecode);
        }
    }

    /** Desde target/classes o target/test-classes del módulo, o desde un jar {@code <módulo>-<versión>.jar} del repo. */
    private static Path sourceFile(String name, URL url, Path root) {
        String outer = (name.contains("$") ? name.substring(0, name.indexOf('$')) : name) + ".java";
        String location = url.toString();
        try {
            if (url.getProtocol().equals("jar")) {
                String jar = location.substring(location.lastIndexOf('/', location.indexOf("!/")) + 1, location.indexOf("!/"));
                String module = jar.substring(0, jar.lastIndexOf('-'));
                return root.resolve(module).resolve("src/main/java").resolve(outer).normalize();
            }
            boolean test = location.contains("/target/test-classes/");
            String marker = test ? "/target/test-classes/" : "/target/classes/";
            Path module = Path.of(URI.create(location.substring(0, location.indexOf(marker)) + "/"));
            return module.resolve(test ? "src/test/java" : "src/main/java").resolve(outer).normalize();
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    private static void referencedClasses(byte[] bytecode, Consumer<String> sink) {
        ClassModel model = ClassFile.of().parse(bytecode);
        Consumer<ClassDesc> desc = type -> {
            ClassDesc element = type;
            while (element.isArray()) {
                element = element.componentType();
            }
            if (element.isClassOrInterface()) {
                String descriptor = element.descriptorString();
                sink.accept(descriptor.substring(1, descriptor.length() - 1));
            }
        };
        Consumer<MethodTypeDesc> signature = type -> {
            desc.accept(type.returnType());
            type.parameterList().forEach(desc);
        };
        model.superclass().ifPresent(entry -> sink.accept(entry.asInternalName()));
        model.interfaces().forEach(entry -> sink.accept(entry.asInternalName()));
        for (FieldModel field : model.fields()) {
            desc.accept(field.fieldTypeSymbol());
        }
        for (MethodModel method : model.methods()) {
            signature.accept(method.methodTypeSymbol());
            method.code().ifPresent(code -> {
                for (CodeElement element : code) {
                    switch (element) {
                        case FieldInstruction field -> sink.accept(field.owner().asInternalName());
                        case InvokeInstruction invoke -> sink.accept(invoke.owner().asInternalName());
                        case NewObjectInstruction created -> sink.accept(created.className().asInternalName());
                        case NewReferenceArrayInstruction array -> desc.accept(array.componentType().asSymbol());
                        case TypeCheckInstruction check -> desc.accept(check.type().asSymbol());
                        case ExceptionCatch handler -> handler.catchType().ifPresent(type -> sink.accept(type.asInternalName()));
                        case ConstantInstruction constant when constant.constantValue() instanceof ClassDesc type ->
                                desc.accept(type);
                        case InvokeDynamicInstruction indy -> indy.bootstrapArgs().forEach(argument -> {
                            if (argument instanceof DirectMethodHandleDesc handle) {
                                desc.accept(handle.owner());
                            }
                        });
                        default -> {
                        }
                    }
                }
            });
        }
    }

    private static List<String> gitAuthors(Path root, List<String> sources) {
        if (sources.isEmpty()) {
            return List.of();
        }
        String output = git(root, sources, "log", "--format=%aN");
        return output == null ? null : List.copyOf(new TreeSet<>(output.lines().filter(line -> !line.isBlank()).toList()));
    }

    private static Boolean uncommittedChanges(Path root, List<String> sources) {
        if (sources.isEmpty()) {
            return false;
        }
        String output = git(root, sources, "status", "--porcelain");
        return output == null ? null : !output.isBlank();
    }

    private static String git(Path root, List<String> sources, String... arguments) {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(arguments));
        command.add("--");
        command.addAll(sources);
        try {
            Process process = new ProcessBuilder(command).directory(root.toFile())
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            process.getOutputStream().close();
            byte[] out = process.getInputStream().readAllBytes();
            if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
                process.destroyForcibly();
                return null;
            }
            return new String(out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static String internalName(String binaryName) {
        return binaryName.replace('.', '/');
    }

    private static byte[] read(URL url) {
        try (InputStream in = url.openStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("no se pudo leer " + url, e);
        }
    }
}
