package com.financeiro.financeiro.repository;

import com.financeiro.financeiro.dto.CategoriaResponseDTO;
import com.financeiro.financeiro.model.Categoria;
import com.financeiro.financeiro.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    @Query("SELECT new com.financeiro.financeiro.dto.CategoriaResponseDTO(c.id, c.nome, COUNT(t)) " +
            "FROM Categoria c LEFT JOIN Transacao t ON t.categoria = c AND t.usuario = :usuario " +
            "WHERE c.usuario = :usuario OR c.usuario IS NULL " +
            "GROUP BY c.id, c.nome ORDER BY c.nome")
    List<CategoriaResponseDTO> listarComTotalPorUsuario(@Param("usuario") Usuario usuario);
}
