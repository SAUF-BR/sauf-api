package br.com.sauf_api.auth;

import static org.springframework.security.config.Customizer.withDefaults;

import br.com.sauf_api.comum.ErroResposta;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Quem pode acessar o quê (D-87). Exigem login: /me (perfil e foto) e /assinatura
 * (menos /assinatura/planos). O resto (catálogo, Swagger, login/cadastro) é público, e a
 * ingestão segue protegida pela X-Api-Key (ApiKeyInterceptor).
 * Sem sessão no servidor: a sessão é o JWT do cookie (FiltroSessao).
 */
@Configuration
public class SegurancaConfig {

    @Bean
    SecurityFilterChain seguranca(HttpSecurity http, TokenService tokens, JsonMapper json) throws Exception {
        http
                // Cookie SameSite=Lax + corpo só JSON/multipart + CORS só para o front (ADR-005)
                .csrf(csrf -> csrf.disable())
                .cors(withDefaults())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/assinatura/planos").permitAll()
                        .requestMatchers("/api/v1/me", "/api/v1/me/**").authenticated()
                        .requestMatchers("/api/v1/assinatura", "/api/v1/assinatura/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint((request, response, e) -> responder(
                                response, json, HttpStatus.UNAUTHORIZED, "Entre na sua conta para continuar"))
                        .accessDeniedHandler((request, response, e) -> responder(
                                response, json, HttpStatus.FORBIDDEN, "Acesso negado")))
                .addFilterBefore(new FiltroSessao(tokens), AnonymousAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // O login é do AuthService, não do Spring. Este bean só impede o Spring de criar o
    // usuário "user" com senha gerada (e de imprimir essa senha no log).
    @Bean
    UserDetailsService semUsuariosDoSpring() {
        return nome -> {
            throw new UsernameNotFoundException(nome);
        };
    }

    private static void responder(HttpServletResponse response, JsonMapper json, HttpStatus status, String mensagem)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(
                response.getOutputStream(),
                new ErroResposta(status.value(), status.getReasonPhrase(), mensagem, Map.of()));
    }
}
