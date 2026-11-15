package br.com.sauf_api.documento;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Um arquivo .md de documento legal (D-93), com o cabeçalho no topo:
 * <pre>
 * ---
 * tipo: TERMOS_USO
 * versao: 1.0.0
 * vigente_a_partir_de: "2026-11-15"
 * exige_novo_aceite: true
 * resumo_alteracoes: "O que mudou desta versão para a anterior" (opcional)
 * ---
 * # Título
 * ...
 * </pre>
 * O conteúdo guardado e o hash são só o texto depois do cabeçalho.
 */
record ArquivoDocumento(
        String nomeArquivo,
        TipoDocumento tipo,
        String versao,
        LocalDate vigenteAPartirDe,
        boolean exigeNovoAceite,
        String resumoAlteracoes,
        String titulo,
        String conteudo,
        String sha256) {

    static ArquivoDocumento ler(String nomeArquivo, String texto) {
        String normalizado = texto.replace("\r\n", "\n").replace("\r", "\n");
        if (normalizado.startsWith("﻿")) normalizado = normalizado.substring(1);
        if (!normalizado.startsWith("---\n")) {
            throw erro(nomeArquivo, "falta o cabeçalho entre --- no topo");
        }
        int fim = normalizado.indexOf("\n---\n", 4);
        if (fim < 0) throw erro(nomeArquivo, "o cabeçalho não foi fechado com ---");

        Map<String, String> cabecalho = cabecalho(normalizado.substring(4, fim));
        String conteudo = normalizado.substring(fim + 5).strip() + "\n";

        TipoDocumento tipo;
        try {
            tipo = TipoDocumento.valueOf(obrigatorio(cabecalho, "tipo", nomeArquivo).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw erro(nomeArquivo, "tipo deve ser TERMOS_USO ou POLITICA_PRIVACIDADE");
        }
        String versao = obrigatorio(cabecalho, "versao", nomeArquivo);
        if (!Versao.valida(versao)) throw erro(nomeArquivo, "versao deve ser como 1.0.0");

        LocalDate vigente;
        try {
            vigente = LocalDate.parse(obrigatorio(cabecalho, "vigente_a_partir_de", nomeArquivo));
        } catch (DateTimeParseException e) {
            throw erro(nomeArquivo, "vigente_a_partir_de deve ser uma data como 2026-11-15");
        }
        boolean exigeNovoAceite = !"false".equalsIgnoreCase(cabecalho.getOrDefault("exige_novo_aceite", "true"));
        String resumo = cabecalho.get("resumo_alteracoes");

        String titulo = conteudo.lines()
                .filter(linha -> linha.startsWith("# "))
                .map(linha -> linha.substring(2).strip())
                .findFirst()
                .orElseThrow(() -> erro(nomeArquivo, "falta o título (linha começando com \"# \")"));

        return new ArquivoDocumento(
                nomeArquivo, tipo, versao, vigente, exigeNovoAceite, resumo, titulo, conteudo, sha256(conteudo));
    }

    // Só "chave: valor" de uma linha; listas (ex.: finalidades) são ignoradas
    private static Map<String, String> cabecalho(String bloco) {
        Map<String, String> valores = new HashMap<>();
        for (String linha : bloco.split("\n")) {
            int doisPontos = linha.indexOf(':');
            if (doisPontos <= 0 || Character.isWhitespace(linha.charAt(0))) continue;
            String valor = linha.substring(doisPontos + 1).strip();
            if (valor.length() >= 2 && valor.startsWith("\"") && valor.endsWith("\"")) {
                valor = valor.substring(1, valor.length() - 1);
            }
            if (!valor.isEmpty()) valores.put(linha.substring(0, doisPontos).strip(), valor);
        }
        return valores;
    }

    private static String obrigatorio(Map<String, String> cabecalho, String chave, String nomeArquivo) {
        String valor = cabecalho.get(chave);
        if (valor == null) throw erro(nomeArquivo, "falta \"" + chave + "\" no cabeçalho");
        return valor;
    }

    private static IllegalStateException erro(String nomeArquivo, String motivo) {
        return new IllegalStateException("Documento legal " + nomeArquivo + " inválido: " + motivo);
    }

    static String sha256(String texto) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
