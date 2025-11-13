package br.edu.fatec.api.dto;

public record OrientadorDisponivelDTO(
        Long id,
        String nome,
        String email,
        String curso,
        int totalOrientandos,
        boolean disponivel
) {
}