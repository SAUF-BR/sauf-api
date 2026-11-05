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
import br.com.sauf_api.curso.Curso;
import br.com.sauf_api.instituicao.Instituicao;
import br.com.sauf_api.instituicao.TipoInstituicao;
import br.com.sauf_api.oferta.OfertaCurso;
import br.com.sauf_api.oferta.OfertaCursoRepository;
import br.com.sauf_api.sisu.NotasSisu;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FavoritoCursoServiceTest {

    static final UUID USUARIO = UUID.randomUUID();
    static final UUID OFERTA = UUID.randomUUID();

    FavoritoCursoRepository favoritos;
    OfertaCursoRepository ofertas;
    NotasSisu sisu;
    FavoritoCursoService service;

    @BeforeEach
    void preparar() {
        favoritos = mock(FavoritoCursoRepository.class);
        ofertas = mock(OfertaCursoRepository.class);
        sisu = mock(NotasSisu.class);
        service = new FavoritoCursoService(favoritos, ofertas, sisu);
    }

    @Test
    void favoritarSalvaQuandoAindaNaoEFavorito() {
        OfertaCurso biomedicina = biomedicinaUem();
        when(ofertas.findById(OFERTA)).thenReturn(Optional.of(biomedicina));
        when(favoritos.existsByUsuarioIdAndOfertaId(USUARIO, OFERTA)).thenReturn(false);

        service.favoritar(USUARIO, OFERTA);

        ArgumentCaptor<FavoritoCurso> salvo = ArgumentCaptor.forClass(FavoritoCurso.class);
        verify(favoritos).save(salvo.capture());
        assertThat(salvo.getValue().getUsuarioId()).isEqualTo(USUARIO);
        assertThat(salvo.getValue().getOferta()).isSameAs(biomedicina);
        assertThat(salvo.getValue().getCriadoEm()).isNotNull();
    }

    @Test
    void favoritarDeNovoNaoSalvaOutraVez() {
        when(ofertas.findById(OFERTA)).thenReturn(Optional.of(biomedicinaUem()));
        when(favoritos.existsByUsuarioIdAndOfertaId(USUARIO, OFERTA)).thenReturn(true);

        service.favoritar(USUARIO, OFERTA);

        verify(favoritos, never()).save(any());
    }

    @Test
    void favoritarOfertaInexistenteDaErro() {
        when(ofertas.findById(OFERTA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.favoritar(USUARIO, OFERTA))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(favoritos, never()).save(any());
    }

    @Test
    void favoritarOfertaInativaDaErro() {
        OfertaCurso inativa = biomedicinaUem();
        inativa.setAtiva(false);
        when(ofertas.findById(OFERTA)).thenReturn(Optional.of(inativa));

        assertThatThrownBy(() -> service.favoritar(USUARIO, OFERTA))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(favoritos, never()).save(any());
    }

    @Test
    void favoritarOfertaDeUniversidadeInativaDaErro() {
        OfertaCurso oferta = biomedicinaUem();
        oferta.getInstituicao().setAtiva(false);
        when(ofertas.findById(OFERTA)).thenReturn(Optional.of(oferta));

        assertThatThrownBy(() -> service.favoritar(USUARIO, OFERTA))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(favoritos, never()).save(any());
    }

    @Test
    void desfavoritarApagaPeloUsuarioEPelaOferta() {
        service.desfavoritar(USUARIO, OFERTA);

        verify(favoritos).deleteByUsuarioIdAndOfertaId(USUARIO, OFERTA);
    }

    @Test
    void listarSemFavoritosNaoConsultaOSisu() {
        when(favoritos.findByUsuarioIdAndOfertaAtivaTrueAndOfertaInstituicaoAtivaTrueOrderByCriadoEmDesc(USUARIO))
                .thenReturn(List.of());

        assertThat(service.listar(USUARIO)).isEmpty();
        verifyNoInteractions(sisu);
    }

    @Test
    void listarMontaARespostaDaOfertaSemCorteDoSisu() {
        OfertaCurso biomedicina = biomedicinaUem();
        when(favoritos.findByUsuarioIdAndOfertaAtivaTrueAndOfertaInstituicaoAtivaTrueOrderByCriadoEmDesc(USUARIO))
                .thenReturn(List.of(new FavoritoCurso(USUARIO, biomedicina)));

        var resultado = service.listar(USUARIO);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).curso().nome()).isEqualTo("Biomedicina");
        assertThat(resultado.get(0).universidade().sigla()).isEqualTo("UEM");
        assertThat(resultado.get(0).notaCorteSisu()).isNull();
    }

    @Test
    void idsVemNaMesmaOrdemDosFavoritos() {
        UUID idPedagogia = UUID.randomUUID();
        UUID idBiomedicina = UUID.randomUUID();
        OfertaCurso pedagogia = comId(idPedagogia);
        OfertaCurso biomedicina = comId(idBiomedicina);
        when(favoritos.findByUsuarioIdAndOfertaAtivaTrueAndOfertaInstituicaoAtivaTrueOrderByCriadoEmDesc(USUARIO))
                .thenReturn(List.of(
                        new FavoritoCurso(USUARIO, pedagogia),
                        new FavoritoCurso(USUARIO, biomedicina)));

        assertThat(service.ids(USUARIO)).containsExactly(idPedagogia, idBiomedicina);
    }

    private static OfertaCurso biomedicinaUem() {
        Instituicao uem = new Instituicao(57, "Universidade Estadual de Maringá", TipoInstituicao.PUBLICA, "PR", "Maringá");
        uem.setSigla("UEM");
        return new OfertaCurso(new Curso("Biomedicina", null), uem, "uem:biomedicina", "Biomedicina");
    }

    private static OfertaCurso comId(UUID id) {
        OfertaCurso o = mock(OfertaCurso.class);
        when(o.getId()).thenReturn(id);
        return o;
    }
}