package br.com.sauf_api;

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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Testes da listagem de universidades e das rotas auxiliares (TR-330). */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class UniversidadeApiTest {

    @Autowired MockMvc mvc;
    @Autowired AreaRepository areas;
    @Autowired InstituicaoRepository instituicoes;
    @Autowired CursoRepository cursos;
    @Autowired OfertaCursoRepository ofertas;

    Instituicao uem;
    Instituicao uel;
    Curso biomedicina;

    @BeforeEach
    void dados() {
        biomedicina = cursos.save(new Curso("Biomedicina", area("09")));
        Curso engenhariaSoftware = cursos.save(new Curso("Engenharia de Software", area("06")));

        uem = instituicao(57, "Universidade Estadual de Maringá", "UEM", TipoInstituicao.PUBLICA, "PR", "Maringá");
        uel = instituicao(9, "Universidade Estadual de Londrina", "UEL", TipoInstituicao.PUBLICA, "PR", "Londrina");

        oferta(biomedicina, uem, "uem:biomedicina:maringa:integral", Modalidade.PRESENCIAL, 1001);
        oferta(biomedicina, uel, "uel:biomedicina:londrina:integral", Modalidade.PRESENCIAL, 1002);
        oferta(engenhariaSoftware, uem, "uem:engenharia-de-software:maringa:noturno", Modalidade.PRESENCIAL, 1003);
        oferta(engenhariaSoftware, uel, "uel:engenharia-de-software:londrina:ead", Modalidade.EAD, 1004);
    }

    @Test
    void buscaIgnoraAcentosEMaiusculas() throws Exception {
        instituicao(6666, "Universidade de São Paulo", "USP", TipoInstituicao.PUBLICA, "SP", "São Paulo");

        mvc.perform(get("/api/v1/universidades").param("busca", "maringa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sigla").value("UEM"));

        mvc.perform(get("/api/v1/universidades").param("busca", "MARINGÁ"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sigla").value("UEM"));

        mvc.perform(get("/api/v1/universidades").param("busca", "sao paulo"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sigla").value("USP"));

        mvc.perform(get("/api/v1/universidades").param("busca", "São"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sigla").value("USP"));

        mvc.perform(get("/api/v1/universidades").param("busca", "usp"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(get("/api/v1/universidades").param("busca", "estadual"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void buscaIgnoraHifen() throws Exception {
        instituicao(7777, "Centro de Pós-Graduação Santa Rita", "CPG", TipoInstituicao.PRIVADA, "PR", "Curitiba");

        mvc.perform(get("/api/v1/universidades").param("busca", "pos graduacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sigla").value("CPG"));

        mvc.perform(get("/api/v1/universidades").param("busca", "Pós-Graduação"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sigla").value("CPG"));

        mvc.perform(get("/api/v1/universidades").param("busca", "pos-graduacao"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sigla").value("CPG"));
    }

    @Test
    void buscaSoComHifenTrazTodas() throws Exception {
        mvc.perform(get("/api/v1/universidades").param("busca", " - "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void filtraPorFaixaDeNotaMec() throws Exception {
        uem.setNotaMec(5);
        uel.setNotaMec(3);
        instituicoes.saveAndFlush(uem);
        instituicoes.saveAndFlush(uel);

        mvc.perform(get("/api/v1/universidades").param("notaMec", "4-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sigla").value("UEM"))
                .andExpect(jsonPath("$.content[0].notaMec").value(5));

        mvc.perform(get("/api/v1/universidades").param("notaMec", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sigla").value("UEL"));

        mvc.perform(get("/api/v1/universidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void notaMecInvalidaResponde400() throws Exception {
        mvc.perform(get("/api/v1/universidades").param("notaMec", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.notaMec").exists());
    }

    @Test
    void filtraPorVariosTiposEModalidades() throws Exception {
        Instituicao privada = instituicao(1234, "Faculdade Particular de Curitiba", "FPC", TipoInstituicao.PRIVADA, "PR", "Curitiba");
        oferta(biomedicina, privada, "fpc:biomedicina:curitiba:semipresencial", Modalidade.SEMIPRESENCIAL, 1005);

        mvc.perform(get("/api/v1/universidades").param("tipo", "privada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sigla").value("FPC"));

        mvc.perform(get("/api/v1/universidades").param("tipo", "publica,privada"))
                .andExpect(jsonPath("$.totalElements").value(3));

        mvc.perform(get("/api/v1/universidades").param("tipo", "publica").param("tipo", "privada"))
                .andExpect(jsonPath("$.totalElements").value(3));

        mvc.perform(get("/api/v1/universidades").param("modalidade", "ead,semipresencial"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].sigla").value("FPC"))
                .andExpect(jsonPath("$.content[1].sigla").value("UEL"));

        mvc.perform(get("/api/v1/universidades").param("tipo", "privada").param("modalidade", "ead"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void modalidadeInvalidaNaListaResponde400() throws Exception {
        mvc.perform(get("/api/v1/universidades").param("modalidade", "ead,hibrido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.modalidade").exists());
    }

    @Test
    void listaCidadesDoEstadoSemRepetirEmOrdem() throws Exception {
        instituicao(4321, "Centro Universitário de Maringá", "CUM", TipoInstituicao.PRIVADA, "PR", "Maringá");
        Instituicao inativa = instituicao(5555, "Faculdade Fechada", "FF", TipoInstituicao.PRIVADA, "PR", "Curitiba");
        inativa.setAtiva(false);
        instituicoes.saveAndFlush(inativa);
        instituicao(6666, "Universidade de São Paulo", "USP", TipoInstituicao.PUBLICA, "SP", "São Paulo");

        mvc.perform(get("/api/v1/universidades/cidades").param("uf", "pr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]").value("Londrina"))
                .andExpect(jsonPath("$[1]").value("Maringá"));

        mvc.perform(get("/api/v1/universidades/cidades").param("uf", "SP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]").value("São Paulo"));
    }

    @Test
    void cidadesSemUfRespondeListaVazia() throws Exception {
        mvc.perform(get("/api/v1/universidades/cidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void contaPorTipoEModalidadeIgnorandoOProprioGrupo() throws Exception {
        Instituicao privada = instituicao(1234, "Faculdade Particular de Curitiba", "FPC", TipoInstituicao.PRIVADA, "PR", "Curitiba");
        oferta(biomedicina, privada, "fpc:biomedicina:curitiba:semipresencial", Modalidade.SEMIPRESENCIAL, 1005);

        mvc.perform(get("/api/v1/universidades/contagens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo.publica").value(2))
                .andExpect(jsonPath("$.tipo.privada").value(1))
                .andExpect(jsonPath("$.modalidade.presencial").value(2))
                .andExpect(jsonPath("$.modalidade.semipresencial").value(1))
                .andExpect(jsonPath("$.modalidade.ead").value(1));

        mvc.perform(get("/api/v1/universidades/contagens").param("tipo", "publica"))
                .andExpect(jsonPath("$.tipo.publica").value(2))
                .andExpect(jsonPath("$.tipo.privada").value(1))
                .andExpect(jsonPath("$.modalidade.presencial").value(2))
                .andExpect(jsonPath("$.modalidade.semipresencial").value(0))
                .andExpect(jsonPath("$.modalidade.ead").value(1));

        mvc.perform(get("/api/v1/universidades/contagens").param("modalidade", "ead"))
                .andExpect(jsonPath("$.tipo.publica").value(1))
                .andExpect(jsonPath("$.tipo.privada").value(0))
                .andExpect(jsonPath("$.modalidade.presencial").value(2))
                .andExpect(jsonPath("$.modalidade.semipresencial").value(1))
                .andExpect(jsonPath("$.modalidade.ead").value(1));
    }

    @Test
    void contagensDeEstadoSemUniversidadeRespondeZero() throws Exception {
        mvc.perform(get("/api/v1/universidades/contagens").param("uf", "SP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo.publica").value(0))
                .andExpect(jsonPath("$.tipo.privada").value(0))
                .andExpect(jsonPath("$.modalidade.presencial").value(0))
                .andExpect(jsonPath("$.modalidade.semipresencial").value(0))
                .andExpect(jsonPath("$.modalidade.ead").value(0));
    }

    private Area area(String codigoCine) {
        return areas.findAll().stream()
                .filter(a -> a.getCodigoCine().equals(codigoCine))
                .findFirst()
                .orElseThrow();
    }

    private Instituicao instituicao(
            int codigoEmec, String nome, String sigla, TipoInstituicao tipo, String uf, String cidade) {
        Instituicao i = new Instituicao(codigoEmec, nome, tipo, uf, cidade);
        i.setSigla(sigla);
        return instituicoes.save(i);
    }

    private void oferta(Curso curso, Instituicao instituicao, String chave, Modalidade modalidade, int codigoEmec) {
        OfertaCurso o = new OfertaCurso(curso, instituicao, chave, curso.getNome());
        o.setOpcoes(List.of(new Opcao(Grau.BACHARELADO, "Integral")));
        o.setModalidade(modalidade);
        o.setDuracaoSemestres(8);
        o.setUf(instituicao.getUf());
        o.setCidade(instituicao.getCidade());
        o.setUrlOrigem("https://exemplo.br/" + chave.replace(':', '/'));
        o.setCodigosEmec(List.of(codigoEmec));
        ofertas.save(o);
    }
}