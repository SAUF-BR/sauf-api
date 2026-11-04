package br.com.sauf_api.instituicao;

import static org.assertj.core.api.Assertions.assertThat;
import br.com.sauf_api.curso.Curso;
import br.com.sauf_api.oferta.Modalidade;
import br.com.sauf_api.oferta.OfertaCurso;
import java.util.List;
import org.junit.jupiter.api.Test;

class UniversidadeRegrasTest {

    @Test
    void normalizarTiraAcentoMaiusculaEHifen() {
        assertThat(FiltroUniversidades.normalizar("São Paulo")).isEqualTo("sao paulo");
        assertThat(FiltroUniversidades.normalizar("MARINGÁ")).isEqualTo("maringa");
        assertThat(FiltroUniversidades.normalizar("Pós-Graduação")).isEqualTo("pos graduacao");
        assertThat(FiltroUniversidades.normalizar("Conceição")).isEqualTo("conceicao");
    }

    @Test
    void normalizarJuntaEspacosRepetidos() {
        assertThat(FiltroUniversidades.normalizar("  Estadual   de  -  Maringá  ")).isEqualTo("estadual de maringa");
    }

    @Test
    void normalizarDevolveVazioSemTexto() {
        assertThat(FiltroUniversidades.normalizar(null)).isEmpty();
        assertThat(FiltroUniversidades.normalizar("   ")).isEmpty();
        assertThat(FiltroUniversidades.normalizar(" - ")).isEmpty();
    }

    @Test
    void esferaDasPublicasNoFormatoDoScraper() {
        assertThat(UniversidadeService.esfera("publica_federal")).isEqualTo(Esfera.FEDERAL);
        assertThat(UniversidadeService.esfera("publica_estadual")).isEqualTo(Esfera.ESTADUAL);
        assertThat(UniversidadeService.esfera("publica_municipal")).isEqualTo(Esfera.MUNICIPAL);
    }

    @Test
    void esferaAceitaAcentoEspacoEMaiuscula() {
        assertThat(UniversidadeService.esfera("Pública Estadual")).isEqualTo(Esfera.ESTADUAL);
        assertThat(UniversidadeService.esfera("PÚBLICA FEDERAL")).isEqualTo(Esfera.FEDERAL);
    }

    @Test
    void esferaNullParaPrivadaOuSemCategoria() {
        assertThat(UniversidadeService.esfera("privada_com_fins_lucrativos")).isNull();
        assertThat(UniversidadeService.esfera("privada_sem_fins_lucrativos")).isNull();
        assertThat(UniversidadeService.esfera("especial")).isNull();
        assertThat(UniversidadeService.esfera("publica")).isNull();
        assertThat(UniversidadeService.esfera(null)).isNull();
    }

    @Test
    void gratuitaQuandoTodasAsInformadasSaoGratuitas() {
        assertThat(UniversidadeService.gratuita(List.of(oferta(true), oferta(true)))).isTrue();
        assertThat(UniversidadeService.gratuita(List.of(oferta(true), oferta(null)))).isTrue();
    }

    @Test
    void naoGratuitaComOfertaPagaOuSemInformacao() {
        assertThat(UniversidadeService.gratuita(List.of(oferta(true), oferta(false)))).isFalse();
        assertThat(UniversidadeService.gratuita(List.of(oferta(null), oferta(null)))).isFalse();
        assertThat(UniversidadeService.gratuita(List.of())).isFalse();
    }

    @Test
    void vagasPorAnoSomaSoAsInformadas() {
        assertThat(UniversidadeService.vagasPorAno(List.of(vagas(40), vagas(60), vagas(null)))).isEqualTo(100);
    }

    @Test
    void vagasPorAnoNullSemInformacao() {
        assertThat(UniversidadeService.vagasPorAno(List.of(vagas(null)))).isNull();
        assertThat(UniversidadeService.vagasPorAno(List.of())).isNull();
    }

    @Test
    void modalidadesSempreAsTresNaOrdemComTotais() {
        Curso biomedicina = new Curso("Biomedicina", null);
        Curso pedagogia = new Curso("Pedagogia", null);
        var resultado = UniversidadeService.modalidadesOfertadas(List.of(
                modalidade(Modalidade.PRESENCIAL, biomedicina),
                modalidade(Modalidade.PRESENCIAL, pedagogia),
                modalidade(Modalidade.EAD, pedagogia),
                modalidade(null, biomedicina)));

        assertThat(resultado).extracting(UniversidadeDetalhe.ModalidadeOfertada::modalidade)
                .containsExactly(Modalidade.PRESENCIAL, Modalidade.SEMIPRESENCIAL, Modalidade.EAD);
        assertThat(resultado).extracting(UniversidadeDetalhe.ModalidadeOfertada::totalCursos)
                .containsExactly(2L, 0L, 1L);
        assertThat(resultado).extracting(UniversidadeDetalhe.ModalidadeOfertada::disponivel)
                .containsExactly(true, false, true);
        assertThat(resultado).extracting(UniversidadeDetalhe.ModalidadeOfertada::descricao)
                .containsOnlyNulls();
    }

    @Test
    void modalidadesContamCursosENaoOfertas() {
        Curso biomedicina = new Curso("Biomedicina", null);
        var resultado = UniversidadeService.modalidadesOfertadas(List.of(
                modalidade(Modalidade.PRESENCIAL, biomedicina),
                modalidade(Modalidade.PRESENCIAL, biomedicina)));

        assertThat(resultado.get(0).totalCursos()).isEqualTo(1);
    }

    @Test
    void modalidadesSemOfertasVemTodasIndisponiveis() {
        var resultado = UniversidadeService.modalidadesOfertadas(List.of());

        assertThat(resultado).hasSize(3);
        assertThat(resultado).extracting(UniversidadeDetalhe.ModalidadeOfertada::disponivel)
                .containsOnly(false);
    }

    @Test
    void faixasDaNotaMec() {
        assertThat(FaixaNotaMec.QUATRO_OU_CINCO.valor()).isEqualTo("4-5");
        assertThat(FaixaNotaMec.QUATRO_OU_CINCO.minima()).isEqualTo(4);
        assertThat(FaixaNotaMec.QUATRO_OU_CINCO.maxima()).isEqualTo(5);
        assertThat(FaixaNotaMec.TRES.valor()).isEqualTo("3");
        assertThat(FaixaNotaMec.TRES.minima()).isEqualTo(3);
        assertThat(FaixaNotaMec.TRES.maxima()).isEqualTo(3);
    }

    private static OfertaCurso novaOferta() {
        return new OfertaCurso(null, null, "chave", "Curso");
    }

    private static OfertaCurso oferta(Boolean gratuito) {
        OfertaCurso o = novaOferta();
        o.setGratuito(gratuito);
        return o;
    }

    private static OfertaCurso vagas(Integer vagas) {
        OfertaCurso o = novaOferta();
        o.setVagas(vagas);
        return o;
    }

    private static OfertaCurso modalidade(Modalidade modalidade, Curso curso) {
        OfertaCurso o = new OfertaCurso(curso, null, "chave", curso.getNome());
        o.setModalidade(modalidade);
        return o;
    }
}