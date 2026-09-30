package com.financeiro.financeiro.controller;

import com.financeiro.financeiro.dto.TransacaoRequestDTO;
import com.financeiro.financeiro.model.Transacao;
import com.financeiro.financeiro.service.TransacaoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transacoes")
public class TransacaoController {
    private final TransacaoService service;
    public TransacaoController(TransacaoService service){
        this.service = service;
    }

    @PostMapping
    public Transacao criar(@RequestBody TransacaoRequestDTO dto){
        return  service.criar(dto.idUsuario(),dto.valor(),dto.data(),dto.tipo(),dto.idCategoria(),dto.formaPagamento());
    }

    @GetMapping
    public List<Transacao> listarPorUsuario(@RequestParam Long idUsuario){
        return service.listarPorUsuario(idUsuario);
    }
}
