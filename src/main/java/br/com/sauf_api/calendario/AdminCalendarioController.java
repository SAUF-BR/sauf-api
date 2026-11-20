package br.com.sauf_api.calendario;

import br.com.sauf_api.ingestao.RespostaIngestao;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro das datas (D-106). Exige sessão ADMIN ou X-Api-Key (AcessoAdminInterceptor, D-107). */
@RestController
@RequestMapping("/api/v1/admin/calendario/eventos")
public class AdminCalendarioController {

    private final CalendarioService service;

    public AdminCalendarioController(CalendarioService service) {
        this.service = service;
    }

    /** Cria ou substitui o evento da chave; reenviar igual responde sem criados nem atualizados. */
    @PutMapping("/{chave}")
    public RespostaIngestao salvar(@PathVariable String chave, @Valid @RequestBody EventoRequest dados) {
        return service.salvar(chave, dados);
    }

    @DeleteMapping("/{chave}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable String chave) {
        service.remover(chave);
    }
}
