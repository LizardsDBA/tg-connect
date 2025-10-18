package br.edu.fatec.api.dao;

import br.edu.fatec.api.model.Mensagem;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface MensagemDao {
    List<Mensagem> listarHistorico(Long trabalhoId) throws SQLException;

    List<Mensagem> listarNovas(Long trabalhoId, LocalDateTime afterCreatedAt) throws SQLException;

    Long inserirTexto(Long trabalhoId, Long remetenteId, Long destinatarioId, String conteudo) throws SQLException;

    int marcarComoLidas(Long trabalhoId, Long destinatarioId) throws SQLException;
}
