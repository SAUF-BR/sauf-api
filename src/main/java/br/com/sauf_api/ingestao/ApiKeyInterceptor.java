package br.com.sauf_api.ingestao;

import br.com.sauf_api.comum.ErroResposta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

public class ApiKeyInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Api-Key";

    private final byte[] chaveEsperada;
    private final JsonMapper json;

    public ApiKeyInterceptor(String chaveEsperada, JsonMapper json) {
        this.chaveEsperada = chaveEsperada == null || chaveEsperada.isBlank()
                ? null
                : chaveEsperada.getBytes(StandardCharsets.UTF_8);
        this.json = json;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String recebida = request.getHeader(HEADER);
        if (chaveEsperada != null
                && recebida != null
                && MessageDigest.isEqual(chaveEsperada, recebida.getBytes(StandardCharsets.UTF_8))) {
            return true;
        }
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String mensagem = chaveEsperada == null
                ? "Ingestão desabilitada: SAUF_API_KEY não configurada"
                : "Header " + HEADER + " ausente ou inválido";
        json.writeValue(response.getWriter(), new ErroResposta(status.value(), status.getReasonPhrase(), mensagem, Map.of()));
        return false;
    }
}
