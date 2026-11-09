package br.com.sauf_api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
class UniversidadeDetalheApiTest {

    @Autowired MockMvc mvc;
    @Autowired AreaRepository areas;
    @Autowired InstituicaoRepository instituicoes;
    @Autowired CursoRepository cursos;
    @Autowired OfertaCursoRepository ofertas;

    Curso biomedicina;
    Curso pedagogia;

    @BeforeEach
    void dados() {
        biomedicina = cursos.save(new Curso("Biomedicina", area("09")));
        pedagogia = cursos.save(new Curso("Pedagogia", area("01")));
    }

    @Test
    void detalheCalculaVagasGratuidadeEModalidades() throws Exception {
        Instituicao uem = instituicao(57, "UEM", TipoInstituicao.PUBLICA, "publica_estadual");
        oferta(biomedicina, uem, "uem:biomedicina:integral", Modalidade.PRESENCIAL, 40, true, true);
        oferta(pedagogia, uem, "uem:pedagogia:noturno", Modalidade.PRESENCIAL, 60, true, true);
        oferta(pedagogia, uem, "uem:pedagogia:semipresencial", Modalidade.SEMIPRESENCIAL, null, true, true);
        oferta(biomedicina, uem, "uem:biomedicina:ead-encerrado", Modalidade.EAD, 999, false, false);

        mvc.perform(get("/api/v1/universidades/{id}", uem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esfera").value("estadual"))
                .andExpect(jsonPath("$.totalCursos").value(2))
                .andExpect(jsonPath("$.vagasPorAno").value(100))
                .andExpect(jsonPath("$.gratuita").value(true))
                .andExpect(jsonPath("$.modalidades", hasSize(3)))
                .andExpect(jsonPath("$.modalidades[0].modalidade").value("presencial"))
                .andExpect(jsonPath("$.modalidades[0].disponivel").value(true))
                .andExpect(jsonPath("$.modalidades[0].totalCursos").value(2))
                .andExpect(jsonPath("$.modalidades[1].modalidade").value("semipresencial"))
                .andExpect(jsonPath("$.modalidades[1].disponivel").value(true))
                .andExpect(jsonPath("$.modalidades[1].totalCursos").value(1))
                .andExpect(jsonPath("$.modalidades[2].modalidade").value("ead"))
                .andExpect(jsonPath("$.modalidades[2].disponivel").value(false))
                .andExpect(jsonPath("$.modalidades[2].totalCursos").value(0));
    }
    @Test
    void mesmoCursoEmDuasOfertasContaComoUmCurso() throws Exception {
        Instituicao uem = instituicao(57, "UEM", TipoInstituicao.PUBLICA, "publica_estadual");
        oferta(biomedicina, uem, "uem:biomedicina:integral", Modalidade.PRESENCIAL, 40, true, true);
        oferta(biomedicina, uem, "uem:biomedicina:noturno", Modalidade.PRESENCIAL, 30, true, true);

        mvc.perform(get("/api/v1/universidades/{id}", uem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCursos").value(1))
                .andExpect(jsonPath("$.vagasPorAno").value(70))
                .andExpect(jsonPath("$.modalidades[0].totalCursos").value(1))
                .andExpect(jsonPath("$.cursosPorArea", hasSize(1)))
                .andExpect(jsonPath("$.cursosPorArea[0].totalCursos").value(1));

        mvc.perform(get("/api/v1/universidades").param("busca", "UEM"))
                .andExpect(jsonPath("$.content[0].totalCursos").value(1));
    }
    @Test
    void camposSemFonteVemVaziosNoFormatoDoContrato() throws Exception {
        Instituicao uem = instituicao(57, "UEM", TipoInstituicao.PUBLICA, "publica_estadual");

        mvc.perform(get("/api/v1/universidades/{id}", uem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capaUrl").value(nullValue()))
                .andExpect(jsonPath("$.anoFundacao").value(nullValue()))
                .andExpect(jsonPath("$.sobre", hasSize(0)))
                .andExpect(jsonPath("$.formasIngresso", hasSize(0)))
                .andExpect(jsonPath("$.notaFormasIngresso").value(nullValue()))
                .andExpect(jsonPath("$.proximoPrazo").value(nullValue()))
                .andExpect(jsonPath("$.enderecoSede.cidade").value("Maringá"));
    }

    @Test
    void universidadeSemOfertasRespondeZerosEVagasNull() throws Exception {
        Instituicao uem = instituicao(57, "UEM", TipoInstituicao.PUBLICA, "publica_estadual");

        mvc.perform(get("/api/v1/universidades/{id}", uem.getId()))
                .andExpect(jsonPath("$.totalCursos").value(0))
                .andExpect(jsonPath("$.vagasPorAno").value(nullValue()))
                .andExpect(jsonPath("$.gratuita").value(false))
                .andExpect(jsonPath("$.modalidades", hasSize(3)))
                .andExpect(jsonPath("$.modalidades[0].disponivel").value(false))
                .andExpect(jsonPath("$.modalidades[0].totalCursos").value(0));
    }

    @Test
    void privadaComOfertaPagaNaoEGratuitaENaoTemEsfera() throws Exception {
        Instituicao privada = instituicao(1234, "FPC", TipoInstituicao.PRIVADA, "privada_com_fins_lucrativos");
        oferta(biomedicina, privada, "fpc:biomedicina", Modalidade.PRESENCIAL, 50, false, true);
        oferta(pedagogia, privada, "fpc:pedagogia", Modalidade.EAD, 200, null, true);

        mvc.perform(get("/api/v1/universidades/{id}", privada.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esfera").value(nullValue()))
                .andExpect(jsonPath("$.gratuita").value(false))
                .andExpect(jsonPath("$.vagasPorAno").value(250));
    }

    @Test
    void esferaAceitaOFormatoComAcentoEEspaco() throws Exception {
        Instituicao federal = instituicao(571, "UTFPR", TipoInstituicao.PUBLICA, "Pública Federal");
        Instituicao municipal = instituicao(572, "FAMU", TipoInstituicao.PUBLICA, "publica_municipal");

        mvc.perform(get("/api/v1/universidades/{id}", federal.getId()))
                .andExpect(jsonPath("$.esfera").value("federal"));

        mvc.perform(get("/api/v1/universidades/{id}", municipal.getId()))
                .andExpect(jsonPath("$.esfera").value("municipal"));
    }

    @Test
    void universidadeInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/v1/universidades/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void universidadeInativaResponde404() throws Exception {
        Instituicao inativa = instituicao(5555, "FF", TipoInstituicao.PRIVADA, "privada_sem_fins_lucrativos");
        inativa.setAtiva(false);
        instituicoes.saveAndFlush(inativa);

        mvc.perform(get("/api/v1/universidades/{id}", inativa.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private Area area(String codigoCine) {
        return areas.findAll().stream()
                .filter(a -> a.getCodigoCine().equals(codigoCine))
                .findFirst()
                .orElseThrow();
    }

    private Instituicao instituicao(int codigoEmec, String sigla, TipoInstituicao tipo, String categoriaAdministrativa) {
        Instituicao i = new Instituicao(codigoEmec, "Instituição " + sigla, tipo, "PR", "Maringá");
        i.setSigla(sigla);
        i.setCategoriaAdministrativa(categoriaAdministrativa);
        return instituicoes.save(i);
    }

    private void oferta(
            Curso curso,
            Instituicao instituicao,
            String chave,
            Modalidade modalidade,
            Integer vagas,
            Boolean gratuito,
            boolean ativa) {
        OfertaCurso o = new OfertaCurso(curso, instituicao, chave, curso.getNome());
        o.setOpcoes(List.of(new Opcao(Grau.BACHARELADO, "Integral")));
        o.setModalidade(modalidade);
        o.setDuracaoSemestres(8);
        o.setUf(instituicao.getUf());
        o.setCidade(instituicao.getCidade());
        o.setUrlOrigem("https://exemplo.br/" + chave.replace(':', '/'));
        o.setVagas(vagas);
        o.setGratuito(gratuito);
        o.setAtiva(ativa);
        ofertas.save(o);
    }
}