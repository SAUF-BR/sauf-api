package br.com.sauf_api.comum;

import com.fasterxml.jackson.annotation.JsonValue;

public interface EnumComValor {

    @JsonValue
    String valor();

    static <E extends Enum<E> & EnumComValor> E deValor(Class<E> tipo, String valor) {
        for (E constante : tipo.getEnumConstants()) {
            if (constante.valor().equalsIgnoreCase(valor)) {
                return constante;
            }
        }
        throw new IllegalArgumentException("Valor inválido para " + tipo.getSimpleName() + ": " + valor);
    }
}
