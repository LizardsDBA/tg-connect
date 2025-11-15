package br.edu.fatec.api.service;

import br.edu.fatec.api.dao.JdbcMensagemDao;
import br.edu.fatec.api.dao.JdbcVersoesTrabalhoDao;
import br.edu.fatec.api.dto.HistoricoItemDTO;
import br.edu.fatec.api.dto.VersaoHistoricoDTO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoricoService {

    private final JdbcVersoesTrabalhoDao versoesDao = new JdbcVersoesTrabalhoDao();
    private final JdbcMensagemDao mensagemDao = new JdbcMensagemDao();

    /**
     * Busca e unifica Versões e Mensagens (feedbacks) em uma
     * lista cronológica única.
     */
    public List<HistoricoItemDTO> getHistoricoUnificado(long trabalhoId) throws SQLException {

        // 1. Buscar as Versões
        List<HistoricoItemDTO> versoes = versoesDao.listarHistoricoCompleto(trabalhoId).stream()
                .map(v -> new HistoricoItemDTO(
                        v.createdAt(),
                        HistoricoItemDTO.TipoHistorico.VERSAO,
                        "Versão submetida: " + v.versao(),
                        v // Guarda o DTO original
                ))
                .toList();

        // 2. Buscar os Feedbacks (Mensagens de chat)
        List<HistoricoItemDTO> feedbacks = mensagemDao.listarHistorico(trabalhoId).stream()
                .map(m -> new HistoricoItemDTO(
                        m.getCreatedAt(),
                        HistoricoItemDTO.TipoHistorico.FEEDBACK,
                        m.getConteudo(), // A própria mensagem é a descrição
                        m // Guarda a Mensagem original
                ))
                .toList();

        // 3. Unificar e Ordenar
        List<HistoricoItemDTO> historicoUnificado = new ArrayList<>(versoes);
        historicoUnificado.addAll(feedbacks);
        Collections.sort(historicoUnificado); // Ordena pela data (usando o compareTo do DTO)

        return historicoUnificado;
    }
}