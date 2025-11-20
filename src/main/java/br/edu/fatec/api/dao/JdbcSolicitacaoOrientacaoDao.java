package br.edu.fatec.api.dao;

import br.edu.fatec.api.model.SolicitacaoOrientacao;
import br.edu.fatec.api.model.SolicitacaoOrientacao.StatusSolicitacao;
import br.edu.fatec.api.config.Database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcSolicitacaoOrientacaoDao {

    public List<OrientadorDisponivel> listarOrientadoresDisponiveis() throws SQLException {
        String sql = "SELECT id, nome, email FROM usuarios WHERE tipo = 'ORIENTADOR' AND ativo = TRUE ORDER BY nome";
        List<OrientadorDisponivel> lista = new ArrayList<>();

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                OrientadorDisponivel o = new OrientadorDisponivel(
                        rs.getLong("id"),
                        rs.getString("nome"),
                        rs.getString("email")
                );
                lista.add(o);
            }
        }
        return lista;
    }

    public boolean alunoTemOrientacaoAtiva(Long alunoId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM orientacoes WHERE aluno_id = ? AND ativo = TRUE";

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean alunoTemSolicitacaoPendente(Long alunoId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM solicitacoes_orientacao WHERE aluno_id = ? AND status = 'PENDENTE'";

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Cria a solicitação E o registro inicial do TG (sem orientador).
     */
    public Long criarSolicitacao(Long alunoId, Long orientadorId, String titulo, String tema) throws SQLException {
        String sqlSolicitacao = "INSERT INTO solicitacoes_orientacao (aluno_id, orientador_id, status) VALUES (?, ?, 'PENDENTE')";

        // Cria ou atualiza o TG existente (caso tenha sido reprovado antes)
        String sqlTG = """
            INSERT INTO trabalhos_graduacao (aluno_id, titulo, tema, versao_atual, percentual_conclusao, status)
            VALUES (?, ?, ?, 'v1', 0.00, 'EM_ANDAMENTO')
            ON DUPLICATE KEY UPDATE 
                titulo = VALUES(titulo), 
                tema = VALUES(tema),
                orientador_id = NULL -- Limpa orientador anterior se houver
        """;

        Connection con = null;
        try {
            con = Database.get();
            con.setAutoCommit(false);

            // 1. Cria/Atualiza o TG com os dados do aluno
            try (PreparedStatement psTg = con.prepareStatement(sqlTG)) {
                psTg.setLong(1, alunoId);
                psTg.setString(2, titulo);
                psTg.setString(3, tema);
                psTg.executeUpdate();
            }

            // 2. Cria a solicitação de orientação
            long solicitacaoId;
            try (PreparedStatement psSol = con.prepareStatement(sqlSolicitacao, Statement.RETURN_GENERATED_KEYS)) {
                psSol.setLong(1, alunoId);
                psSol.setLong(2, orientadorId);
                psSol.executeUpdate();

                try (ResultSet rs = psSol.getGeneratedKeys()) {
                    if (rs.next()) {
                        solicitacaoId = rs.getLong(1);
                    } else {
                        throw new SQLException("Falha ao obter ID da solicitação criada");
                    }
                }
            }

            con.commit();
            return solicitacaoId;

        } catch (SQLException e) {
            if (con != null) con.rollback();
            throw e;
        } finally {
            if (con != null) {
                con.setAutoCommit(true);
                con.close();
            }
        }
    }

    public List<SolicitacaoOrientacao> listarSolicitacoesPorOrientador(Long orientadorId, StatusSolicitacao status) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT s.id, s.aluno_id, s.orientador_id, s.status, s.justificativa, " +
                        "s.data_solicitacao, s.data_resposta, u.nome as nome_aluno " +
                        "FROM solicitacoes_orientacao s " +
                        "JOIN usuarios u ON s.aluno_id = u.id " +
                        "WHERE s.orientador_id = ?"
        );

        if (status != null) {
            sql.append(" AND s.status = ?");
        }
        sql.append(" ORDER BY s.data_solicitacao DESC");

        List<SolicitacaoOrientacao> lista = new ArrayList<>();

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            ps.setLong(1, orientadorId);
            if (status != null) {
                ps.setString(2, status.name());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SolicitacaoOrientacao sol = mapearResultSet(rs);
                    lista.add(sol);
                }
            }
        }
        return lista;
    }

    public Optional<SolicitacaoOrientacao> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT s.*, u.nome as nome_aluno, o.nome as nome_orientador " +
                "FROM solicitacoes_orientacao s " +
                "JOIN usuarios u ON s.aluno_id = u.id " +
                "JOIN usuarios o ON s.orientador_id = o.id " +
                "WHERE s.id = ?";

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SolicitacaoOrientacao sol = mapearResultSet(rs);
                    sol.setNomeOrientador(rs.getString("nome_orientador"));
                    return Optional.of(sol);
                }
            }
        }
        return Optional.empty();
    }

    public void aprovarSolicitacao(Long solicitacaoId) throws SQLException {
        Connection con = null;
        try {
            con = Database.get();
            con.setAutoCommit(false);

            SolicitacaoOrientacao sol = buscarPorId(solicitacaoId)
                    .orElseThrow(() -> new SQLException("Solicitação não encontrada"));

            // 1. Atualiza status da solicitação
            String sqlUpdateSol = "UPDATE solicitacoes_orientacao SET status = 'APROVADA', data_resposta = NOW() WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlUpdateSol)) {
                ps.setLong(1, solicitacaoId);
                ps.executeUpdate();
            }

            // 2. Cria o vínculo na tabela orientacoes
            String sqlInsertOrientacao = "INSERT INTO orientacoes (aluno_id, orientador_id, ativo) VALUES (?, ?, TRUE)";
            try (PreparedStatement ps = con.prepareStatement(sqlInsertOrientacao)) {
                ps.setLong(1, sol.getAlunoId());
                ps.setLong(2, sol.getOrientadorId());
                ps.executeUpdate();
            }

            // 3. VINCULA o orientador ao TG já existente (criado na solicitação)
            String sqlUpdateTG = "UPDATE trabalhos_graduacao SET orientador_id = ? WHERE aluno_id = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlUpdateTG)) {
                ps.setLong(1, sol.getOrientadorId());
                ps.setLong(2, sol.getAlunoId());
                ps.executeUpdate();
            }

            con.commit();
        } catch (SQLException e) {
            if (con != null) con.rollback();
            throw e;
        } finally {
            if (con != null) {
                con.setAutoCommit(true);
                con.close();
            }
        }
    }

    public void recusarSolicitacao(Long solicitacaoId, String justificativa) throws SQLException {
        String sql = "UPDATE solicitacoes_orientacao SET status = 'RECUSADA', justificativa = ?, data_resposta = NOW() WHERE id = ?";

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, justificativa);
            ps.setLong(2, solicitacaoId);
            ps.executeUpdate();
        }
    }

    public Optional<SolicitacaoOrientacao> buscarUltimaSolicitacaoAluno(Long alunoId) throws SQLException {
        String sql = "SELECT s.*, o.nome as nome_orientador " +
                "FROM solicitacoes_orientacao s " +
                "JOIN usuarios o ON s.orientador_id = o.id " +
                "WHERE s.aluno_id = ? " +
                "ORDER BY s.data_solicitacao DESC LIMIT 1";

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SolicitacaoOrientacao sol = mapearResultSet(rs);
                    sol.setNomeOrientador(rs.getString("nome_orientador"));
                    return Optional.of(sol);
                }
            }
        }
        return Optional.empty();
    }

    private SolicitacaoOrientacao mapearResultSet(ResultSet rs) throws SQLException {
        SolicitacaoOrientacao sol = new SolicitacaoOrientacao();
        sol.setId(rs.getLong("id"));
        sol.setAlunoId(rs.getLong("aluno_id"));
        sol.setOrientadorId(rs.getLong("orientador_id"));
        sol.setStatus(StatusSolicitacao.valueOf(rs.getString("status")));
        sol.setJustificativa(rs.getString("justificativa"));

        Timestamp ts = rs.getTimestamp("data_solicitacao");
        if (ts != null) sol.setDataSolicitacao(ts.toLocalDateTime());

        Timestamp tr = rs.getTimestamp("data_resposta");
        if (tr != null) sol.setDataResposta(tr.toLocalDateTime());

        try {
            String nomeAluno = rs.getString("nome_aluno");
            sol.setNomeAluno(nomeAluno);
        } catch (SQLException e) {
            // Ignora se coluna não existir
        }

        return sol;
    }

    public record OrientadorDisponivel(Long id, String nome, String email) {}
}