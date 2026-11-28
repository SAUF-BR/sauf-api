package br.com.sauf_api.notificacao;

import java.util.List;

/** Dias de antecedência em que o aviso sai. Da mais larga para a mais próxima. */
public enum JanelaAviso {
    SETE_DIAS(7),
    TRES_DIAS(3),
    UM_DIA(1),
    NO_DIA(0);

    public static final List<JanelaAviso> TODAS = List.of(values());

    private final int dias;

    JanelaAviso(int dias) {
        this.dias = dias;
    }

    public int dias() {
        return dias;
    }
}
