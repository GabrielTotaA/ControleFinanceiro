package com.financeiro.financeiro.repository;

import com.financeiro.financeiro.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario,Long> {
}