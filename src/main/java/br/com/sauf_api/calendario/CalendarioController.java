package br.com.sauf_api.calendario;

import br.com.sauf_api.comum.DadosInvalidosException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendario")
public class CalendarioController {

    private static final int MAXIMO_PRAZOS = 20;

    private final CalendarioService service;

    public CalendarioController(CalendarioService service) {
        this.service = service;
    }

    /** Eventos com algum dia no ano (padrão: o ano atual), em ordem de início. */
    @GetMapping("/eventos")
    public List<EventoResposta> eventos(@RequestParam(required = false) Integer ano) {
        int escolhido = ano == null ? service.hoje().getYear() : ano;
        if (escolhido < 2000 || escolhido > 2100) {
            throw new DadosInvalidosException("ano", "Use um ano entre 2000 e 2100");
        }
        return service.doAno(escolhido);
    }

    /** Os próximos prazos (fim do período ou dia do evento a partir de hoje), do mais próximo ao mais distante. */
    @GetMapping("/proximos-prazos")
    public List<PrazoResposta> proximosPrazos(@RequestParam(defaultValue = "3") int limite) {
        if (limite < 1 || limite > MAXIMO_PRAZOS) {
            throw new DadosInvalidosException("limite", "Use um limite entre 1 e " + MAXIMO_PRAZOS);
        }
        return service.proximosPrazos(limite);
    }
}
