package com.infoprodutos.api.course;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Gera slugs amigáveis para URL a partir de um título (ex.: "Curso de Java!" -> "curso-de-java"). */
public final class Slugifier {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-+|-+$)");

    private Slugifier() {}

    public static String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = NON_ALPHANUMERIC.matcher(normalized.toLowerCase()).replaceAll("-");
        return EDGE_HYPHENS.matcher(slug).replaceAll("");
    }
}
