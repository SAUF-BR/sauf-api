package br.com.sauf_api.comum;

import br.com.sauf_api.auth.CredenciaisInvalidasException;
import br.com.sauf_api.auth.EmailEmUsoException;
import br.com.sauf_api.documento.DocumentoDesatualizadoException;
import br.com.sauf_api.ingestao.InstituicaoNaoCadastradaException;
import br.com.sauf_api.usuario.FotoInvalidaException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroResposta> naoEncontrado(RecursoNaoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage(), Map.of());
    }

    @ExceptionHandler(InstituicaoNaoCadastradaException.class)
    ResponseEntity<ErroResposta> instituicaoNaoCadastrada(InstituicaoNaoCadastradaException e) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage(), Map.of());
    }

    @ExceptionHandler(AcaoNaoPermitidaException.class)
    ResponseEntity<ErroResposta> acaoNaoPermitida(AcaoNaoPermitidaException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage(), Map.of());
    }

    @ExceptionHandler(DadosInvalidosException.class)
    ResponseEntity<ErroResposta> dadosInvalidos(DadosInvalidosException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage(), e.campos());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResposta> corpoIlegivel(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErroResposta> rotaInexistente(NoResourceFoundException e) {
        return responder(HttpStatus.NOT_FOUND, "Rota não encontrada: /" + e.getResourcePath(), Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroResposta> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return responder(
                HttpStatus.BAD_REQUEST,
                "Parâmetro inválido",
                Map.of(e.getName(), "valor inválido: " + e.getValue()));
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ResponseEntity<ErroResposta> ordenacaoInvalida(PropertyReferenceException e) {
        return responder(
                HttpStatus.BAD_REQUEST,
                "Parâmetro inválido",
                Map.of("sort", "campo de ordenação inexistente: " + e.getPropertyName()));
    }

    @ExceptionHandler({CredenciaisInvalidasException.class, NaoAutenticadoException.class})
    ResponseEntity<ErroResposta> naoAutenticado(RuntimeException e) {
        return responder(HttpStatus.UNAUTHORIZED, e.getMessage(), Map.of());
    }

    @ExceptionHandler(CampoInvalidoException.class)
    ResponseEntity<ErroResposta> campoInvalido(CampoInvalidoException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage(), Map.of(e.getCampo(), e.getMessage()));
    }

    @ExceptionHandler(DocumentoDesatualizadoException.class)
    ResponseEntity<ErroResposta> documentoDesatualizado(DocumentoDesatualizadoException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage(), Map.of("aceites", e.getMessage()));
    }

    @ExceptionHandler(EmailEmUsoException.class)
    ResponseEntity<ErroResposta> emailEmUso(EmailEmUsoException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage(), Map.of("email", e.getMessage()));
    }

    @ExceptionHandler(FotoInvalidaException.class)
    ResponseEntity<ErroResposta> fotoInvalida(FotoInvalidaException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage(), Map.of("foto", e.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ErroResposta> arquivoFaltando(MissingServletRequestPartException e) {
        return responder(
                HttpStatus.BAD_REQUEST,
                "Dados inválidos",
                Map.of(e.getRequestPartName(), "arquivo obrigatório"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErroResposta> arquivoGrande(MaxUploadSizeExceededException e) {
        return responder(HttpStatus.CONTENT_TOO_LARGE, "A foto deve ter no máximo 2 MB.", Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResposta> corpoInvalido(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        return responder(HttpStatus.BAD_REQUEST, "Dados inválidos", campos);
    }

    // Banco fora do ar ou sem conexão: 503, para o front mostrar "fora do ar, tente em instantes"
    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    ResponseEntity<ErroResposta> bancoIndisponivel(Exception e) {
        log.error("Banco de dados indisponível", e);
        return responder(
                HttpStatus.SERVICE_UNAVAILABLE,
                "O SAUF está temporariamente indisponível. Tente novamente em instantes.",
                Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResposta> inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", Map.of());
    }

    private ResponseEntity<ErroResposta> responder(HttpStatus status, String mensagem, Map<String, String> campos) {
        return ResponseEntity.status(status)
                .body(new ErroResposta(status.value(), status.getReasonPhrase(), mensagem, campos));
    }
}
