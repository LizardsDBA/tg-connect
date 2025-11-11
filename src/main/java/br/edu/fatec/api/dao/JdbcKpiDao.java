package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class JdbcKpiDao implements KpiDao {

    /**
     * CONTAGEM DE PENDÊNCIAS (ATUALIZADO)
     * Conta quantos campos de status NÃO são 1 (Aprovado)
     */
    private static final String SQL_PENDENCIAS_VALIDACOES = """
        SELECT 
            COALESCE(ap.pendencias_ap, 0) + COALESCE(sec.pendencias_sec, 0) + COALESCE(res.pendencias_res, 0) AS pendencias_totais
        FROM trabalhos_graduacao tg
        
        -- Subquery para Apresentação (conta 9 campos)
        LEFT JOIN (
            SELECT 
                trabalho_id, versao,
                (CASE WHEN nome_completo_status != 1 THEN 1 ELSE 0 END) +
                (CASE WHEN idade_status != 1 THEN 1 ELSE 0 END) +
                (CASE WHEN curso_status != 1 THEN 1 ELSE 0 END) +
                (CASE WHEN historico_academico_status != 1 THEN 1 ELSE 0 END) +
                (CASE WHEN motivacao_fatec_status != 1 THEN 1 ELSE 0 END) +
                (CASE WHEN historico_profissional_status != 1 THEN 1 ELSE 0 END) +
                (CASE WHEN contatos_email_status != 1 THEN 1 ELSE 0 END) +
                (CASE WHEN principais_conhecimentos_status != 1 THEN 1 ELSE 0 END) +
                (CASE WHEN consideracoes_finais_status != 1 THEN 1 ELSE 0 END) AS pendencias_ap
            FROM tg_apresentacao
        ) ap ON ap.trabalho_id = tg.id AND ap.versao = tg.versao_atual
        
        -- Subquery para Seções (conta 8 campos * 6 seções = 48 campos)
        LEFT JOIN (
            SELECT 
                trabalho_id, versao,
                SUM(
                    (CASE WHEN empresa_parceira_status != 1 THEN 1 ELSE 0 END) +
                    (CASE WHEN problema_status != 1 THEN 1 ELSE 0 END) +
                    (CASE WHEN solucao_resumo_status != 1 THEN 1 ELSE 0 END) +
                    (CASE WHEN link_repositorio_status != 1 THEN 1 ELSE 0 END) +
                    (CASE WHEN tecnologias_status != 1 THEN 1 ELSE 0 END) +
                    (CASE WHEN contribuicoes_status != 1 THEN 1 ELSE 0 END) +
                    (CASE WHEN hard_skills_status != 1 THEN 1 ELSE 0 END) +
                    (CASE WHEN soft_skills_status != 1 THEN 1 ELSE 0 END)
                ) AS pendencias_sec
            FROM tg_secao
            GROUP BY trabalho_id, versao
        ) sec ON sec.trabalho_id = tg.id AND sec.versao = tg.versao_atual
        
        -- Subquery para Resumo (conta 1 campo)
        LEFT JOIN (
            SELECT 
                trabalho_id, versao,
                (CASE WHEN versao_validada != 1 THEN 1 ELSE 0 END) AS pendencias_res
            FROM tg_resumo
        ) res ON res.trabalho_id = tg.id AND res.versao = tg.versao_atual
        
        WHERE tg.aluno_id = ?
        LIMIT 1
        """;

    // 🔹 Última versão do trabalho (mantida funcional)
    private static final String SQL_ULTIMA_VERSAO = """
        SELECT versao
        FROM versoes_trabalho
        WHERE trabalho_id = ?
        ORDER BY created_at DESC, id DESC
        LIMIT 1
        """;

    /**
     * CÁLCULO DE PERCENTUAL (ATUALIZADO)
     * Soma quantos campos de status SÃO 1 (Aprovado)
     * Total de campos = 9 (Apres) + 48 (6 * 8 APIs) + 1 (Resumo) = 58 campos
     */
    private static final String SQL_PERCENTUAL = """
        SELECT
            ROUND((
                (COALESCE(ap.aprovados_ap, 0) + COALESCE(sec.aprovados_sec, 0) + COALESCE(res.aprovados_res, 0)) / 58.0
            ) * 100, 0) AS percentual_conclusao
        FROM trabalhos_graduacao tg

        -- Subquery para Apresentação
        LEFT JOIN (
            SELECT 
                trabalho_id, versao,
                (CASE WHEN nome_completo_status = 1 THEN 1 ELSE 0 END) +
                (CASE WHEN idade_status = 1 THEN 1 ELSE 0 END) +
                (CASE WHEN curso_status = 1 THEN 1 ELSE 0 END) +
                (CASE WHEN historico_academico_status = 1 THEN 1 ELSE 0 END) +
                (CASE WHEN motivacao_fatec_status = 1 THEN 1 ELSE 0 END) +
                (CASE WHEN historico_profissional_status = 1 THEN 1 ELSE 0 END) +
                (CASE WHEN contatos_email_status = 1 THEN 1 ELSE 0 END) +
                (CASE WHEN principais_conhecimentos_status = 1 THEN 1 ELSE 0 END) +
                (CASE WHEN consideracoes_finais_status = 1 THEN 1 ELSE 0 END) AS aprovados_ap
            FROM tg_apresentacao
        ) ap ON ap.trabalho_id = tg.id AND ap.versao = tg.versao_atual

        -- Subquery para Seções
        LEFT JOIN (
            SELECT 
                trabalho_id, versao,
                SUM(
                    (CASE WHEN empresa_parceira_status = 1 THEN 1 ELSE 0 END) +
                    (CASE WHEN problema_status = 1 THEN 1 ELSE 0 END) +
                    (CASE WHEN solucao_resumo_status = 1 THEN 1 ELSE 0 END) +
                    (CASE WHEN link_repositorio_status = 1 THEN 1 ELSE 0 END) +
                    (CASE WHEN tecnologias_status = 1 THEN 1 ELSE 0 END) +
                    (CASE WHEN contribuicoes_status = 1 THEN 1 ELSE 0 END) +
                    (CASE WHEN hard_skills_status = 1 THEN 1 ELSE 0 END) +
                    (CASE WHEN soft_skills_status = 1 THEN 1 ELSE 0 END)
                ) AS aprovados_sec
            FROM tg_secao
            GROUP BY trabalho_id, versao
        ) sec ON sec.trabalho_id = tg.id AND sec.versao = tg.versao_atual

        -- Subquery para Resumo
        LEFT JOIN (
            SELECT 
                trabalho_id, versao,
                (CASE WHEN versao_validada = 1 THEN 1 ELSE 0 END) AS aprovados_res
            FROM tg_resumo
        ) res ON res.trabalho_id = tg.id AND res.versao = tg.versao_atual
        
        WHERE tg.id = ?
        LIMIT 1
        """;

    @Override
    public int countPendenciasAluno(long alunoId) {
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(SQL_PENDENCIAS_VALIDACOES)) {
            ps.setLong(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("pendencias_totais");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0; // Retorna 0 em caso de erro
    }

    @Override
    public Optional<String> findUltimaVersaoByTrabalhoId(long trabalhoId) {
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(SQL_ULTIMA_VERSAO)) {
            ps.setLong(1, trabalhoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.ofNullable(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public double findPercentualConclusaoByTrabalhoId(long trabalhoId) {
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(SQL_PERCENTUAL)) {
            ps.setLong(1, trabalhoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("percentual_conclusao");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * CONTAGEM KPI: Total de Alunos Ativos
     */
    public int countAlunosAtivos() {
        // (Usamos 'ativo = 1' se quiséssemos filtrar, mas por enquanto contamos todos)
        String sql = "SELECT COUNT(id) FROM usuarios WHERE tipo = 'ALUNO'";
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * CONTAGEM KPI: Total de Orientadores COM Alunos
     */
    public int countOrientadoresComAlunos() {
        // Conta Orientadores + Coordenadores que TÊM vínculo na tabela 'orientacoes'
        String sql = """
            SELECT COUNT(DISTINCT u.id)
            FROM usuarios u
            JOIN orientacoes o ON u.id = o.orientador_id
            WHERE (u.tipo = 'ORIENTADOR' OR u.tipo = 'COORDENADOR')
        """;
        // (Nota: 'JOIN' naturalmente só pega quem tem vínculo)
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * CONTAGEM KPI: Total de Orientadores SEM Alunos
     */
    public int countOrientadoresSemAlunos() {
        // Conta Orientadores + Coordenadores que NÃO TÊM vínculo na 'orientacoes'
        String sql = """
            SELECT COUNT(DISTINCT u.id)
            FROM usuarios u
            LEFT JOIN orientacoes o ON u.id = o.orientador_id
            WHERE (u.tipo = 'ORIENTADOR' OR u.tipo = 'COORDENADOR')
            AND o.id IS NULL
        """;
        // (Nota: 'LEFT JOIN' com 'IS NULL' pega quem não tem vínculo)
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}