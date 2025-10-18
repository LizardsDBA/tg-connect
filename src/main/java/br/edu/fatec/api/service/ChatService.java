package br.edu.fatec.api.service;

import br.edu.fatec.api.dao.MensagemDao;
import br.edu.fatec.api.model.Mensagem;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class ChatService {
    private final MensagemDao mensagemDao;

    public ChatService(MensagemDao mensagemDao) {
        this.mensagemDao = mensagemDao;
    }

    public List<Mensagem> carregarHistorico(Long trabalhoId) throws SQLException {
        return mensagemDao.listarHistorico(trabalhoId);
    }

    public List<Mensagem> carregarNovas(Long trabalhoId, LocalDateTime after) throws SQLException {
        return mensagemDao.listarNovas(trabalhoId, after);
    }

    public Long enviar(Long trabalhoId, Long remetenteId, Long destinatarioId, String conteudo) throws SQLException {
        if (conteudo == null || conteudo.isBlank()) return null;
        return mensagemDao.inserirTexto(trabalhoId, remetenteId, destinatarioId, conteudo.trim());
    }

    public int marcarLidas(Long trabalhoId, Long destinatarioId) throws SQLException {
        return mensagemDao.marcarComoLidas(trabalhoId, destinatarioId);
    }
}
