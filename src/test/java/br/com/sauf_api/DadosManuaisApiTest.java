package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.area.AreaRepository;
import br.com.sauf_api.instituicao.Instituicao;
import br.com.sauf_api.instituicao.InstituicaoRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
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
class DadosManuaisApiTest {

    // PNG de 1x1 pixel
    static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");
    static final byte[] OUTRO_PNG = Arrays.copyOf(PNG, PNG.length + 1);

    @Autowired MockMvc mvc;
    @Autowired InstituicaoRepository instituicoes;
    @Autowired AreaRepository areas;

    Instituicao uem;

    @BeforeEach
    void censo() throws Exception {
        censoDeNovo();
        uem = instituicoes.findByCodigoEmec(57).orElseThrow();
    }

    @Test
    void putValidoPreencheDetalheListaECurso() throws Exception {
        enviar(57, uemCompleta(PNG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atualizados").value(1));

        mvc.perform(get("/api/v1/universidades/{id}", uem.getId()))
                .andExpect(jsonPath("$.anoFundacao").value(1969))
                .andExpect(jsonPath("$.sobre", contains("Primeiro parágrafo.", "Segundo parágrafo.")))
                .andExpect(jsonPath("$.siteUrl").value("https://www.uem.br"))
                .andExpect(jsonPath("$.logoUrl", startsWith("http://localhost/api/v1/universidades/" + uem.getId() + "/logo?v=")))
                .andExpect(jsonPath("$.capaUrl", startsWith("http://localhost/api/v1/universidades/" + uem.getId() + "/capa?v=")));
        mvc.perform(get("/api/v1/universidades").param("busca", "UEM"))
                .andExpect(jsonPath("$.content[0].logoUrl", containsString("/logo?v=")));
    }

    @Test
    void imagensSaoLidasSemLoginComCacheEEtag() throws Exception {
        enviar(57, uemCompleta(PNG));

        String etag = mvc.perform(get("/api/v1/universidades/{id}/logo", uem.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("Cache-Control", containsString("max-age=604800")))
                .andReturn().getResponse().getHeader("ETag");

        mvc.perform(get("/api/v1/universidades/{id}/logo", uem.getId()).header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    void semImagemDevolve404() throws Exception {
        mvc.perform(get("/api/v1/universidades/{id}/logo", uem.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/universidades/{id}/capa", uem.getId())).andExpect(status().isNotFound());
        var area = areas.findByCodigoCine("07").orElseThrow();
        mvc.perform(get("/api/v1/areas/{id}/imagem", area.getId())).andExpect(status().isNotFound());
    }

    @Test
    void reenviarIgualNaoMudaNadaEAUrlContinuaAMesma() throws Exception {
        enviar(57, uemCompleta(PNG));
        String antes = logoUrl();

        enviar(57, uemCompleta(PNG)).andExpect(jsonPath("$.atualizados").value(0));
        assertThat(logoUrl()).isEqualTo(antes);

        enviar(57, uemCompleta(OUTRO_PNG)).andExpect(jsonPath("$.atualizados").value(1));
        assertThat(logoUrl()).isNotEqualTo(antes);
    }

    @Test
    void imagemNullApagaAAtual() throws Exception {
        enviar(57, uemCompleta(PNG));
        enviar(57, "{\"anoFundacao\": 1969, \"sobre\": [\"Primeiro parágrafo.\"], \"logo\": null, \"capa\": null}")
                .andExpect(jsonPath("$.atualizados").value(1));

        mvc.perform(get("/api/v1/universidades/{id}", uem.getId()))
                .andExpect(jsonPath("$.logoUrl").value(nullValue()))
                .andExpect(jsonPath("$.capaUrl").value(nullValue()))
                .andExpect(jsonPath("$.siteUrl").value(nullValue()));
        mvc.perform(get("/api/v1/universidades/{id}/logo", uem.getId())).andExpect(status().isNotFound());
    }

    @Test
    void universidadeNaoCadastradaDevolve422() throws Exception {
        enviar(999999, uemCompleta(PNG))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value(
                        "Instituição com código e-MEC 999999 não está cadastrada. Rode o import do Censo antes."));
    }

    @Test
    void arquivoQueNaoEImagemDevolve400() throws Exception {
        String texto = Base64.getEncoder().encodeToString("<svg></svg>".getBytes());
        enviar(57, "{\"anoFundacao\": 1969, \"logo\": " + imagem(texto) + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.logo").value("A imagem deve ser JPG, PNG ou WebP."));
        enviar(57, "{\"anoFundacao\": 1969, \"logo\": " + imagem("@@@") + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.logo").value("O arquivo não está em base64 válido."));
    }

    @Test
    void imagemAcimaDe2MbDevolve400ENaoGravaNada() throws Exception {
        byte[] grande = Arrays.copyOf(PNG, 2 * 1024 * 1024 + 1);
        enviar(57, "{\"anoFundacao\": 1969, \"logo\": " + imagem(PNG) + ", \"capa\": " + imagem(grande) + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.capa").value("A imagem deve ter no máximo 2 MB."));

        mvc.perform(get("/api/v1/universidades/{id}", uem.getId()))
                .andExpect(jsonPath("$.anoFundacao").value(nullValue()))
                .andExpect(jsonPath("$.logoUrl").value(nullValue()));
    }

    @Test
    void camposInvalidosDevolvem400() throws Exception {
        enviar(57, "{\"anoFundacao\": 3000}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.anoFundacao").exists());
        enviar(57, "{\"sobre\": [\"   \"]}").andExpect(status().isBadRequest());
        enviar(57, "{\"siteUrl\": \"www.uem.br\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.siteUrl").exists());
        enviar(57, "{\"logo\": {\"conteudoBase64\": \"" + b64(PNG) + "\", \"autor\": \"\", \"licenca\": \"CC0\","
                        + " \"fonteUrl\": \"https://commons.wikimedia.org/wiki/File:X.png\"}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['logo.autor']").exists());
        enviar(57, "{\"sobre\": [\"um\\n\\ndois\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['sobre[0]']").exists());
    }

    @Test
    void censoDeNovoNaoApagaOsDadosManuais() throws Exception {
        enviar(57, uemCompleta(PNG));

        censoDeNovo();

        mvc.perform(get("/api/v1/universidades/{id}", uem.getId()))
                .andExpect(jsonPath("$.anoFundacao").value(1969))
                .andExpect(jsonPath("$.sobre[0]").value("Primeiro parágrafo."))
                .andExpect(jsonPath("$.siteUrl").value("https://www.uem.br"))
                .andExpect(jsonPath("$.logoUrl", containsString("/logo?v=")));
    }

    @Test
    void imagemDaAreaViraAImagemDosCursosDaArea() throws Exception {
        var engenharia = areas.findByCodigoCine("07").orElseThrow();
        mvc.perform(put("/api/v1/ingestao/areas/07/imagem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Api-Key", "chave-de-teste")
                        .content(imagem(PNG)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atualizados").value(1));

        mvc.perform(post("/api/v1/ingestao/lotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Api-Key", "chave-de-teste")
                        .content(IngestaoApiTest.LOTE))
                .andExpect(status().isOk());

        String cursos = mvc.perform(get("/api/v1/cursos").param("size", "50"))
                .andReturn().getResponse().getContentAsString();
        List<String> daEngenharia = JsonPath.read(
                cursos, "$.content[?(@.area.id == '" + engenharia.getId() + "')].imagemUrl");
        List<Object> deOutrasAreas = JsonPath.read(
                cursos, "$.content[?(@.area.id != '" + engenharia.getId() + "')].imagemUrl");
        assertThat(daEngenharia).isNotEmpty()
                .allSatisfy(url -> assertThat(url)
                        .startsWith("http://localhost/api/v1/areas/" + engenharia.getId() + "/imagem?v="));
        assertThat(deOutrasAreas).isNotEmpty().containsOnlyNulls();

        mvc.perform(get(daEngenharia.get(0).replace("http://localhost", "")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG));
    }

    @Test
    void areaNaoCadastradaDevolve422() throws Exception {
        mvc.perform(put("/api/v1/ingestao/areas/99/imagem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Api-Key", "chave-de-teste")
                        .content(imagem(PNG)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value(
                        "Área Cine 99 não está cadastrada. Rode o import do Censo antes."));
    }

    @Test
    void semApiKeyDevolve401() throws Exception {
        mvc.perform(put("/api/v1/ingestao/universidades/57")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(uemCompleta(PNG)))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/ingestao/areas/07/imagem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imagem(PNG)))
                .andExpect(status().isUnauthorized());
    }

    private void censoDeNovo() throws Exception {
        mvc.perform(post("/api/v1/ingestao/censo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Api-Key", "chave-de-teste")
                        .content(IngestaoApiTest.CENSO))
                .andExpect(status().isOk());
    }

    private String logoUrl() throws Exception {
        return JsonPath.read(mvc.perform(get("/api/v1/universidades/{id}", uem.getId()))
                .andReturn().getResponse().getContentAsString(), "$.logoUrl");
    }

    private ResultActions enviar(int codigoEmec, String corpo) throws Exception {
        return mvc.perform(put("/api/v1/ingestao/universidades/{codigo}", codigoEmec)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", "chave-de-teste")
                .content(corpo));
    }

    static String uemCompleta(byte[] logo) {
        return """
                {"anoFundacao": 1969,
                 "sobre": ["Primeiro parágrafo.", "Segundo parágrafo."],
                 "siteUrl": "https://www.uem.br",
                 "logo": %s,
                 "capa": %s}
                """.formatted(imagem(logo), imagem(PNG));
    }

    static String imagem(byte[] conteudo) {
        return imagem(b64(conteudo));
    }

    static String imagem(String base64) {
        return """
                {"conteudoBase64": "%s", "autor": "Fulano", "licenca": "CC BY-SA 4.0",
                 "licencaUrl": "https://creativecommons.org/licenses/by-sa/4.0",
                 "fonteUrl": "https://commons.wikimedia.org/wiki/File:Exemplo.png"}
                """.formatted(base64);
    }

    static String b64(byte[] conteudo) {
        return Base64.getEncoder().encodeToString(conteudo);
    }
}
