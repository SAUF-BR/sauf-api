package br.com.sauf_api.controller;

import br.com.sauf_api.model.Anuncio;
import br.com.sauf_api.repository.AnuncioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/anuncios")
public class AnuncioController {

    @Autowired
    private AnuncioRepository repository;

    @GetMapping
    public List<Anuncio> listarTodos() {
        return repository.findAll();
    }
}