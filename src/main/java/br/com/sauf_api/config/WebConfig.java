package br.com.sauf_api.config;

import br.com.sauf_api.admin.AcessoAdmin;
import br.com.sauf_api.admin.AcessoAdminInterceptor;
import br.com.sauf_api.comum.EnumComValorConverterFactory;
import br.com.sauf_api.ingestao.ApiKeyInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] origensPermitidas;
    private final String apiKey;
    private final JsonMapper json;
    private final AcessoAdmin acessoAdmin;

    public WebConfig(
            @Value("${sauf.cors.origens-permitidas}") String[] origensPermitidas,
            @Value("${sauf.ingestao.api-key:}") String apiKey,
            JsonMapper json,
            AcessoAdmin acessoAdmin) {
        this.origensPermitidas = origensPermitidas;
        this.apiKey = apiKey;
        this.json = json;
        this.acessoAdmin = acessoAdmin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origensPermitidas)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowCredentials(true);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new EnumComValorConverterFactory());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        ApiKeyInterceptor chave = new ApiKeyInterceptor(apiKey, json);
        registry.addInterceptor(chave).addPathPatterns("/api/v1/ingestao/**");
        // Admin (D-107): sessão ADMIN ou X-Api-Key
        registry.addInterceptor(new AcessoAdminInterceptor(chave, acessoAdmin, json)).addPathPatterns("/api/v1/admin/**");
    }
}
