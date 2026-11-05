package br.com.sauf_api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.area.Area;
import br.com.sauf_api.area.AreaRepository;
import br.com.sauf_api.curso.Curso;
import br.com.sauf_api.curso.CursoRepository;
import br.com.sauf_api.instituicao.Instituicao;
import br.com.sauf_api.instituicao.InstituicaoRepository;
import br.com.sauf_api.instituicao.TipoInstituicao;
import br.com.sauf_api.oferta.Grau;
import br.com.sauf_api.oferta.Modalidade;
import br.com.sauf_api.oferta.OfertaCurso;
import br.com.sauf_api.oferta.OfertaCursoRepository;
import br.com.sauf_api.oferta.Opcao;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Testes dos cursos favoritos (Meus favoritos - Cursos). */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class FavoritoCursoApiTest {

    static final String ROTA = "/api/v1/favoritos/cursos";

    @Autowired MockMvc mvc;
    @Autowired AreaRepository areas;
    @Autowired InstituicaoRepository instituicoes;
    @Autowired CursoRepository cursos;
    @Autowired OfertaCursoRepository ofertas;

    Instituicao uem;
    OfertaCurso biomedicinaUem;

    @BeforeEach
    void dados() {
        uem = instituicao(57, "UEM", TipoInstituicao.PUBLICA);
        Curso biomedicina = cursos.save(new Curso("Biomedicina", area("09")));
        biomedicinaUem = oferta(biomedicina, uem, "uem:biomedicina:integral");
    }

    @Test
    void semFavoritosRespondeListaVazia() throws Exception {
        mvc.perform(get(ROTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get(ROTA + "/ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void favoritarApareceNaListaComOsDadosDaOferta() throws Exception {
        mvc.perform(post(ROTA + "/{id}", biomedicinaUem.getId()))
                .andExpect(status().isNoContent());

        mvc.perform(get(ROTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(biomedicinaUem.getId().toString()))
                .andExpect(jsonPath("$[0].curso.nome").value("Biomedicina"))
                .andExpect(jsonPath("$[0].universidade.sigla").value("UEM"))
                .andExpect(jsonPath("$[0].universidade.tipo").value("publica"))
                .andExpect(jsonPath("$[0].modalidade").value("presencial"))
                .andExpect(jsonPath("$[0].opcoes[0].grau").value("bacharelado"))
                .andExpect(jsonPath("$[0].duracaoSemestres").value(8))
                .andExpect(jsonPath("$[0].cidade").value("Maringá"))
                .andExpect(jsonPath("$[0].uf").value("PR"))
                .andExpect(jsonPath("$[0].notaCorteSisu").value(nullValue()))
                .andExpect(jsonPath("$[0].mensalidade").value(nullValue()));

        mvc.perform(get(ROTA + "/ids"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]").value(biomedicinaUem.getId().toString()));
    }

    @Test
    void favoritarDuasVezesNaoDuplica() throws Exception {
        mvc.perform(post(ROTA + "/{id}", biomedicinaUem.getId())).andExpect(status().isNoContent());
        mvc.perform(post(ROTA + "/{id}", biomedicinaUem.getId())).andExpect(status().isNoContent());

        mvc.perform(get(ROTA))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void desfavoritarTiraDaLista() throws Exception {
        mvc.perform(post(ROTA + "/{id}", biomedicinaUem.getId())).andExpect(status().isNoContent());

        mvc.perform(delete(ROTA + "/{id}", biomedicinaUem.getId()))
                .andExpect(status().isNoContent());

        mvc.perform(get(ROTA))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void desfavoritarQueNaoEraFavoritoNaoDaErro() throws Exception {
        mvc.perform(delete(ROTA + "/{id}", biomedicinaUem.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void favoritarOfertaInexistenteResponde404() throws Exception {
        mvc.perform(post(ROTA + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void favoritarOfertaInativaResponde404() throws Exception {
        biomedicinaUem.setAtiva(false);
        ofertas.saveAndFlush(biomedicinaUem);

        mvc.perform(post(ROTA + "/{id}", biomedicinaUem.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void favoritarOfertaDeUniversidadeInativaResponde404() throws Exception {
        uem.setAtiva(false);
        instituicoes.saveAndFlush(uem);

        mvc.perform(post(ROTA + "/{id}", biomedicinaUem.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void ofertaQueFicouInativaSaiDosFavoritos() throws Exception {
        mvc.perform(post(ROTA + "/{id}", biomedicinaUem.getId())).andExpect(status().isNoContent());

        biomedicinaUem.setAtiva(false);
        ofertas.saveAndFlush(biomedicinaUem);

        mvc.perform(get(ROTA))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(ROTA + "/ids"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void universidadeQueFicouInativaTiraOCursoDosFavoritos() throws Exception {
        mvc.perform(post(ROTA + "/{id}", biomedicinaUem.getId())).andExpect(status().isNoContent());

        uem.setAtiva(false);
        instituicoes.saveAndFlush(uem);

        mvc.perform(get(ROTA))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private Area area(String codigoCine) {
        return areas.findAll().stream()
                .filter(a -> a.getCodigoCine().equals(codigoCine))
                .findFirst()
                .orElseThrow();
    }

    private Instituicao instituicao(int codigoEmec, String sigla, TipoInstituicao tipo) {
        Instituicao i = new Instituicao(codigoEmec, "Instituição " + sigla, tipo, "PR", "Maringá");
        i.setSigla(sigla);
        return instituicoes.save(i);
    }

    private OfertaCurso oferta(Curso curso, Instituicao instituicao, String chave) {
        OfertaCurso o = new OfertaCurso(curso, instituicao, chave, curso.getNome());
        o.setOpcoes(List.of(new Opcao(Grau.BACHARELADO, "Integral")));
        o.setModalidade(Modalidade.PRESENCIAL);
        o.setDuracaoSemestres(8);
        o.setUf(instituicao.getUf());
        o.setCidade(instituicao.getCidade());
        o.setUrlOrigem("https://exemplo.br/" + chave.replace(':', '/'));
        return ofertas.save(o);
    }
}