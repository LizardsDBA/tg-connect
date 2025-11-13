package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;
import br.edu.fatec.api.dto.OrientadorDisponivelDTO;
import br.edu.fatec.api.model.SolicitacaoOrientacao;
import br.edu.fatec.api.model.SolicitacaoOrientacao.StatusSolicitacao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcSolicitacaoOrientacaoDao {

    // ====== MÉTODOS PARA O ALUNO ======

    /**
     * Verifica se o aluno já possui um orientador atribuído
     */
    public boolean alunoTemOrientador(Long alunoId) throws SQLException {
        String sql = """
            SELECT COUNT(*) 
            FROM orientacoes 
            WHERE aluno_id = ? AND ativo = TRUE
        """;

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Verifica se o aluno tem alguma solicitação pendente
     */
    public boolean alunoTemSolicitacaoPendente(Long alunoId) throws SQLException {
        String sql = """
            SELECT COUNT(*) 
            FROM solicitacoes_orientacao 
            WHERE aluno_id = ? AND status = 'PENDENTE'
        """;

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Lista todos os orientadores disponíveis para o aluno solicitar
     */
    public List<OrientadorDisponivelDTO> listarOrientadoresDisponiveis() throws SQLException {
        String sql = """
            SELECT 
                u.id,
                u.nome,
                u.email,
                '' as curso,
                COUNT(DISTINCT o.aluno_id) as total_orientandos,
                CASE WHEN COUNT(DISTINCT o.aluno_id) < 10 THEN TRUE ELSE FALSE END as disponivel
            FROM usuarios u
            LEFT JOIN orientacoes o ON u.id = o.orientador_id AND o.ativo = TRUE
            WHERE u.tipo IN ('ORIENTADOR', 'COORDENADOR') 
              AND u.ativo = TRUE
            GROUP BY u.id, u.nome, u.email
            ORDER BY total_orientandos ASC, u.nome ASC
        """;

        List<OrientadorDisponivelDTO> lista = new ArrayList<>();

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new OrientadorDisponivelDTO(
                        rs.getLong("id"),
                        rs.getString("nome"),
                        rs.getString("email"),
                        rs.getString("curso"),
                        rs.getInt("total_orientandos"),
                        rs.getBoolean("disponivel")
                ));
            }
        }

        return lista;
    }

    /**
     * Cria uma nova solicitação de orientação
     */
    public Long criarSolicitacao(Long alunoId, Long orientadorId, String mensagemAluno) throws SQLException {
        String sql = """
            INSERT INTO solicitacoes_orientacao 
                (aluno_id, orientador_id, mensagem_aluno, status, data_solicitacao)
            VALUES (?, ?, ?, 'PENDENTE', NOW())
        """;

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, alunoId);
            ps.setLong(2, orientadorId);
            ps.setString(3, mensagemAluno);

            int affected = ps.executeUpdate();

            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        }

        throw new SQLException("Falha ao criar solicitação - nenhuma linha afetada");
    }

    /**
     * Busca a última solicitação do aluno (qualquer status)
     */
    public Optional<SolicitacaoOrientacao> buscarUltimaSolicitacaoAluno(Long alunoId) throws SQLException {
        String sql = """
            SELECT 
                s.id, s.aluno_id, s.orientador_id, s.status,
                s.mensagem_aluno, s.justificativa_recusa,
                s.data_solicitacao, s.data_resposta,
                a.nome as nome_aluno, a.email as email_aluno,
                o.nome as nome_orientador, o.email as email_orientador
            FROM solicitacoes_orientacao s
            INNER JOIN usuarios a ON s.aluno_id = a.id
            INNER JOIN usuarios o ON s.orientador_id = o.id
            WHERE s.aluno_id = ?
            ORDER BY s.data_solicitacao DESC
            LIMIT 1
        """;

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, alunoId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearSolicitacao(rs));
                }
            }
        }

        return Optional.empty();
    }

    // ====== MÉTODOS PARA O ORIENTADOR ======

    /**
     * Lista todas as solicitações pendentes para um orientador
     */
    public List<SolicitacaoOrientacao> listarSolicitacoesPendentes(Long orientadorId) throws SQLException {
        String sql = """
            SELECT 
                s.id, s.aluno_id, s.orientador_id, s.status,
                s.mensagem_aluno, s.justificativa_recusa,
                s.data_solicitacao, s.data_resposta,
                a.nome as nome_aluno, a.email as email_aluno,
                o.nome as nome_orientador, o.email as email_orientador
            FROM solicitacoes_orientacao s
            INNER JOIN usuarios a ON s.aluno_id = a.id
            INNER JOIN usuarios o ON s.orientador_id = o.id
            WHERE s.orientador_id = ? AND s.status = 'PENDENTE'
            ORDER BY s.data_solicitacao ASC
        """;

        List<SolicitacaoOrientacao> lista = new ArrayList<>();

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, orientadorId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearSolicitacao(rs));
                }
            }
        }

        return lista;
    }

    /**
     * Lista todas as solicitações (qualquer status) para um orientador
     */
    public List<SolicitacaoOrientacao> listarTodasSolicitacoes(Long orientadorId) throws SQLException {
        String sql = """
            SELECT 
                s.id, s.aluno_id, s.orientador_id, s.status,
                s.mensagem_aluno, s.justificativa_recusa,
                s.data_solicitacao, s.data_resposta,
                a.nome as nome_aluno, a.email as email_aluno,
                o.nome as nome_orientador, o.email as email_orientador
            FROM solicitacoes_orientacao s
            INNER JOIN usuarios a ON s.aluno_id = a.id
            INNER JOIN usuarios o ON s.orientador_id = o.id
            WHERE s.orientador_id = ?
            ORDER BY 
                CASE s.status 
                    WHEN 'PENDENTE' THEN 1 
                    WHEN 'APROVADA' THEN 2 
                    WHEN 'RECUSADA' THEN 3 
                END,
                s.data_solicitacao DESC
        """;

        List<SolicitacaoOrientacao> lista = new ArrayList<>();

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, orientadorId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearSolicitacao(rs));
                }
            }
        }

        return lista;
    }

    /**
     * Aprova uma solicitação e cria a orientação
     */
    public void aprovarSolicitacao(Long solicitacaoId) throws SQLException {
        Connection con = null;
        try {
            con = Database.get();
            con.setAutoCommit(false);

            // 1. Buscar dados da solicitação
            String sqlBuscar = """
                SELECT aluno_id, orientador_id 
                FROM solicitacoes_orientacao 
                WHERE id = ? AND status = 'PENDENTE'
            """;

            Long alunoId = null;
            Long orientadorId = null;

            try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
                ps.setLong(1, solicitacaoId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        alunoId = rs.getLong("aluno_id");
                        orientadorId = rs.getLong("orientador_id");
                    } else {
                        throw new SQLException("Solicitação não encontrada ou já processada");
                    }
                }
            }

            // 2. Atualizar status da solicitação
            String sqlAprovar = """
                UPDATE solicitacoes_orientacao 
                SET status = 'APROVADA', data_resposta = NOW()
                WHERE id = ?
            """;

            try (PreparedStatement ps = con.prepareStatement(sqlAprovar)) {
                ps.setLong(1, solicitacaoId);
                ps.executeUpdate();
            }

            // 3. Criar a orientação
            String sqlOrientacao = """
                INSERT INTO orientacoes (aluno_id, orientador_id, ativo, criado_em)
                VALUES (?, ?, TRUE, NOW())
            """;

            try (PreparedStatement ps = con.prepareStatement(sqlOrientacao)) {
                ps.setLong(1, alunoId);
                ps.setLong(2, orientadorId);
                ps.executeUpdate();
            }

            // 4. Criar o trabalho de graduação
            String sqlTG = """
                INSERT INTO trabalhos_graduacao 
                    (aluno_id, orientador_id, titulo, tema, versao_atual, status, percentual_conclusao, data_inicio)
                VALUES (?, ?, ?, ?, 'v1', 'EM_ANDAMENTO', 0.00, NOW())
            """;

            try (PreparedStatement ps = con.prepareStatement(sqlTG)) {
                ps.setLong(1, alunoId);
                ps.setLong(2, orientadorId);
                ps.setString(3, "Portfólio TG - Em desenvolvimento");
                ps.setString(4, "Tema a ser definido");
                ps.executeUpdate();
            }

            con.commit();

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    /**
     * Recusa uma solicitação com justificativa
     */
    public void recusarSolicitacao(Long solicitacaoId, String justificativa) throws SQLException {
        String sql = """
            UPDATE solicitacoes_orientacao 
            SET status = 'RECUSADA', 
                justificativa_recusa = ?,
                data_resposta = NOW()
            WHERE id = ? AND status = 'PENDENTE'
        """;

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, justificativa);
            ps.setLong(2, solicitacaoId);

            int affected = ps.executeUpdate();

            if (affected == 0) {
                throw new SQLException("Solicitação não encontrada ou já processada");
            }
        }
    }

    /**
     * Conta o número de solicitações pendentes de um orientador
     */
    public int contarSolicitacoesPendentes(Long orientadorId) throws SQLException {
        String sql = """
            SELECT COUNT(*) 
            FROM solicitacoes_orientacao 
            WHERE orientador_id = ? AND status = 'PENDENTE'
        """;

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, orientadorId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    // ====== MÉTODO AUXILIAR ======

    private SolicitacaoOrientacao mapearSolicitacao(ResultSet rs) throws SQLException {
        SolicitacaoOrientacao sol = new SolicitacaoOrientacao();

        sol.setId(rs.getLong("id"));
        sol.setAlunoId(rs.getLong("aluno_id"));
        sol.setOrientadorId(rs.getLong("orientador_id"));
        sol.setStatus(StatusSolicitacao.valueOf(rs.getString("status")));
        sol.setMensagemAluno(rs.getString("mensagem_aluno"));
        sol.setJustificativaRecusa(rs.getString("justificativa_recusa"));

        Timestamp ts = rs.getTimestamp("data_solicitacao");
        if (ts != null) {
            sol.setDataSolicitacao(ts.toLocalDateTime());
        }

        ts = rs.getTimestamp("data_resposta");
        if (ts != null) {
            sol.setDataResposta(ts.toLocalDateTime());
        }

        sol.setNomeAluno(rs.getString("nome_aluno"));
        sol.setEmailAluno(rs.getString("email_aluno"));
        sol.setNomeOrientador(rs.getString("nome_orientador"));
        sol.setEmailOrientador(rs.getString("email_orientador"));

        return sol;
    }
}