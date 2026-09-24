package com.financeiro.financeiro.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
//    private Categorias categorias;

    public Usuario(){}

    public Usuario(String nome){
        this.nome = nome;
    }

    public String getNome(){
        return nome;
    }

    public Long getId(){
        return id;
    }
}
