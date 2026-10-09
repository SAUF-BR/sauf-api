package br.com.sauf_api.comum;

import br.com.sauf_api.ingestao.InstituicaoNaoCadastradaException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResposta> corpoInvalido(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        return responder(HttpStatus.BAD_REQUEST, "Dados inválidos", campos);
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
