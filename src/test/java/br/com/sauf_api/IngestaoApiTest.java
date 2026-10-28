package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.censo.CensoCursoRepository;
import br.com.sauf_api.curso.CursoRepository;
import br.com.sauf_api.instituicao.InstituicaoRepository;
import br.com.sauf_api.oferta.Grau;
import br.com.sauf_api.oferta.OfertaCurso;
import br.com.sauf_api.oferta.OfertaCursoRepository;
import br.com.sauf_api.oferta.Opcao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class IngestaoApiTest {

    static final String CENSO = """
            {
              "anoCenso": 2024,
              "instituicoes": [{
                "codigoEmec": 57, "nome": "UNIVERSIDADE ESTADUAL DE MARINGÁ", "sigla": "UEM",
                "tipo": "publica", "categoriaAdministrativa": "publica_estadual",
                "organizacaoAcademica": "universidade", "uf": "PR", "cidade": "Maringá",
                "endereco": {"logradouro": "Avenida Colombo", "numero": "5790",
                             "complemento": "Campus Universitário", "bairro": "Zona 7", "cep": "87020900"}
              }],
              "ofertas": [
                {"codigoEmec": 3402, "codigoEmecInstituicao": 57, "nome": "Engenharia Civil",
                 "grau": "bacharelado", "modalidade": "presencial", "cidade": "Maringá", "uf": "PR",
                 "vagas": 80, "gratuito": true, "codigoCineRotulo": "0732E01",
                 "nomeCineRotulo": "Engenharia civil", "codigoCineArea": "07"},
                {"codigoEmec": 3405, "codigoEmecInstituicao": 57, "nome": "Física",
                 "grau": "licenciatura", "modalidade": "presencial", "cidade": "Maringá", "uf": "PR",
                 "vagas": 53, "gratuito": true, "codigoCineRotulo": "0114F01",
                 "nomeCineRotulo": "Física formação de professor", "codigoCineArea": "01"},
                {"codigoEmec": 303405, "codigoEmecInstituicao": 57, "nome": "Física",
                 "grau": "bacharelado", "modalidade": "presencial", "cidade": "Maringá", "uf": "PR",
                 "vagas": 45, "gratuito": true, "codigoCineRotulo": "0533F01",
                 "nomeCineRotulo": "Física", "codigoCineArea": "05"},
                {"codigoEmec": 9001, "codigoEmecInstituicao": 9, "nome": "Medicina",
                 "codigoCineRotulo": "0912M01", "nomeCineRotulo": "Medicina", "codigoCineArea": "09"}
              ]
            }
            """;

    static final String LOTE = """
            {
              "execucaoId": "t1",
              "fonte": {"url": "https://www.pen.uem.br/site/public/cursos", "coletadoEm": "2026-10-08T12:00:00Z"},
              "codigoEmecInstituicao": 57,
              "cursos": [
                {"chave": "uem:engenharia-civil:maringa:integral", "codigosEmec": [3402],
                 "urlOrigem": "https://www.pen.uem.br/site/public/curso/abc", "nome": "Engenharia Civil",
                 "opcoes": [{"grau": "bacharelado", "turno": "Integral"}],
                 "modalidade": "presencial", "turno": "Integral",
                 "duracaoTexto": "5 anos", "duracaoSemestres": 10, "cidade": "Maringá", "uf": "PR"},
                {"chave": "uem:fisica:maringa", "codigosEmec": [3405, 303405],
                 "urlOrigem": "https://www.pen.uem.br/site/public/curso/fis", "nome": "Física",
                 "sobre": "Forma físicos.\\n- Bacharelado: pesquisa.\\n\\nLicenciatura: ensino.",
                 "mercadoTrabalho": "Atua em laboratórios.\\n\\nE em escolas.",
                 "opcoes": [{"grau": "licenciatura", "turno": "Noturno"},
                            {"grau": "bacharelado", "turno": "Vespertino/Noturno"}],
                 "modalidade": "presencial", "cidade": "Maringá", "uf": "PR"},
                {"chave": "uem:engenharia-de-software:maringa:noturno",
                 "urlOrigem": "https://www.pen.uem.br/site/public/curso/def", "nome": "Engenharia de Software",
                 "opcoes": [{"grau": "bacharelado", "turno": "Noturno"}],
                 "modalidade": "presencial", "turno": "Noturno",
                 "duracaoTexto": "5 anos", "duracaoSemestres": 10, "cidade": "Maringá", "uf": "PR"}
              ]
            }
            """;

    @Autowired MockMvc mvc;
    @Autowired InstituicaoRepository instituicoes;
    @Autowired CursoRepository cursos;
    @Autowired CensoCursoRepository censoCursos;
    @Autowired OfertaCursoRepository ofertas;

    @Test
    void semApiKeyRecusa() throws Exception {
        mvc.perform(post("/api/v1/ingestao/censo").contentType(MediaType.APPLICATION_JSON).content(CENSO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mvc.perform(post("/api/v1/ingestao/censo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Api-Key", "errada")
                        .content(CENSO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void censoGuardaOEnderecoDaSede() throws Exception {
        censo().andExpect(status().isOk());
        var uem = instituicoes.findByCodigoEmec(57).orElseThrow();

        mvc.perform(get("/api/v1/universidades/{id}", uem.getId()))
                .andExpect(jsonPath("$.enderecoSede.logradouro").value("Avenida Colombo"))
                .andExpect(jsonPath("$.enderecoSede.numero").value("5790"))
                .andExpect(jsonPath("$.enderecoSede.complemento").value("Campus Universitário"))
                .andExpect(jsonPath("$.enderecoSede.bairro").value("Zona 7"))
                .andExpect(jsonPath("$.enderecoSede.cep").value("87020900"))
                .andExpect(jsonPath("$.enderecoSede.cidade").value("Maringá"))
                .andExpect(jsonPath("$.enderecoSede.uf").value("PR"));
    }

    @Test
    void censoSemEnderecoMantemCidadeEUf() throws Exception {
        mvc.perform(enviar("/api/v1/ingestao/censo", CENSO.replaceAll("(?s),\\s*\"endereco\": \\{[^}]*}", "")))
                .andExpect(status().isOk());
        var uem = instituicoes.findByCodigoEmec(57).orElseThrow();

        mvc.perform(get("/api/v1/universidades/{id}", uem.getId()))
                .andExpect(jsonPath("$.enderecoSede.logradouro").doesNotExist())
                .andExpect(jsonPath("$.enderecoSede.cep").doesNotExist())
                .andExpect(jsonPath("$.enderecoSede.cidade").value("Maringá"))
                .andExpect(jsonPath("$.enderecoSede.uf").value("PR"));
    }

    @Test
    void cepComMascaraDevolve400() throws Exception {
        mvc.perform(enviar("/api/v1/ingestao/censo", CENSO.replace("\"87020900\"", "\"87020-900\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void censoGuardaOsCursosSemCriarOfertas() throws Exception {
        censo()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criados").value(4))
                .andExpect(jsonPath("$.rejeitados.length()").value(1));

        assertThat(instituicoes.findByCodigoEmec(57)).get()
                .satisfies(i -> assertThat(i.getSigla()).isEqualTo("UEM"));
        assertThat(censoCursos.count()).isEqualTo(3);
        assertThat(ofertas.count()).isZero();
        mvc.perform(get("/api/v1/cursos")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void licenciaturaEntraNoMesmoCursoGenericoComAreaDoBacharelado() throws Exception {
        censo();

        var fisicaLic = censoCursos.findById(3405).orElseThrow().getCurso();
        var fisicaBach = censoCursos.findById(303405).orElseThrow().getCurso();
        assertThat(fisicaLic.getId()).isEqualTo(fisicaBach.getId());
        assertThat(fisicaBach.getNome()).isEqualTo("Física");
        assertThat(fisicaBach.getArea().getCodigoCine()).isEqualTo("05");
    }

    @Test
    void censoDeNovoAtualizaSemDuplicar() throws Exception {
        censo();
        censo().andExpect(jsonPath("$.criados").value(0)).andExpect(jsonPath("$.atualizados").value(4));
        assertThat(censoCursos.count()).isEqualTo(3);
    }

    @Test
    void umaPaginaViraUmaOfertaComOpcoesCompletadaPeloCenso() throws Exception {
        censo();
        enviarLote(LOTE).andExpect(status().isOk()).andExpect(jsonPath("$.criados").value(3));

        OfertaCurso fisica = oferta("uem:fisica:maringa");
        assertThat(fisica.getCodigosEmec()).containsExactly(3405, 303405);
        assertThat(fisica.getOpcoes()).extracting(Opcao::getGrau)
                .containsExactly(Grau.LICENCIATURA, Grau.BACHARELADO);
        assertThat(fisica.getCurso().getNome()).isEqualTo("Física");
        assertThat(fisica.getVagas()).isEqualTo(98);
        assertThat(fisica.getGratuito()).isTrue();

        mvc.perform(get("/api/v1/cursos/{id}/ofertas", fisica.getCurso().getId()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].opcoes[0].grau").value("licenciatura"))
                .andExpect(jsonPath("$.content[0].opcoes[1].turno").value("Vespertino/Noturno"))
                .andExpect(jsonPath("$.content[0].codigosEmec").doesNotExist())
                .andExpect(jsonPath("$.content[0].sobre")
                        .value("Forma físicos.\n- Bacharelado: pesquisa.\n\nLicenciatura: ensino."))
                .andExpect(jsonPath("$.content[0].mercadoTrabalho").value("Atua em laboratórios.\n\nE em escolas."))
                .andExpect(jsonPath("$.content[0].urlOrigem").value("https://www.pen.uem.br/site/public/curso/fis"));

        mvc.perform(get("/api/v1/cursos").param("grau", "licenciatura"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Física"))
                .andExpect(jsonPath("$.content[0].graus", containsInAnyOrder("licenciatura", "bacharelado")));
    }

    @Test
    void cursoForaDoCensoEntraComCursoGenericoPendenteDeRevisao() throws Exception {
        censo();
        enviarLote(LOTE);

        OfertaCurso software = oferta("uem:engenharia-de-software:maringa:noturno");
        assertThat(software.getCodigosEmec()).isEmpty();
        assertThat(software.getCurso().isPendenteRevisao()).isTrue();
        assertThat(software.getCurso().getArea()).isNull();

        mvc.perform(get("/api/v1/cursos").param("busca", "software"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].area").doesNotExist());
    }

    @Test
    void censoDepoisDoSiteNaoSobrescreveOSiteMasAtualizaOComplemento() throws Exception {
        censo();
        enviarLote(LOTE.replaceFirst("\"nome\": \"Engenharia Civil\"", "\"nome\": \"Eng. Civil\""))
                .andExpect(status().isOk());
        censo().andExpect(status().isOk());

        OfertaCurso civil = oferta("uem:engenharia-civil:maringa:integral");
        assertThat(civil.getNomeNoSite()).isEqualTo("Eng. Civil");
        assertThat(civil.getVagas()).isEqualTo(80);
    }

    @Test
    void paginaSemGrauRecebeOsGrausDoCenso() throws Exception {
        censo();
        enviarLote(LOTE.replace(
                "\"opcoes\": [{\"grau\": \"licenciatura\", \"turno\": \"Noturno\"},\n"
                        + "                            {\"grau\": \"bacharelado\", \"turno\": \"Vespertino/Noturno\"}],",
                "\"opcoes\": [],"))
                .andExpect(status().isOk());

        assertThat(oferta("uem:fisica:maringa").getOpcoes()).extracting(Opcao::getGrau)
                .containsExactlyInAnyOrder(Grau.LICENCIATURA, Grau.BACHARELADO);
    }

    @Test
    void sobreVemDoSiteENaoEApagadoPeloCenso() throws Exception {
        censo();
        enviarLote(LOTE);
        assertThat(oferta("uem:fisica:maringa").getSobre()).startsWith("Forma físicos.");
        assertThat(oferta("uem:engenharia-de-software:maringa:noturno").getSobre()).isNull();

        censo();
        assertThat(oferta("uem:fisica:maringa").getSobre()).startsWith("Forma físicos.");

        enviarLote(LOTE.replace("\"sobre\": \"Forma físicos.", "\"sobre\": \"Texto novo."));
        assertThat(oferta("uem:fisica:maringa").getSobre()).startsWith("Texto novo.");
    }

    @Test
    void mercadoDeTrabalhoVemDoSiteENaoEApagadoPeloCenso() throws Exception {
        censo();
        enviarLote(LOTE);
        assertThat(oferta("uem:fisica:maringa").getMercadoTrabalho()).isEqualTo("Atua em laboratórios.\n\nE em escolas.");
        assertThat(oferta("uem:engenharia-de-software:maringa:noturno").getMercadoTrabalho()).isNull();

        censo();
        assertThat(oferta("uem:fisica:maringa").getMercadoTrabalho()).startsWith("Atua em laboratórios.");
    }

    @Test
    void ofertaQueSomeDoSiteFicaInativa() throws Exception {
        censo();
        enviarLote(LOTE);
        String semSoftware = LOTE.substring(0, LOTE.indexOf(",\n    {\"chave\": \"uem:engenharia-de-software"))
                + "\n  ]\n}";

        enviarLote(semSoftware).andExpect(status().isOk()).andExpect(jsonPath("$.inativados").value(1));

        mvc.perform(get("/api/v1/cursos").param("busca", "software"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void loteDeInstituicaoNaoCadastradaEhRecusado() throws Exception {
        enviarLote(LOTE)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("57")));
    }

    @Test
    void loteInvalidoDevolve400ComOsCampos() throws Exception {
        censo();
        enviarLote(LOTE.replace("\"uf\": \"PR\"}", "\"uf\": \"pr\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields").isMap());
    }

    private OfertaCurso oferta(String chave) {
        var uem = instituicoes.findByCodigoEmec(57).orElseThrow();
        return ofertas.findByInstituicaoIdAndChave(uem.getId(), chave).orElseThrow();
    }

    private ResultActions censo() throws Exception {
        return mvc.perform(enviar("/api/v1/ingestao/censo", CENSO));
    }

    private ResultActions enviarLote(String corpo) throws Exception {
        return mvc.perform(enviar("/api/v1/ingestao/lotes", corpo));
    }

    private static MockHttpServletRequestBuilder enviar(String rota, String corpo) {
        return post(rota)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", "chave-de-teste")
                .content(corpo);
    }
}
