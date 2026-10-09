package br.com.sauf_api;

import br.com.sauf_api.config.ExigirPostgres;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SaufApiApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SaufApiApplication.class);
        app.addListeners(new ExigirPostgres());
        app.run(args);
    }
}
