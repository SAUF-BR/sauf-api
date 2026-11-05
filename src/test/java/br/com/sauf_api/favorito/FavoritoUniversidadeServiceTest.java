package br.com.sauf_api.favorito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.sauf_api.comum.RecursoNaoEncontradoException;
import br.com.sauf_api.instituicao.Instituicao;
import br.com.sauf_api.instituicao.InstituicaoRepository;
import br.com.sauf_api.instituicao.TipoInstituicao;
import br.com.sauf_api.oferta.EstatisticasOfertas;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FavoritoUniversidadeServiceTest {

    static final UUID USUARIO = UUID.randomUUID();
    static final UUID UNIVERSIDADE = UUID.randomUUID();

    FavoritoUniversidadeRepository favoritos;
    InstituicaoRepository instituicoes;
    EstatisticasOfertas estatisticas;
    FavoritoUniversidadeService service;

    @BeforeEach
    void preparar() {
        favoritos = mock(FavoritoUniversidadeRepository.class);
        instituicoes = mock(InstituicaoRepository.class);
        estatisticas = mock(EstatisticasOfertas.class);
        service = new FavoritoUniversidadeService(favoritos, instituicoes, estatisticas);
    }

    @Test
    void favoritarSalvaQuandoAindaNaoEFavorita() {
        Instituicao uem = uem();
        when(instituicoes.findByIdAndAtivaTrue(UNIVERSIDADE)).thenReturn(Optional.of(uem));
        when(favoritos.existsByUsuarioIdAndInstituicaoId(USUARIO, UNIVERSIDADE)).thenReturn(false);

        service.favoritar(USUARIO, UNIVERSIDADE);

        ArgumentCaptor<FavoritoUniversidade> salvo = ArgumentCaptor.forClass(FavoritoUniversidade.class);
        verify(favoritos).save(salvo.capture());
        assertThat(salvo.getValue().getUsuarioId()).isEqualTo(USUARIO);
        assertThat(salvo.getValue().getInstituicao()).isSameAs(uem);
        assertThat(salvo.getValue().getCriadoEm()).isNotNull();
    }

    @Test
    void favoritarDeNovoNaoSalvaOutraVez() {
        when(instituicoes.findByIdAndAtivaTrue(UNIVERSIDADE)).thenReturn(Optional.of(uem()));
        when(favoritos.existsByUsuarioIdAndInstituicaoId(USUARIO, UNIVERSIDADE)).thenReturn(true);

        service.favoritar(USUARIO, UNIVERSIDADE);

        verify(favoritos, never()).save(any());
    }

    @Test
    void favoritarUniversidadeInexistenteOuInativaDaErroENaoSalva() {
        when(instituicoes.findByIdAndAtivaTrue(UNIVERSIDADE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.favoritar(USUARIO, UNIVERSIDADE))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(favoritos, never()).save(any());
    }

    @Test
    void desfavoritarApagaPeloUsuarioEPelaUniversidade() {
        service.desfavoritar(USUARIO, UNIVERSIDADE);

        verify(favoritos).deleteByUsuarioIdAndInstituicaoId(USUARIO, UNIVERSIDADE);
    }

    @Test
    void listarSemFavoritasNaoConsultaAsEstatisticas() {
        when(favoritos.findByUsuarioIdAndInstituicaoAtivaTrueOrderByCriadoEmDesc(USUARIO)).thenReturn(List.of());

        assertThat(service.listar(USUARIO)).isEmpty();
        verifyNoInteractions(estatisticas);
    }

    @Test
    void listarUniversidadeSemOfertasVemComZeroCursosECampi() {
        Instituicao uem = uem();
        when(favoritos.findByUsuarioIdAndInstituicaoAtivaTrueOrderByCriadoEmDesc(USUARIO))
                .thenReturn(List.of(new FavoritoUniversidade(USUARIO, uem)));
        when(estatisticas.porInstituicao(any())).thenReturn(new HashMap<>());

        var resultado = service.listar(USUARIO);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).sigla()).isEqualTo("UEM");
        assertThat(resultado.get(0).totalCursos()).isZero();
        assertThat(resultado.get(0).totalCampi()).isZero();
        assertThat(resultado.get(0).modalidades()).isEmpty();
    }

    @Test
    void listarUsaOTotalDeCampiDaConsulta() {
        UUID idUem = UUID.randomUUID();
        when(favoritos.findByUsuarioIdAndInstituicaoAtivaTrueOrderByCriadoEmDesc(USUARIO))
                .thenReturn(List.of(new FavoritoUniversidade(USUARIO, comId(idUem))));
        when(estatisticas.porInstituicao(any())).thenReturn(new HashMap<>());
        when(favoritos.campiPorInstituicao(any()))
                .thenReturn(List.<Object[]>of(new Object[] {idUem, 3L}));

        assertThat(service.listar(USUARIO).get(0).totalCampi()).isEqualTo(3);
    }

    @Test
    void idsVemNaMesmaOrdemDosFavoritos() {
        UUID idUel = UUID.randomUUID();
        UUID idUem = UUID.randomUUID();
        when(favoritos.findByUsuarioIdAndInstituicaoAtivaTrueOrderByCriadoEmDesc(USUARIO))
                .thenReturn(List.of(
                        new FavoritoUniversidade(USUARIO, comId(idUel)),
                        new FavoritoUniversidade(USUARIO, comId(idUem))));

        assertThat(service.ids(USUARIO)).containsExactly(idUel, idUem);
    }

    private static Instituicao uem() {
        Instituicao i = new Instituicao(57, "Universidade Estadual de Maringá", TipoInstituicao.PUBLICA, "PR", "Maringá");
        i.setSigla("UEM");
        return i;
    }

    private static Instituicao comId(UUID id) {
        Instituicao i = mock(Instituicao.class);
        when(i.getId()).thenReturn(id);
        return i;
    }
}