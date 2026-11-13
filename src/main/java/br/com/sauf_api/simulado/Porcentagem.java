package br.com.sauf_api.simulado;

final class Porcentagem {

    private Porcentagem() {}

    /** Parte do total em %, com 1 casa decimal. */
    static double de(int parte, int total) {
        return total == 0 ? 0 : Math.round(parte * 1000.0 / total) / 10.0;
    }
}
