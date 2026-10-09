package br.com.sauf_api.comum;

import java.util.Map;

public record ErroResposta(int status, String error, String message, Map<String, String> fields) {}
