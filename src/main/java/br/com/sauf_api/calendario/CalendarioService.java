package br.com.sauf_api.calendario;

import br.com.sauf_api.comum.DadosInvalidosException;
import br.com.sauf_api.comum.RecursoNaoEncontradoException;
import br.com.sauf_api.ingestao.InstituicaoNaoCadastradaException;
import br.com.sauf_api.ingestao.RespostaIngestao;
import br.com.sauf_api.instituicao.Instituicao;
import br.com.sauf_api.instituicao.InstituicaoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calendário de vestibulares (D-104 a D-106). As datas entram pelo scraper ou pelas futuras telas de
 * admin (PUT por chave, idempotente) e são lidas pela tela de calendário, pelos prazos da Home, pelo
 * detalhe da universidade e pelas notificações ({@link #proximosEventos}).
 */
@Service
public class CalendarioService {

    private static final Logger log = LoggerFactory.getLogger(CalendarioService.class);

    /** "Hoje" é o dia em Brasília: às 22h de 10/12 em São Paulo ainda é 10/12 para o aluno. */
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private static final Pattern CHAVE = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final int TAMANHO_MAXIMO_CHAVE = 100;

    private final EventoCalendarioRepository eventos;
    private final InstituicaoRepository instituicoes;
    private final Clock relogio;

    public CalendarioService(EventoCalendarioRepository eventos, InstituicaoRepository instituicoes, Clock relogio) {
        this.eventos = eventos;
        this.instituicoes = instituicoes;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio.withZone(FUSO));
    }

    @Transactional(readOnly = true)
    public List<EventoResposta> doAno(int ano) {
        return eventosEntre(LocalDate.of(ano, 1, 1), LocalDate.of(ano, 12, 31));
    }

    /** Eventos com algum dia entre as duas datas (inclusive); um período aparece se cruzar o intervalo. */
    @Transactional(readOnly = true)
    public List<EventoResposta> eventosEntre(LocalDate de, LocalDate ate) {
        return eventos.noPeriodo(de, ate).stream().map(EventoResposta::de).toList();
    }

    /**
     * Para as notificações: o que acontece de hoje até {@code ate}, inclusive os períodos já
     * abertos (inscrição que termina na semana). Só consulta; quem avisa é o módulo de notificações.
     */
    @Transactional(readOnly = true)
    public List<EventoResposta> proximosEventos(LocalDate ate) {
        return eventosEntre(hoje(), ate);
    }

    @Transactional(readOnly = true)
    public List<PrazoResposta> proximosPrazos(int limite) {
        LocalDate hoje = hoje();
        return eventos.comPrazoAPartirDe(hoje, PageRequest.of(0, limite)).stream()
                .map(e -> PrazoResposta.de(e, hoje))
                .toList();
    }

    /** O próximo prazo da universidade, para o detalhe dela; vazio se não houver evento por vir. */
    @Transactional(readOnly = true)
    public Optional<EventoCalendario> proximoPrazoDaInstituicao(UUID instituicaoId) {
        return eventos.daInstituicaoComPrazoAPartirDe(instituicaoId, hoje(), PageRequest.of(0, 1)).stream().findFirst();
    }

    @Transactional
    public RespostaIngestao salvar(String chave, EventoRequest dados) {
        validarChave(chave);
        if (dados.fim() != null && dados.fim().isBefore(dados.inicio())) {
            throw new DadosInvalidosException("fim", "O fim não pode ser antes do início");
        }
        Instituicao instituicao = dados.codigoEmecInstituicao() == null
                ? null
                : instituicoes.findByCodigoEmec(dados.codigoEmecInstituicao())
                        .orElseThrow(() -> new InstituicaoNaoCadastradaException(dados.codigoEmecInstituicao()));

        Optional<EventoCalendario> existente = eventos.findByChave(chave);
        EventoCalendario evento = existente.orElseGet(() -> new EventoCalendario(chave));
        boolean mudou = evento.aplicar(
                dados.titulo().trim(),
                dados.tipo(),
                dados.inicio(),
                dados.fim(),
                instituicao,
                vazioComoNull(dados.descricao()),
                dados.fonteUrl().trim(),
                dados.anoReferencia(),
                relogio.instant());

        RespostaIngestao resposta = new RespostaIngestao();
        if (existente.isEmpty()) {
            eventos.save(evento);
            resposta.criado();
        } else if (mudou) {
            resposta.atualizado();
        }
        log.info("evento do calendário: chave={} novo={} mudou={}", chave, existente.isEmpty(), mudou);
        return resposta;
    }

    @Transactional
    public void remover(String chave) {
        EventoCalendario evento = eventos.findByChave(chave)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento do calendário não encontrado: " + chave));
        eventos.delete(evento);
        log.info("evento do calendário removido: chave={}", chave);
    }

    private static void validarChave(String chave) {
        if (chave.length() > TAMANHO_MAXIMO_CHAVE || !CHAVE.matcher(chave).matches()) {
            throw new DadosInvalidosException(
                    "chave", "Use só letras minúsculas, números e hífen, até 100 caracteres (ex.: enem-2026-prova-1)");
        }
    }

    private static String vazioComoNull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
