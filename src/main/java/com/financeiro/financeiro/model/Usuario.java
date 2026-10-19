package com.financeiro.financeiro.model;

import jakarta.persistence.Column;
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
    @Column(unique = true)
    private String email;
    private String senhaHash;

    public Usuario(){}

    public Usuario(String nome, String email, String senhaHash){
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
    }

    public String getNome(){
        return nome;
    }

    public Long getId(){
        return id;
    }

    public String getEmail(){
        return email;
    }

    public String getSenhaHash(){
        return senhaHash;
    }
}
