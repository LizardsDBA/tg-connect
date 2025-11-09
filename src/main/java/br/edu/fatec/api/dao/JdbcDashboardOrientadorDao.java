package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class JdbcDashboardOrientadorDao {

    /**
     * DTO (Record) para encapsular os 4 KPIs calculados.
     */
    public record KpisDTO(
            int totalOrientandos,
            int totalPendencias,
            int totalConcluidos,
            int totalComReprovacoes
    ) {}

    /**
     * Consulta SQL principal que calcula os 4 KPIs para um orientador.
     * Total de campos por TG = 58
     * (Apresentação = 9 campos)
     * (APIs 1-6 = 6 seções * 8 campos = 48 campos)
     * (Resumo = 1 campo)
     */
    private static final String SQL_KPI = """
        WITH AlunoStats AS (
            -- Etapa 1: Calcular o total de campos aprovados e reprovados POR ALUNO
            SELECT
                tg.orientador_id,
                tg.aluno_id,
                
                -- Soma de campos APROVADOS (Status = 1)
                (COALESCE(ap.valid_ap, 0) + COALESCE(sec.valid_sec, 0) + COALESCE(res.valid_res, 0)) AS total_aprovados,
                
                -- Soma de campos REPROVADOS (Status = 2)
                (COALESCE(ap.reprov_ap, 0) + COALESCE(sec.reprov_sec, 0) + COALESCE(res.reprov_res, 0)) AS total_reprovados
                
            FROM trabalhos_graduacao tg
            INNER JOIN usuarios uAluno ON uAluno.id = tg.aluno_id AND uAluno.ativo = TRUE
            INNER JOIN orientacoes o ON o.aluno_id = uAluno.id AND o.ativo = TRUE

            -- Subquery para Apresentação (9 campos)
            LEFT JOIN (
                SELECT 
                    trabalho_id, versao,
                    SUM(CASE WHEN nome_completo_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN idade_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN curso_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN historico_academico_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN motivacao_fatec_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN historico_profissional_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN contatos_email_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN principais_conhecimentos_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN consideracoes_finais_status = 1 THEN 1 ELSE 0 END) AS valid_ap,
                        
                    SUM(CASE WHEN nome_completo_status = 2 THEN 1 ELSE 0 END +
                        CASE WHEN idade_status = 2 THEN 1 ELSE 0 END +
                        CASE WHEN curso_status = 2 THEN 1 ELSE 0 END +
                        CASE WHEN historico_academico_status = 2 THEN 1 ELSE 0 END +
                        CASE WHEN motivacao_fatec_status = 2 THEN 1 ELSE 0 END +
                        CASE WHEN historico_profissional_status = 2 THEN 1 ELSE 0 END +
                        CASE WHEN contatos_email_status = 2 THEN 1 ELSE 0 END +
                        CASE WHEN principais_conhecimentos_status = 2 THEN 1 ELSE 0 END +
                        CASE WHEN consideracoes_finais_status = 2 THEN 1 ELSE 0 END) AS reprov_ap
                FROM tg_apresentacao
                GROUP BY trabalho_id, versao
            ) ap ON ap.trabalho_id = tg.id AND ap.versao = tg.versao_atual

            -- Subquery para Seções (8 campos * 6 seções = 48)
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
                    ) AS valid_sec,
                    SUM(
                        (CASE WHEN empresa_parceira_status = 2 THEN 1 ELSE 0 END) +
                        (CASE WHEN problema_status = 2 THEN 1 ELSE 0 END) +
                        (CASE WHEN solucao_resumo_status = 2 THEN 1 ELSE 0 END) +
                        (CASE WHEN link_repositorio_status = 2 THEN 1 ELSE 0 END) +
                        (CASE WHEN tecnologias_status = 2 THEN 1 ELSE 0 END) +
                        (CASE WHEN contribuicoes_status = 2 THEN 1 ELSE 0 END) +
                        (CASE WHEN hard_skills_status = 2 THEN 1 ELSE 0 END) +
                        (CASE WHEN soft_skills_status = 2 THEN 1 ELSE 0 END)
                    ) AS reprov_sec
                FROM tg_secao
                GROUP BY trabalho_id, versao
            ) sec ON sec.trabalho_id = tg.id AND sec.versao = tg.versao_atual

            -- Subquery para Resumo (1 campo)
            LEFT JOIN (
                SELECT 
                    trabalho_id, versao,
                    (CASE WHEN versao_validada = 1 THEN 1 ELSE 0 END) AS valid_res,
                    (CASE WHEN versao_validada = 2 THEN 1 ELSE 0 END) AS reprov_res
                FROM tg_resumo
                GROUP BY trabalho_id, versao
            ) res ON res.trabalho_id = tg.id AND res.versao = tg.versao_atual
            
            WHERE tg.orientador_id = ?
        )
        -- Etapa 2: Agregar os KPIs para o Orientador
        SELECT
            -- Card 1: Total de Orientandos
            COUNT(DISTINCT aluno_id) AS totalOrientandos,
            
            -- Card 2: Total de Pendências (Total de 58 campos - Aprovados)
            SUM(58 - total_aprovados) AS totalPendencias,
            
            -- Card 3: TGs Concluídos (Aprovados = 58)
            SUM(CASE WHEN total_aprovados = 58 THEN 1 ELSE 0 END) AS totalConcluidos,
            
            -- Card 4: Alunos com Reprovações (Pelo menos 1 campo = 2)
            SUM(CASE WHEN total_reprovados > 0 THEN 1 ELSE 0 END) AS totalComReprovacoes
            
        FROM AlunoStats
        WHERE orientador_id = ?
    """;

    /**
     * Busca os 4 KPIs para o Dashboard do Orientador.
     */
    public Optional<KpisDTO> getKpis(long orientadorId) {
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(SQL_KPI)) {

            ps.setLong(1, orientadorId);
            ps.setLong(2, orientadorId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new KpisDTO(
                            rs.getInt("totalOrientandos"),
                            rs.getInt("totalPendencias"),
                            rs.getInt("totalConcluidos"),
                            rs.getInt("totalComReprovacoes")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Retorna DTO zerado em caso de falha
        return Optional.of(new KpisDTO(0, 0, 0, 0));
    }
}