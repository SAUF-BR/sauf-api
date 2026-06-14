package br.com.sauf_api.controller;

import br.com.sauf_api.model.Instituicao;
import br.com.sauf_api.repository.InstituicaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/instituicoes")
public class InstituicaoController {

    @Autowired
    private InstituicaoRepository repository;

    @GetMapping
    public List<Instituicao> listarTodas() {
        return repository.findAll();
    }
}