package br.com.sauf_api.comum;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Quem está fazendo a requisição. Provisório até o login (D-55): todas as
 * requisições usam o mesmo usuário de teste. Com o login, só esta classe muda.
 */
@Component
public class UsuarioLogado {

    public static final UUID PROVISORIO = UUID.fromString("f9def6d7-0717-4540-8ad0-ec03e301b2c7");

    public UUID id() {
        return PROVISORIO;
    }
}
