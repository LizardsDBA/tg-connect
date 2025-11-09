package br.edu.fatec.api.dto;

import java.time.LocalDateTime;

public record VersaoHistoricoDTO(
        Long id,
        String versao,
        String conteudoMd,
        String comentario,
        LocalDateTime createdAt
) {}
