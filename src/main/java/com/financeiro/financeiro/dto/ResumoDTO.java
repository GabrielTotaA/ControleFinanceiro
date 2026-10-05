package com.financeiro.financeiro.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumoDTO(BigDecimal totalEntradas, BigDecimal totalSaidas, BigDecimal saldo,
                        List<ResumoCategoriaDTO> porCategoria) {
}
