package com.financeiro.financeiro.repository;

import com.financeiro.financeiro.model.Transacao;
import com.financeiro.financeiro.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao,Long> {
    @Query("SELECT t FROM Transacao t WHERE t.usuario = :usuario")
    List<Transacao> findByUsuario(@Param("usuario") Usuario usuario);
}
