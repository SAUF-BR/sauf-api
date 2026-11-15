package br.com.sauf_api.documento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ArquivoDocumentoTest {

    private static final String ARQUIVO = """
            ---
            tipo: TERMOS_USO
            versao: 1.2.0
            vigente_a_partir_de: "2026-12-01"
            exige_novo_aceite: false
            resumo_alteracoes: "Ajuste de redação"
            finalidades:
              - FOTO_PERFIL
            ---

            # Termos de Uso

            Texto.
            """;

    @Test
    void leOCabecalhoEOTitulo() {
        ArquivoDocumento arquivo = ArquivoDocumento.ler("termos.md", ARQUIVO);

        assertThat(arquivo.tipo()).isEqualTo(TipoDocumento.TERMOS_USO);
        assertThat(arquivo.versao()).isEqualTo("1.2.0");
        assertThat(arquivo.vigenteAPartirDe()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(arquivo.exigeNovoAceite()).isFalse();
        assertThat(arquivo.resumoAlteracoes()).isEqualTo("Ajuste de redação");
        assertThat(arquivo.titulo()).isEqualTo("Termos de Uso");
        assertThat(arquivo.conteudo()).startsWith("# Termos de Uso").doesNotContain("versao:");
    }

    @Test
    void oHashNaoMudaComQuebraDeLinhaDoWindows() {
        assertThat(ArquivoDocumento.ler("a.md", ARQUIVO.replace("\n", "\r\n")).sha256())
                .isEqualTo(ArquivoDocumento.ler("a.md", ARQUIVO).sha256());
    }

    @Test
    void dataSemPreencherNaoSobe() {
        String semData = ARQUIVO.replace("\"2026-12-01\"", "\"[DATA DE PUBLICAÇÃO]\"");

        assertThatThrownBy(() -> ArquivoDocumento.ler("termos.md", semData))
                .hasMessageContaining("vigente_a_partir_de");
    }

    @Test
    void ordenaVersoesComoNumeros() {
        assertThat(Versao.ORDEM.compare("1.10.0", "1.9.0")).isPositive();
        assertThat(Versao.ORDEM.compare("2.0.0", "1.99.99")).isPositive();
        assertThat(Versao.ORDEM.compare("1.0", "1.0.0")).isZero();
    }
}
