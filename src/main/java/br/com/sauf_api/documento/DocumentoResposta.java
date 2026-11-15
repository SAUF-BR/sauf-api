package br.com.sauf_api.documento;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** Versão vigente de um documento legal, para mostrar no cadastro e no novo aceite (D-93). */
public record DocumentoResposta(
        TipoDocumento tipo,
        String versao,
        String titulo,
        @Schema(description = "Desde quando esta versão vale (\"Última atualização\")") LocalDate vigenteDesde,
        @Schema(description = "Tempo de leitura estimado (200 palavras por minuto)") int minutosLeitura,
        @Schema(description = "O que mudou desta versão para a anterior; null na primeira") String resumoAlteracoes,
        @Schema(description = "Texto em Markdown") String conteudo,
        String sha256) {

    private static final int PALAVRAS_POR_MINUTO = 200;

    static DocumentoResposta de(DocumentoLegalVersao documento) {
        int palavras = documento.getConteudoMarkdown().split("\\s+").length;
        return new DocumentoResposta(
                documento.getTipo(),
                documento.getVersao(),
                documento.getTitulo(),
                documento.getVigenteAPartirDe(),
                Math.max(1, Math.round(palavras / (float) PALAVRAS_POR_MINUTO)),
                documento.getResumoAlteracoes(),
                documento.getConteudoMarkdown(),
                documento.getConteudoSha256());
    }
}
