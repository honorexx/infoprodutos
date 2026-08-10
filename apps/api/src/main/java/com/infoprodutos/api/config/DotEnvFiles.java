package com.infoprodutos.api.config;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Leitura simples de arquivos {@code .env} locais (sem dependência externa). */
public final class DotEnvFiles {

    private static final Logger log = LoggerFactory.getLogger(DotEnvFiles.class);

    private DotEnvFiles() {}

    public static Map<String, String> load() {
        Map<String, String> values = new LinkedHashMap<>();
        for (Path path : candidatePaths()) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                Map<String, String> fromFile = parseFile(path);
                if (fromFile.isEmpty()) {
                    continue;
                }
                int before = values.size();
                fromFile.forEach(values::putIfAbsent);
                if (values.size() > before) {
                    log.info(
                            "dotenv: carregado {} chave(s) de {} (MP_ACCESS_TOKEN={})",
                            values.size() - before,
                            path.toAbsolutePath().normalize(),
                            values.containsKey("MP_ACCESS_TOKEN") ? "sim" : "não");
                }
            } catch (IOException e) {
                log.debug("dotenv: falha ao ler {}: {}", path, e.toString());
            }
        }
        return values;
    }

    public static String get(String key) {
        String value = load().get(key);
        return value != null && !value.isBlank() ? value : null;
    }

    static List<Path> candidatePaths() {
        Set<Path> paths = new LinkedHashSet<>();
        Path cwd = Path.of("").toAbsolutePath().normalize();
        addAround(paths, cwd);
        Path walk = cwd;
        for (int i = 0; i < 6 && walk != null; i++) {
            addAround(paths, walk);
            walk = walk.getParent();
        }
        try {
            URL location = DotEnvFiles.class.getProtectionDomain().getCodeSource().getLocation();
            if (location != null) {
                Path classes = Path.of(URI.create(location.toString())).toAbsolutePath().normalize();
                Path cur = Files.isRegularFile(classes) ? classes.getParent() : classes;
                for (int i = 0; i < 8 && cur != null; i++) {
                    addAround(paths, cur);
                    cur = cur.getParent();
                }
            }
        } catch (Exception ignored) {
            // classpath opcional
        }
        return new ArrayList<>(paths);
    }

    private static void addAround(Set<Path> paths, Path base) {
        paths.add(base.resolve(".env"));
        paths.add(base.resolve("apps/api/.env"));
        if (base.getFileName() != null && "api".equals(base.getFileName().toString())) {
            paths.add(base.resolve(".env"));
        }
    }

    private static Map<String, String> parseFile(Path path) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        for (String raw : Files.readAllLines(path)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                continue;
            }
            int eq = line.indexOf('=');
            String key = line.substring(0, eq).trim();
            if (key.isEmpty() || values.containsKey(key)) {
                continue;
            }
            String value = stripQuotes(line.substring(eq + 1).trim()).trim();
            if (!value.isBlank()) {
                values.put(key, value);
            }
        }
        return values;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
