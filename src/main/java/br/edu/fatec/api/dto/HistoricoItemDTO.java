package br.edu.fatec.api.dto;

import br.edu.fatec.api.model.Mensagem; // <-- Verifique se este import está correto
import java.time.LocalDateTime;

/**
 * DTO unificado para a timeline de histórico.
 * Armazena a data, o tipo (VERSAO ou FEEDBACK) e a descrição.
 */
public record HistoricoItemDTO(
        LocalDateTime data,
        TipoHistorico tipo,
        String descricao,
        Object payload // Guarda o objeto original (VersaoHistoricoDTO ou Mensagem)
) implements Comparable<HistoricoItemDTO> {

    public enum TipoHistorico { VERSAO, FEEDBACK }

    /**
     * Permite que a lista seja ordenada cronologicamente (do mais antigo para o mais novo).
     */
    @Override
    public int compareTo(HistoricoItemDTO other) {
        // Se a data for nula, trata como o item mais antigo
        if (this.data == null && other.data == null) {
            return 0;
        }
        if (this.data == null) {
            return -1; // this é mais antigo
        }
        if (other.data == null) {
            return 1; // other é mais antigo
        }
        return this.data.compareTo(other.data);
    }
}