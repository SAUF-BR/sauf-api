package br.com.sauf_api.comum;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record Pagina<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> Pagina<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new Pagina<>(
                pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
