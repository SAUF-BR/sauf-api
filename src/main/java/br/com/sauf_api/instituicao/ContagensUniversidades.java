package br.com.sauf_api.instituicao;

import java.util.Map;

public record ContagensUniversidades(Map<String, Long> tipo, Map<String, Long> modalidade) {}