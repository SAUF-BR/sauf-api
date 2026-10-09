package br.com.sauf_api.area;

import java.util.UUID;

public record AreaResposta(UUID id, String codigoCine, String nome, long totalCursos) {}
