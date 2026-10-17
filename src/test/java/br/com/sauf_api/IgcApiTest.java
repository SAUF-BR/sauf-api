package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.instituicao.IndicadorIesRepository;
import br.com.sauf_api.instituicao.Instituicao;
import br.com.sauf_api.instituicao.InstituicaoRepository;
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
class IgcApiTest {

    @Autowired MockMvc mvc;
    @Autowired InstituicaoRepository instituicoes;
    @Autowired IndicadorIesRepository indicadores;

    Instituicao uem;

    @BeforeEach
    void censo() throws Exception {
        mvc.perform(post("/api/v1/ingestao/censo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Api-Key", "chave-de-teste")
                        .content(IngestaoApiTest.CENSO))
                .andExpect(status().isOk());
        uem = instituicoes.findByCodigoEmec(57).orElseThrow();
    }

    @Test
    void igcViraANotaMecDaUniversidade() throws Exception {
        igc(2023, "{\"codigoEmec\": 57, \"igcContinuo\": 3.946, \"igcFaixa\": 5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criados").value(1));

        mvc.perform(get("/api/v1/universidades/{id}", uem.getId()))
                .andExpect(jsonPath("$.notaMec").value(5));
        mvc.perform(get("/api/v1/universidades").param("busca", "UEM"))
                .andExpect(jsonPath("$.content[0].notaMec").value(5));
    }

    @Test
    void guardaCadaAnoEMostraOMaisRecente() throws Exception {
        igc(2023, "{\"codigoEmec\": 57, \"igcContinuo\": 3.946, \"igcFaixa\": 5}");
        igc(2022, "{\"codigoEmec\": 57, \"igcContinuo\": 2.9, \"igcFaixa\": 3}");

        assertThat(instituicoes.findByCodigoEmec(57).orElseThrow().getNotaMec()).isEqualTo(5);
        assertThat(indicadores.count()).isEqualTo(2);

        igc(2024, "{\"codigoEmec\": 57, \"igcContinuo\": null, \"igcFaixa\": null}");
        assertThat(instituicoes.findByCodigoEmec(57).orElseThrow().getNotaMec()).isNull();
    }

    @Test
    void reenviarOMesmoAnoAtualiza() throws Exception {
        igc(2023, "{\"codigoEmec\": 57, \"igcContinuo\": 3.5, \"igcFaixa\": 4}");
        igc(2023, "{\"codigoEmec\": 57, \"igcContinuo\": 3.946, \"igcFaixa\": 5}")
                .andExpect(jsonPath("$.criados").value(0))
                .andExpect(jsonPath("$.atualizados").value(1));

        assertThat(indicadores.count()).isEqualTo(1);
        assertThat(indicadores.findByInstituicaoIdAndAno(uem.getId(), 2023).orElseThrow().getIgcFaixa())
                .isEqualTo(5);
    }

    @Test
    void instituicaoNaoCadastradaDevolve422() throws Exception {
        igc(2023, "{\"codigoEmec\": 999999, \"igcContinuo\": 3.0, \"igcFaixa\": 4}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value(
                        "Instituição com código e-MEC 999999 não está cadastrada. Rode o import do Censo antes."));
    }

    @Test
    void faixaForaDe1a5Devolve400() throws Exception {
        igc(2023, "{\"codigoEmec\": 57, \"igcContinuo\": 3.0, \"igcFaixa\": 6}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void semApiKeyDevolve401() throws Exception {
        mvc.perform(post("/api/v1/ingestao/igc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ano\": 2023, \"instituicoes\": []}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions igc(int ano, String instituicao) throws Exception {
        return mvc.perform(post("/api/v1/ingestao/igc")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", "chave-de-teste")
                .content("{\"ano\": " + ano + ", \"instituicoes\": [" + instituicao + "]}"));
    }
}
