package br.com.sauf_api.favorito;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import br.com.sauf_api.comum.UsuarioLogado;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class FavoritoUniversidadeControllerTest {

    @Test
    void cliqueDuploQueEstouraAUniqueContaComoJaFavoritada() {
        FavoritoUniversidadeService service = mock(FavoritoUniversidadeService.class);
        doThrow(new DataIntegrityViolationException("uk_favorito_universidade"))
                .when(service).favoritar(any(), any());
        var controller = new FavoritoUniversidadeController(service, new UsuarioLogado());

        assertThatCode(() -> controller.favoritar(UUID.randomUUID())).doesNotThrowAnyException();
    }
}