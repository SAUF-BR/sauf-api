package br.com.sauf_api.comum;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EnumComValorConverterFactory implements ConverterFactory<String, Enum> {

    @Override
    public <T extends Enum> Converter<String, T> getConverter(Class<T> tipo) {
        if (!EnumComValor.class.isAssignableFrom(tipo)) {
            return valor -> (T) Enum.valueOf(tipo, valor.trim());
        }
        return valor -> (T) EnumComValor.deValor((Class) tipo, valor.trim());
    }
}
