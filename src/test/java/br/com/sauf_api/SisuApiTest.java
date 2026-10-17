package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.instituicao.InstituicaoRepository;
import br.com.sauf_api.oferta.OfertaCurso;
import br.com.sauf_api.oferta.OfertaCursoRepository;
import br.com.sauf_api.sisu.NotaCorteSisuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class SisuApiTest {

    // Física da UEM: uma oferta com dois códigos e-MEC (licenciatura 3405, bacharelado 303405).
    static final String SISU_2025 = """
            {"ano": 2025, "linhas": [
              {"codigoEmecInstituicao": 57, "codigoEmecCurso": 3405, "turno": "Noturno",
               "vagas": 10, "nota": 700.50, "inscricoes": 90},
              {"codigoEmecInstituicao": 57, "codigoEmecCurso": 303405, "turno": "Vespertino",
               "vagas": 8, "nota": 650.25, "inscricoes": 40},
              {"codigoEmecInstituicao": 57, "codigoEmecCurso": 3402, "turno": "Integral",
               "vagas": 20, "nota": null, "inscricoes": 15},
              {"codigoEmecInstituicao": 57, "codigoEmecCurso": 777777, "turno": "Matutino",
               "vagas": 5, "nota": 600.00, "inscricoes": 30}
            ]}
            """;

    @Autowired MockMvc mvc;
    @Autowired InstituicaoRepository instituicoes;
    @Autowired OfertaCursoRepository ofertas;
    @Autowired NotaCorteSisuRepository notas;

    @BeforeEach
    void censoESite() throws Exception {
        enviar("/api/v1/ingestao/censo", IngestaoApiTest.CENSO).andExpect(status().isOk());
        enviar("/api/v1/ingestao/lotes", IngestaoApiTest.LOTE).andExpect(status().isOk());
    }

    @Test
    void importaEContaLinhasSemOferta() throws Exception {
        sisu(SISU_2025)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criados").value(4))
                .andExpect(jsonPath("$.atualizados").value(0))
                .andExpect(jsonPath("$.semOferta").value(1));
    }

    @Test
    void ofertaMostraOMenorCorteComTurnoEAno() throws Exception {
        sisu(SISU_2025);
        OfertaCurso fisica = oferta("uem:fisica:maringa");

        mvc.perform(get("/api/v1/cursos/{id}/ofertas", fisica.getCurso().getId()))
                .andExpect(jsonPath("$.content[0].notaCorteSisu").value(650.25))
                .andExpect(jsonPath("$.content[0].notaCorteSisuTurno").value("Vespertino"))
                .andExpect(jsonPath("$.content[0].notaCorteSisuAno").value(2025));
    }

    @Test
    void cursoGenericoMostraAMediaDosPares() throws Exception {
        sisu(SISU_2025);
        OfertaCurso fisica = oferta("uem:fisica:maringa");

        mvc.perform(get("/api/v1/cursos/{id}", fisica.getCurso().getId()))
                .andExpect(jsonPath("$.notaCorteSisuMedia").value(675.38))
                .andExpect(jsonPath("$.notaCorteSisuAno").value(2025));
    }

    @Test
    void corteNullViraOfertaSemNota() throws Exception {
        sisu(SISU_2025);
        OfertaCurso civil = oferta("uem:engenharia-civil:maringa:integral");

        mvc.perform(get("/api/v1/cursos/{id}/ofertas", civil.getCurso().getId()))
                .andExpect(jsonPath("$.content[0].notaCorteSisu").doesNotExist())
                .andExpect(jsonPath("$.content[0].notaCorteSisuTurno").doesNotExist());
        mvc.perform(get("/api/v1/cursos/{id}", civil.getCurso().getId()))
                .andExpect(jsonPath("$.notaCorteSisuMedia").doesNotExist());
    }

    @Test
    void usaOAnoMaisRecenteComNota() throws Exception {
        sisu(linha(2024, 3402, "Integral", "610.00"));
        sisu(linha(2025, 3402, "Integral", "720.00"));
        sisu(linha(2026, 3402, "Integral", "null"));
        OfertaCurso civil = oferta("uem:engenharia-civil:maringa:integral");

        mvc.perform(get("/api/v1/cursos/{id}/ofertas", civil.getCurso().getId()))
                .andExpect(jsonPath("$.content[0].notaCorteSisu").value(720.00))
                .andExpect(jsonPath("$.content[0].notaCorteSisuAno").value(2025));
        assertThat(notas.count()).isEqualTo(3);
    }

    @Test
    void reenviarOMesmoAnoAtualiza() throws Exception {
        sisu(SISU_2025);
        sisu(SISU_2025.replace("700.50", "705.00"))
                .andExpect(jsonPath("$.criados").value(0))
                .andExpect(jsonPath("$.atualizados").value(4));

        assertThat(notas.count()).isEqualTo(4);
        assertThat(notas.findByCodigoEmecCursoAndTurnoAndAno(3405, "Noturno", 2025).orElseThrow().getNota())
                .isEqualByComparingTo("705.00");
    }

    @Test
    void instituicaoNaoCadastradaDevolve422() throws Exception {
        sisu(SISU_2025.replaceFirst("\"codigoEmecInstituicao\": 57", "\"codigoEmecInstituicao\": 999999"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value(
                        "Instituição com código e-MEC 999999 não está cadastrada. Rode o import do Censo antes."));
        assertThat(notas.count()).isZero();
    }

    @Test
    void linhaInvalidaDevolve400() throws Exception {
        sisu(linha(2025, 3402, "", "700.00")).andExpect(status().isBadRequest());
        sisu(linha(2025, 3402, "Integral", "-1")).andExpect(status().isBadRequest());
    }

    @Test
    void semApiKeyDevolve401() throws Exception {
        mvc.perform(post("/api/v1/ingestao/sisu").contentType(MediaType.APPLICATION_JSON).content(SISU_2025))
                .andExpect(status().isUnauthorized());
    }

    private static String linha(int ano, int codigoCurso, String turno, String nota) {
        return """
                {"ano": %d, "linhas": [{"codigoEmecInstituicao": 57, "codigoEmecCurso": %d,
                  "turno": "%s", "vagas": 10, "nota": %s, "inscricoes": 50}]}
                """.formatted(ano, codigoCurso, turno, nota);
    }

    private OfertaCurso oferta(String chave) {
        var uem = instituicoes.findByCodigoEmec(57).orElseThrow();
        return ofertas.findByInstituicaoIdAndChave(uem.getId(), chave).orElseThrow();
    }

    private ResultActions sisu(String corpo) throws Exception {
        return enviar("/api/v1/ingestao/sisu", corpo);
    }

    private ResultActions enviar(String rota, String corpo) throws Exception {
        return mvc.perform(post(rota)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", "chave-de-teste")
                .content(corpo));
    }
}
