package com.financeiro.financeiro.repository;

import com.financeiro.financeiro.dto.ResumoCategoriaDTO;
import com.financeiro.financeiro.model.Categoria;
import com.financeiro.financeiro.model.Transacao;
import com.financeiro.financeiro.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao,Long> {
    long countByCategoria(Categoria categoria);

    @Query("SELECT t FROM Transacao t WHERE t.usuario = :usuario")
    List<Transacao> findByUsuario(@Param("usuario") Usuario usuario);

    @Query("SELECT new com.financeiro.financeiro.dto.ResumoCategoriaDTO(t.categoria.nome, t.tipo, SUM(t.valor)) " +
            "FROM Transacao t WHERE t.usuario = :usuario AND t.data BETWEEN :inicio AND :fim " +
            "GROUP BY t.categoria.nome, t.tipo")
    List<ResumoCategoriaDTO> somarPorCategoria(@Param("usuario") Usuario usuario,
                                               @Param("inicio") LocalDate inicio,
                                               @Param("fim") LocalDate fim);
}
