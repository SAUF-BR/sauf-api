package br.com.sauf_api;

import static org.hamcrest.Matchers.hasSize;
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

/** Testes das universidades favoritas (Meus favoritos - Universidades). */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class FavoritoUniversidadeApiTest {

    static final String ROTA = "/api/v1/favoritos/universidades";

    @Autowired MockMvc mvc;
    @Autowired AreaRepository areas;
    @Autowired InstituicaoRepository instituicoes;
    @Autowired CursoRepository cursos;
    @Autowired OfertaCursoRepository ofertas;

    Instituicao uem;

    @BeforeEach
    void dados() {
        uem = instituicao(57, "UEM", TipoInstituicao.PUBLICA);
        Curso biomedicina = cursos.save(new Curso("Biomedicina", area("09")));
        oferta(biomedicina, uem, "uem:biomedicina:integral", Modalidade.PRESENCIAL);
    }

    @Test
    void semFavoritasRespondeListaVazia() throws Exception {
        mvc.perform(get(ROTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get(ROTA + "/ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void favoritarApareceNaListaComOsDadosDaListagem() throws Exception {
        mvc.perform(post(ROTA + "/{id}", uem.getId()))
                .andExpect(status().isNoContent());

        mvc.perform(get(ROTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(uem.getId().toString()))
                .andExpect(jsonPath("$[0].sigla").value("UEM"))
                .andExpect(jsonPath("$[0].tipo").value("publica"))
                .andExpect(jsonPath("$[0].totalCursos").value(1))
                .andExpect(jsonPath("$[0].totalCampi").value(1))
                .andExpect(jsonPath("$[0].modalidades[0]").value("presencial"));

        mvc.perform(get(ROTA + "/ids"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]").value(uem.getId().toString()));
    }

    @Test
    void totalCampiContaAsCidadesDasOfertasAtivas() throws Exception {
        Curso pedagogia = cursos.save(new Curso("Pedagogia", area("01")));
        oferta(pedagogia, uem, "uem:pedagogia:maringa", Modalidade.PRESENCIAL, "Maringá", true);
        oferta(pedagogia, uem, "uem:pedagogia:umuarama", Modalidade.PRESENCIAL, "Umuarama", true);
        oferta(pedagogia, uem, "uem:pedagogia:goioere", Modalidade.PRESENCIAL, "Goioerê", false);

        mvc.perform(post(ROTA + "/{id}", uem.getId())).andExpect(status().isNoContent());

        mvc.perform(get(ROTA))
                .andExpect(jsonPath("$[0].totalCampi").value(2));
    }

    @Test
    void favoritarDuasVezesNaoDuplica() throws Exception {
        mvc.perform(post(ROTA + "/{id}", uem.getId())).andExpect(status().isNoContent());
        mvc.perform(post(ROTA + "/{id}", uem.getId())).andExpect(status().isNoContent());

        mvc.perform(get(ROTA))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void desfavoritarTiraDaLista() throws Exception {
        mvc.perform(post(ROTA + "/{id}", uem.getId())).andExpect(status().isNoContent());

        mvc.perform(delete(ROTA + "/{id}", uem.getId()))
                .andExpect(status().isNoContent());

        mvc.perform(get(ROTA))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void desfavoritarQueNaoEraFavoritaNaoDaErro() throws Exception {
        mvc.perform(delete(ROTA + "/{id}", uem.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void favoritarUniversidadeInexistenteResponde404() throws Exception {
        mvc.perform(post(ROTA + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void favoritarUniversidadeInativaResponde404() throws Exception {
        Instituicao inativa = instituicao(5555, "FF", TipoInstituicao.PRIVADA);
        inativa.setAtiva(false);
        instituicoes.saveAndFlush(inativa);

        mvc.perform(post(ROTA + "/{id}", inativa.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void universidadeQueFicouInativaSaiDosFavoritos() throws Exception {
        mvc.perform(post(ROTA + "/{id}", uem.getId())).andExpect(status().isNoContent());

        uem.setAtiva(false);
        instituicoes.saveAndFlush(uem);

        mvc.perform(get(ROTA))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(ROTA + "/ids"))
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

    private void oferta(Curso curso, Instituicao instituicao, String chave, Modalidade modalidade) {
        oferta(curso, instituicao, chave, modalidade, instituicao.getCidade(), true);
    }

    private void oferta(
            Curso curso, Instituicao instituicao, String chave, Modalidade modalidade, String cidade, boolean ativa) {
        OfertaCurso o = new OfertaCurso(curso, instituicao, chave, curso.getNome());
        o.setOpcoes(List.of(new Opcao(Grau.BACHARELADO, "Integral")));
        o.setModalidade(modalidade);
        o.setDuracaoSemestres(8);
        o.setUf(instituicao.getUf());
        o.setCidade(cidade);
        o.setUrlOrigem("https://exemplo.br/" + chave.replace(':', '/'));
        o.setAtiva(ativa);
        ofertas.save(o);
    }
}