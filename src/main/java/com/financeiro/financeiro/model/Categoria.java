package com.financeiro.financeiro.model;

import jakarta.persistence.*;

@Entity
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @ManyToOne
    private Usuario usuario;

    public Categoria(){}

    public Categoria(String nome, Usuario usuario){
        this.nome = nome;
        this.usuario = usuario;
    }

    public String getNome(){return nome;}

    public Long getId() {return id;}

    public Usuario getUsuario(){return usuario;}
}
