package br.com.sauf_api.documento;

import java.util.Arrays;
import java.util.Comparator;

/** Compara versões no formato 1.0.0 (semver): 1.10.0 vem depois de 1.9.0. */
final class Versao {

    static final Comparator<String> ORDEM = (a, b) -> {
        int[] x = partes(a);
        int[] y = partes(b);
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int dif = Integer.compare(i < x.length ? x[i] : 0, i < y.length ? y[i] : 0);
            if (dif != 0) return dif;
        }
        return 0;
    };

    private Versao() {}

    static boolean valida(String versao) {
        return versao != null && versao.matches("\\d+(\\.\\d+){0,2}");
    }

    private static int[] partes(String versao) {
        return Arrays.stream(versao.split("\\.")).mapToInt(Integer::parseInt).toArray();
    }
}
