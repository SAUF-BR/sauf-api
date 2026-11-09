package br.com.sauf_api.questao;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** null no Java = '' no banco, para o UNIQUE (prova_id, numero, lingua) valer também sem língua. */
@Converter
public class LinguaConverter implements AttributeConverter<Lingua, String> {

    @Override
    public String convertToDatabaseColumn(Lingua lingua) {
        return lingua == null ? "" : lingua.name();
    }

    @Override
    public Lingua convertToEntityAttribute(String valor) {
        return valor == null || valor.isEmpty() ? null : Lingua.valueOf(valor);
    }
}
