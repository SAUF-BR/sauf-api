package br.com.sauf_api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CatalogoApiTest {

    @Autowired MockMvc mvc;
    @Autowired AreaRepository areas;
    @Autowired InstituicaoRepository instituicoes;
    @Autowired CursoRepository cursos;
    @Autowired OfertaCursoRepository ofertas;

    Instituicao uem;
    Instituicao uel;
    Curso biomedicina;
    Curso engenhariaSoftware;

    @BeforeEach
    void dados() {
        Area saude = area("09");
        Area computacao = area("06");

        uem = instituicao(57, "Universidade Estadual de Maringá", "UEM", "Maringá");
        uel = instituicao(9, "Universidade Estadual de Londrina", "UEL", "Londrina");

        biomedicina = cursos.save(new Curso("Biomedicina", saude));
        engenhariaSoftware = cursos.save(new Curso("Engenharia de Software", computacao));

        oferta(biomedicina, uem, "uem:biomedicina:maringa:integral", Modalidade.PRESENCIAL, 8, "712.40");
        oferta(biomedicina, uel, "uel:biomedicina:londrina:integral", Modalidade.PRESENCIAL, 10, "698.10");
        oferta(engenhariaSoftware, uem, "uem:engenharia-de-software:maringa:noturno", Modalidade.PRESENCIAL, 10, null);
        oferta(engenhariaSoftware, uel, "uel:engenharia-de-software:londrina:ead", Modalidade.EAD, 8, null);
    }

    @Test
    void listaAreasDoCensoComTotalDeCursos() throws Exception {
        mvc.perform(get("/api/v1/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(11)))
                .andExpect(jsonPath("$[?(@.codigoCine == '09')].totalCursos").value(1))
                .andExpect(jsonPath("$[?(@.codigoCine == '01')].totalCursos").value(0));
    }

    @Test
    void listaCatalogoComNumerosAgregados() throws Exception {
        mvc.perform(get("/api/v1/cursos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].nome").value("Biomedicina"))
                .andExpect(jsonPath("$.content[0].area.nome").value("Saúde e bem-estar"))
                .andExpect(jsonPath("$.content[0].totalUniversidades").value(2))
                .andExpect(jsonPath("$.content[0].modalidades", containsInAnyOrder("presencial")))
                .andExpect(jsonPath("$.content[0].graus", containsInAnyOrder("bacharelado")))
                .andExpect(jsonPath("$.content[0].duracaoSemestresMin").value(8))
                .andExpect(jsonPath("$.content[0].duracaoSemestresMax").value(10))
                .andExpect(jsonPath("$.content[0].notaCorteSisuMin").value(698.10))
                .andExpect(jsonPath("$.content[1].modalidades", containsInAnyOrder("presencial", "ead")));
    }

    @Test
    void filtrosDoCatalogoValemParaAsOfertas() throws Exception {
        mvc.perform(get("/api/v1/cursos").param("modalidade", "ead"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Engenharia de Software"))
                .andExpect(jsonPath("$.content[0].totalUniversidades").value(1));

        mvc.perform(get("/api/v1/cursos").param("cidade", "Maringá"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].totalUniversidades").value(1));

        mvc.perform(get("/api/v1/cursos").param("grau", "bacharelado"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/v1/cursos").param("grau", "licenciatura"))
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(get("/api/v1/cursos").param("area", engenhariaSoftware.getArea().getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listaOfertasDoCursoComLinkDoSite() throws Exception {
        mvc.perform(get("/api/v1/cursos/{id}/ofertas", engenhariaSoftware.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].universidade.sigla").exists())
                .andExpect(jsonPath("$.content[0].urlOrigem").value(org.hamcrest.Matchers.startsWith("https://")))
                .andExpect(jsonPath("$.content[0].opcoes[0].grau").value("bacharelado"))
                .andExpect(jsonPath("$.content[0].codigosEmec").doesNotExist());
    }

    @Test
    void listaUniversidadesComTotalDeCursosEModalidades() throws Exception {
        mvc.perform(get("/api/v1/universidades").param("busca", "londrina"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sigla").value("UEL"))
                .andExpect(jsonPath("$.content[0].tipo").value("publica"))
                .andExpect(jsonPath("$.content[0].totalCursos").value(2))
                .andExpect(jsonPath("$.content[0].modalidades", containsInAnyOrder("presencial", "ead")));

        mvc.perform(get("/api/v1/universidades").param("modalidade", "ead"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void detalhaUniversidadeComCursosPorArea() throws Exception {
        mvc.perform(get("/api/v1/universidades/{id}", uem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoEmec").value(57))
                .andExpect(jsonPath("$.cursosPorArea", hasSize(2)));
    }

    @Test
    void recursoInexistenteDevolveErroNoFormatoCombinado() throws Exception {
        mvc.perform(get("/api/v1/cursos/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.fields").isMap());
    }

    @Test
    void filtroDeNotaDeCorteNaoExisteNoSwagger() throws Exception {
        mvc.perform(get("/api/v1/docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/cursos'].get.parameters[*].name",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("notaCorteMax"))))
                .andExpect(jsonPath("$.paths['/api/v1/cursos'].get.parameters[*].name",
                        org.hamcrest.Matchers.hasItem("duracaoSemestres")));
    }

    @Test
    void parametroInvalidoDevolve400ComOCampo() throws Exception {
        mvc.perform(get("/api/v1/cursos").param("modalidade", "hibrido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.modalidade").exists());

        mvc.perform(get("/api/v1/cursos").param("sort", "inexistente"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.sort").exists());
    }

    private Area area(String codigoCine) {
        return areas.findAll().stream()
                .filter(a -> a.getCodigoCine().equals(codigoCine))
                .findFirst()
                .orElseThrow();
    }

    private Instituicao instituicao(int codigoEmec, String nome, String sigla, String cidade) {
        Instituicao i = new Instituicao(codigoEmec, nome, TipoInstituicao.PUBLICA, "PR", cidade);
        i.setSigla(sigla);
        return instituicoes.save(i);
    }

    private void oferta(
            Curso curso, Instituicao instituicao, String chave, Modalidade modalidade, int semestres, String notaCorte) {
        OfertaCurso o = new OfertaCurso(curso, instituicao, chave, curso.getNome());
        o.setOpcoes(java.util.List.of(new Opcao(Grau.BACHARELADO, "Integral")));
        o.setModalidade(modalidade);
        o.setDuracaoSemestres(semestres);
        o.setUf(instituicao.getUf());
        o.setCidade(instituicao.getCidade());
        o.setUrlOrigem("https://exemplo.br/" + chave.replace(':', '/'));
        o.setNotaCorteSisu(notaCorte == null ? null : new BigDecimal(notaCorte));
        ofertas.save(o);
    }
}
