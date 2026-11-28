package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.sauf_api.calendario.CalendarioService;
import br.com.sauf_api.calendario.EventoRequest;
import br.com.sauf_api.calendario.TipoEvento;
import br.com.sauf_api.notificacao.GeradorNotificacoesCalendario;
import br.com.sauf_api.notificacao.Notificacao;
import br.com.sauf_api.notificacao.NotificacaoEventoGeradaRepository;
import br.com.sauf_api.notificacao.NotificacaoRepository;
import br.com.sauf_api.usuario.Usuario;
import br.com.sauf_api.usuario.UsuarioRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GeradorNotificacoesCalendarioTest {

    static final LocalDate HOJE = LocalDate.of(2026, 11, 23);

    @Autowired GeradorNotificacoesCalendario gerador;
    @Autowired CalendarioService calendario;
    @Autowired NotificacaoRepository notificacoes;
    @Autowired NotificacaoEventoGeradaRepository registros;
    @Autowired UsuarioRepository usuarios;

    UUID usuarioA;
    UUID usuarioB;

    @BeforeEach
    void preparar() {
        usuarioA = Sessao.USUARIO_TESTE;
        Usuario outro = usuarios.save(new Usuario("Outro", "outro@sauf.local", null));
        usuarioB = outro.getId();
    }

    @Test
    void geraUmAvisoPorUsuarioQuandoOPrazoEstaNaJanela() {
        criarEvento("enem-2026-prova", HOJE.plusDays(7), null);

        int geradas = gerador.executar(HOJE);

        assertThat(geradas).isEqualTo(2);
        List<Notificacao> doUsuarioA = notificacoes.findByUsuarioIdOrderByCriadaEmDesc(usuarioA);
        assertThat(doUsuarioA).hasSize(1);
        assertThat(doUsuarioA.get(0).getTitulo()).isEqualTo("Prova: ENEM 2026 - 1º dia");
        assertThat(doUsuarioA.get(0).getDescricao()).isEqualTo("Faltam 7 dias");
        assertThat(doUsuarioA.get(0).getAcaoRotulo()).isEqualTo("Ver calendário");
        assertThat(doUsuarioA.get(0).getAcaoPara()).isEqualTo("/calendario");
        assertThat(notificacoes.findByUsuarioIdOrderByCriadaEmDesc(usuarioB)).hasSize(1);
    }

    @Test
    void naoGeraDuasVezesParaOMesmoUsuarioEJanela() {
        criarEvento("enem-2026-prova", HOJE.plusDays(7), null);

        int primeiraRodada = gerador.executar(HOJE);
        int segundaRodada = gerador.executar(HOJE);

        assertThat(primeiraRodada).isEqualTo(2);
        assertThat(segundaRodada).isZero();
        assertThat(notificacoes.findByUsuarioIdOrderByCriadaEmDesc(usuarioA)).hasSize(1);
    }

    @Test
    void geraEmCadaJanelaAtravessadaPorDias() {
        criarEvento("sete-dias", HOJE.plusDays(7), null);
        criarEvento("tres-dias", HOJE.plusDays(3), null);
        criarEvento("amanha", HOJE.plusDays(1), null);
        criarEvento("hoje", HOJE, null);

        int geradas = gerador.executar(HOJE);

        assertThat(geradas).isEqualTo(8);
        List<Notificacao> doUsuarioA = notificacoes.findByUsuarioIdOrderByCriadaEmDesc(usuarioA);
        assertThat(doUsuarioA).hasSize(4);
        assertThat(doUsuarioA.stream().map(Notificacao::getDescricao))
                .containsExactlyInAnyOrder("Faltam 7 dias", "Faltam 3 dias", "Amanhã", "É hoje");
    }

    @Test
    void eventoForaDasJanelasNaoGera() {
        criarEvento("daqui-a-dez-dias", HOJE.plusDays(10), null);
        criarEvento("ontem", HOJE.minusDays(1), null);

        int geradas = gerador.executar(HOJE);

        assertThat(geradas).isZero();
        assertThat(registros.count()).isZero();
    }

    @Test
    void usaOPrazoDoFimDoPeriodo() {
        // Inscrição começa amanhã (1 dia) e termina em 7 dias - só a janela de 7 dias deve gerar aviso
        criarEvento("inscricao-periodo", HOJE.plusDays(1), HOJE.plusDays(7));

        int geradas = gerador.executar(HOJE);

        assertThat(geradas).isEqualTo(2);
        List<Notificacao> doUsuarioA = notificacoes.findByUsuarioIdOrderByCriadaEmDesc(usuarioA);
        assertThat(doUsuarioA).hasSize(1);
        assertThat(doUsuarioA.get(0).getDescricao()).isEqualTo("Faltam 7 dias");
    }

    @Test
    void usuarioNovoApareceNaProximaRodada() {
        criarEvento("enem-2026-prova", HOJE.plusDays(7), null);
        gerador.executar(HOJE);

        Usuario recemCadastrado = usuarios.save(new Usuario("Novo", "novo@sauf.local", null));
        int geradas = gerador.executar(HOJE);

        assertThat(geradas).isEqualTo(1);
        assertThat(notificacoes.findByUsuarioIdOrderByCriadaEmDesc(recemCadastrado.getId())).hasSize(1);
    }

    @Test
    void semUsuariosNadaAcontece() {
        usuarios.deleteAll();
        criarEvento("enem-2026-prova", HOJE.plusDays(7), null);

        int geradas = gerador.executar(HOJE);

        assertThat(geradas).isZero();
    }

    private void criarEvento(String chave, LocalDate inicio, LocalDate fim) {
        String titulo = chave.equals("enem-2026-prova") ? "ENEM 2026 - 1º dia" : chave;
        calendario.salvar(chave, new EventoRequest(
                titulo,
                TipoEvento.PROVA,
                inicio,
                fim,
                null,
                null,
                "https://enem.inep.gov.br/",
                2026));
    }
}
