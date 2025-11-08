package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JdbcFeedbackDao {

    public boolean atualizarStatusCampoApresentacao(Long trabalhoId, String versao, String nomeColuna, int novoStatus) throws SQLException {
        // Lista de campos permitidos para evitar SQL Injection
        List<String> camposPermitidos = List.of(
                "nome_completo", "idade", "curso", "historico_academico",
                "motivacao_fatec", "historico_profissional", "contatos_email",
                "principais_conhecimentos", "consideracoes_finais"
        );

        if (!camposPermitidos.contains(nomeColuna)) {
            throw new SQLException("Nome de coluna inválido ou não permitido: " + nomeColuna);
        }

        // Constrói a query com segurança (pois o nome da coluna foi validado)
        String sql = String.format(
                "UPDATE tg_apresentacao SET %s_status = ? WHERE trabalho_id = ? AND versao = ?",
                nomeColuna // nome_completo_status, idade_status, etc.
        );

        return execUpdate(sql, p -> {
            p.setInt(1, novoStatus);
            p.setLong(2, trabalhoId);
            p.setString(3, versao);
        });
    }

    // ADICIONE ESTE MÉTODO
    /**
     * Atualiza o status (0, 1, 2) da versão inteira do Resumo.
     */
    public boolean atualizarStatusResumo(Long trabalhoId, String versao, int novoStatus) throws SQLException {
        String sql = "UPDATE tg_resumo SET versao_validada = ? WHERE trabalho_id = ? AND versao = ?";

        return execUpdate(sql, p -> {
            p.setInt(1, novoStatus);
            p.setLong(2, trabalhoId);
            p.setString(3, versao);
        });
    }

    public boolean atualizarStatusCampoApi(Long trabalhoId, String versao, int semestreApi, String nomeColuna, int novoStatus) throws SQLException {
        // Lista de campos permitidos para evitar SQL Injection
        List<String> camposPermitidos = List.of(
                "empresa_parceira", "problema", "solucao_resumo", "link_repositorio",
                "tecnologias", "contribuicoes", "hard_skills", "soft_skills"
        );

        if (!camposPermitidos.contains(nomeColuna)) {
            throw new SQLException("Nome de coluna da API inválido ou não permitido: " + nomeColuna);
        }

        // Constrói a query com segurança
        String sql = String.format(
                // CORREÇÃO: "WHERE trabalhoId = ?" foi trocado para "WHERE trabalho_id = ?"
                "UPDATE tg_secao SET %s_status = ? WHERE trabalho_id = ? AND versao = ? AND semestre_api = ?",
                nomeColuna // ex: problema_status
        );

        return execUpdate(sql, p -> {
            p.setInt(1, novoStatus);
            p.setLong(2, trabalhoId);
            p.setString(3, versao);
            p.setInt(4, semestreApi);
        });
    }

    public String carregarPreviewCompleto(Long trabalhoId, String versao) throws SQLException {
        // Usa 'COMPLETO' conforme sua definição de tabela
        String sql = "SELECT conteudo_md FROM versoes_trabalho WHERE trabalho_id = ? AND versao = ? LIMIT 1";

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("conteudo_md");
                }
            }
        }
        // Retorna uma string de aviso se não encontrar
        return "# Preview não encontrado\n\nNão foi possível localizar o conteúdo consolidado para esta versão.";
    }

    // ====== PARTES do TG ======
    public enum Parte { APRESENTACAO, API1, API2, API3, API4, API5, API6, RESUMO, FINAIS }

    // ====== DTOs de Saída (NOVOS) ======
    // DTO para carregar os campos individuais da Apresentação
    public record ApresentacaoCamposDTO(
            String versao, boolean concluida,
            String nomeCompleto, int nomeCompletoStatus,
            String idade, int idadeStatus,
            String curso, int cursoStatus,
            String historicoAcademico, int historicoAcademicoStatus,
            String motivacaoFatec, int motivacaoFatecStatus,
            String historicoProfissional, int historicoProfissionalStatus,
            String contatosEmail, int contatosEmailStatus,
            String principaisConhecimentos, int principaisConhecimentosStatus,
            String consideracoesFinais, int consideracoesFinaisStatus
    ) {}

    // DTO para carregar os campos individuais do Resumo
    public record ResumoCamposDTO(
            String versao, boolean concluida,
            String resumoMd, int versaoValidada // status 0, 1, 2
    ) {}

    // DTO para carregar os campos individuais das APIs
    public record ApiCamposDTO(
            String versao, boolean concluida,
            String empresaParceira, int empresaParceiraStatus,
            String problema, int problemaStatus,
            String solucaoResumo, int solucaoResumoStatus,
            String linkRepositorio, int linkRepositorioStatus,
            String tecnologias, int tecnologiasStatus,
            String contribuicoes, int contribuicoesStatus,
            String hardSkills, int hardSkillsStatus,
            String softSkills, int softSkillsStatus
            // O campo 'conteudo_md' é um consolidado,
            // podemos decidir se o carregamos ou não.
    ) {}

    // DTOs Antigos (mantidos para listarOrientandos)
    public record OrientandoDTO(Long alunoId, String nome, Long trabalhoId) {}


    // ====== API Pública ======

    /** Lista orientandos ativos do professor. */
    public List<OrientandoDTO> listarOrientandos(Long orientadorId) throws SQLException {
        String sql = """
            SELECT u.id AS aluno_id, u.nome, t.id AS trabalho_id
              FROM usuarios u
              JOIN orientacoes o ON o.aluno_id = u.id AND o.ativo = TRUE
              LEFT JOIN trabalhos_graduacao t ON t.aluno_id = u.id
             WHERE o.orientador_id = ? AND u.ativo = TRUE
             ORDER BY u.nome
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orientadorId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrientandoDTO> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new OrientandoDTO(
                            rs.getLong("aluno_id"),
                            rs.getString("nome"),
                            rs.getObject("trabalho_id") == null ? null : rs.getLong("trabalho_id")
                    ));
                }
                return out;
            }
        }
    }

    /** Retorna trabalho_id pelo aluno_id. */
    public Long obterTrabalhoIdPorAluno(Long alunoId) throws SQLException {
        String sql = "SELECT id FROM trabalhos_graduacao WHERE aluno_id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Trabalho de Graduação não encontrado para o aluno " + alunoId);
    }

    // =================================================================================
    // MÉTODOS DE CARREGAMENTO DE CAMPOS (NOVOS)
    // =================================================================================

    /** Carrega os campos individuais da última versão da Apresentação. */
    public ApresentacaoCamposDTO carregarCamposApresentacao(Long trabalhoId) throws SQLException {
        String sql = """
            SELECT
                versao,
                nome_completo, nome_completo_status,
                idade, idade_status,
                curso, curso_status,
                historico_academico, historico_academico_status,
                motivacao_fatec, motivacao_fatec_status,
                historico_profissional, historico_profissional_status,
                contatos_email, contatos_email_status,
                principais_conhecimentos, principais_conhecimentos_status,
                consideracoes_finais, consideracoes_finais_status,
                
                -- Lógica de 'concluida' (true se TODOS os campos = 1)
                (   nome_completo_status = 1 AND
                    idade_status = 1 AND
                    curso_status = 1 AND
                    historico_academico_status = 1 AND
                    motivacao_fatec_status = 1 AND
                    historico_profissional_status = 1 AND
                    contatos_email_status = 1 AND
                    principais_conhecimentos_status = 1 AND
                    consideracoes_finais_status = 1
                ) AS concluida
                
            FROM tg_apresentacao
            WHERE trabalho_id = ?
            ORDER BY LENGTH(versao) DESC, versao DESC
            LIMIT 1
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ApresentacaoCamposDTO(
                            rs.getString("versao"),
                            rs.getBoolean("concluida"),
                            rs.getString("nome_completo"), rs.getInt("nome_completo_status"),
                            rs.getString("idade"), rs.getInt("idade_status"),
                            rs.getString("curso"), rs.getInt("curso_status"),
                            rs.getString("historico_academico"), rs.getInt("historico_academico_status"),
                            rs.getString("motivacao_fatec"), rs.getInt("motivacao_fatec_status"),
                            rs.getString("historico_profissional"), rs.getInt("historico_profissional_status"),
                            rs.getString("contatos_email"), rs.getInt("contatos_email_status"),
                            rs.getString("principais_conhecimentos"), rs.getInt("principais_conhecimentos_status"),
                            rs.getString("consideracoes_finais"), rs.getInt("consideracoes_finais_status")
                    );
                }
            }
        }
        // Retorna um DTO vazio se não houver dados
        return new ApresentacaoCamposDTO("—", false, null, 0, null, 0, null, 0, null, 0, null, 0, null, 0, null, 0, null, 0, null, 0);
    }

    /** Carrega os campos individuais da última versão do Resumo. */
    public ResumoCamposDTO carregarCamposResumo(Long trabalhoId) throws SQLException {
        String sql = """
            SELECT 
                versao, 
                resumo_md, 
                versao_validada,
                (versao_validada = 1) AS concluida
            FROM tg_resumo
            WHERE trabalho_id = ?
            ORDER BY LENGTH(versao) DESC, versao DESC
            LIMIT 1
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ResumoCamposDTO(
                            rs.getString("versao"),
                            rs.getBoolean("concluida"),
                            rs.getString("resumo_md"),
                            rs.getInt("versao_validada")
                    );
                }
            }
        }
        return new ResumoCamposDTO("—", false, null, 0);
    }

    /** Carrega os campos individuais da última versão de uma API (tg_secao). */
    public ApiCamposDTO carregarCamposApi(Long trabalhoId, int semestreApi) throws SQLException {
        String sql = """
            SELECT
                versao,
                empresa_parceira, empresa_parceira_status,
                problema, problema_status,
                solucao_resumo, solucao_resumo_status,
                link_repositorio, link_repositorio_status,
                tecnologias, tecnologias_status,
                contribuicoes, contribuicoes_status,
                hard_skills, hard_skills_status,
                soft_skills, soft_skills_status,
                
                -- Lógica de 'concluida' (true se TODOS os campos = 1)
                (   empresa_parceira_status = 1 AND
                    problema_status = 1 AND
                    solucao_resumo_status = 1 AND
                    link_repositorio_status = 1 AND
                    tecnologias_status = 1 AND
                    contribuicoes_status = 1 AND
                    hard_skills_status = 1 AND
                    soft_skills_status = 1
                    -- COALESCE(conteudo_md_status, 1) = 1 -- se 'conteudo_md' for opcional
                ) AS concluida
                
            FROM tg_secao
            WHERE trabalho_id = ? AND semestre_api = ?
            ORDER BY LENGTH(versao) DESC, versao DESC
            LIMIT 1
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setInt(2, semestreApi);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ApiCamposDTO(
                            rs.getString("versao"),
                            rs.getBoolean("concluida"),
                            rs.getString("empresa_parceira"), rs.getInt("empresa_parceira_status"),
                            rs.getString("problema"), rs.getInt("problema_status"),
                            rs.getString("solucao_resumo"), rs.getInt("solucao_resumo_status"),
                            rs.getString("link_repositorio"), rs.getInt("link_repositorio_status"),
                            rs.getString("tecnologias"), rs.getInt("tecnologias_status"),
                            rs.getString("contribuicoes"), rs.getInt("contribuicoes_status"),
                            rs.getString("hard_skills"), rs.getInt("hard_skills_status"),
                            rs.getString("soft_skills"), rs.getInt("soft_skills_status")
                    );
                }
            }
        }
        // Retorna DTO vazio
        return new ApiCamposDTO("—", false, null, 0, null, 0, null, 0, null, 0, null, 0, null, 0, null, 0, null, 0);
    }

    /** Carrega as considerações finais (que estão na tabela Apresentação) */
    public ApresentacaoCamposDTO carregarCamposFinais(Long trabalhoId) throws SQLException {
        // "Finais" é apenas um subconjunto da Apresentação,
        // então reutilizamos o método principal para evitar duplicar a lógica de SQL.
        return carregarCamposApresentacao(trabalhoId);
    }


    // =================================================================================
    // MÉTODOS DE SUPORTE (A MAIORIA JÁ ESTAVA CORRETA NO SEU ARQUIVO)
    // =================================================================================

    /** Envia comentário (mensagens) – aparece na tela de Chat. */
    public Long enviarComentario(Long trabalhoId, Long orientadorId, Long alunoId, Parte parte, String versaoRef, String texto)
            throws SQLException {
        // Este método estava OK, sem alterações necessárias.
        String sql = """
            INSERT INTO mensagens (trabalho_id, remetente_id, destinatario_id, secao, conteudo)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, trabalhoId);
            ps.setLong(2, orientadorId);
            ps.setLong(3, alunoId);
            ps.setString(4, secaoString(parte)); // APRESENTACAO | API1..API6 | RESUMO | FINAIS
            ps.setString(5, texto);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        return null;
    }

    /** (Deprecado?) Marca parte inteira como concluída. */
    public boolean marcarComoConcluida(Long trabalhoId, Parte parte, String versao) throws SQLException {
        // Seu arquivo já tinha a versão correta deste método (atualizando todos os status para 1).
        // Mantido como está.
        Objects.requireNonNull(versao, "versao não pode ser nula");

        return switch (parte) {
            case APRESENTACAO -> execUpdate("""
            UPDATE tg_apresentacao SET
                nome_completo_status = 1,
                idade_status = 1,
                curso_status = 1,
                historico_academico_status = 1,
                motivacao_fatec_status = 1,
                historico_profissional_status = 1,
                contatos_email_status = 1,
                principais_conhecimentos_status = 1,
                consideracoes_finais_status = 1
            WHERE trabalho_id = ? AND versao = ?
        """, p -> { p.setLong(1, trabalhoId); p.setString(2, versao); });

            case FINAIS -> execUpdate("""
            UPDATE tg_apresentacao SET consideracoes_finais_status = 1
            WHERE trabalho_id = ? AND versao = ?
        """, p -> { p.setLong(1, trabalhoId); p.setString(2, versao); });

            case RESUMO -> execUpdate("""
            UPDATE tg_resumo SET versao_validada = 1
            WHERE trabalho_id = ? AND versao = ?
        """, p -> { p.setLong(1, trabalhoId); p.setString(2, versao); });

            default -> {
                int api = apiIndex(parte);
                yield execUpdate("""
                UPDATE tg_secao SET
                    empresa_parceira_status = 1,
                    problema_status = 1,
                    solucao_resumo_status = 1,
                    link_repositorio_status = 1,
                    tecnologias_status = 1,
                    contribuicoes_status = 1,
                    hard_skills_status = 1,
                    soft_skills_status = 1,
                    conteudo_md_status = 1
                WHERE trabalho_id = ? AND semestre_api = ? AND versao = ?
            """, p -> { p.setLong(1, trabalhoId); p.setInt(2, api); p.setString(3, versao); });
            }
        };
    }

    /** Verifica se a versão inteira está concluída (status 1) */
    public boolean verificarConclusao(Long trabalhoId, Parte parte, String versao) throws SQLException {
        // Seu arquivo já tinha a versão correta deste método. Mantido como está.
        if (trabalhoId == null || parte == null || versao == null || versao.isBlank()) return false;

        String sql;
        switch (parte) {
            case APRESENTACAO -> {
                sql = """
                SELECT CASE WHEN
                    nome_completo_status = 1 AND
                    COALESCE(idade_status, 1) = 1 AND
                    curso_status = 1 AND
                    historico_academico_status = 1 AND
                    motivacao_fatec_status = 1 AND
                    historico_profissional_status = 1 AND
                    contatos_email_status = 1 AND
                    principais_conhecimentos_status = 1 AND
                    consideracoes_finais_status = 1
                THEN 1 ELSE 0 END AS ok
                FROM tg_apresentacao
                WHERE trabalho_id = ? AND versao = ?
                LIMIT 1
            """;
                try (Connection c = Database.get();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setLong(1, trabalhoId);
                    ps.setString(2, versao);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() && rs.getInt("ok") == 1;
                    }
                }
            }
            case RESUMO -> {
                sql = "SELECT (versao_validada = 1) AS ok FROM tg_resumo WHERE trabalho_id = ? AND versao = ? LIMIT 1";
                try (Connection c = Database.get();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setLong(1, trabalhoId);
                    ps.setString(2, versao);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() && rs.getBoolean("ok");
                    }
                }
            }
            case FINAIS -> {
                sql = """
                SELECT (consideracoes_finais_status = 1) AS ok
                FROM tg_apresentacao
                WHERE trabalho_id = ? AND versao = ?
                LIMIT 1
            """;
                try (Connection c = Database.get();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setLong(1, trabalhoId);
                    ps.setString(2, versao);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() && rs.getBoolean("ok");
                    }
                }
            }
            default -> { // API1..API6
                int semestreApi = apiIndex(parte);
                sql = """
                SELECT CASE WHEN
                    COALESCE(empresa_parceira_status,1) = 1 AND
                    problema_status = 1 AND
                    solucao_resumo_status = 1 AND
                    COALESCE(link_repositorio_status,1) = 1 AND
                    tecnologias_status = 1 AND
                    contribuicoes_status = 1 AND
                    hard_skills_status = 1 AND
                    soft_skills_status = 1 AND
                    COALESCE(conteudo_md_status,1) = 1
                THEN 1 ELSE 0 END AS ok
                FROM tg_secao
                WHERE trabalho_id = ? AND versao = ? AND semestre_api = ?
                LIMIT 1
            """;
                try (Connection c = Database.get();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setLong(1, trabalhoId);
                    ps.setString(2, versao);
                    ps.setInt(3, semestreApi);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() && rs.getInt("ok") == 1;
                    }
                }
            }
        }
    }


    // ====== Helpers ======
    // Nenhuma alteração necessária aqui

    private boolean execUpdate(String sql, Binder binder) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate() > 0;
        }
    }

    private String secaoString(Parte p) {
        return switch (p) {
            case APRESENTACAO -> "APRESENTACAO";
            case RESUMO       -> "RESUMO";
            case FINAIS       -> "FINAIS";
            default           -> p.name(); // API1..API6
        };
    }

    private int apiIndex(Parte p) {
        return switch (p) {
            case API1 -> 1;
            case API2 -> 2;
            case API3 -> 3;
            case API4 -> 4;
            case API5 -> 5;
            case API6 -> 6;
            default -> throw new IllegalArgumentException("Parte não é API: " + p);
        };
    }

    private Parte parteFromApi(int i) {
        return switch (i) {
            case 1 -> Parte.API1;
            case 2 -> Parte.API2;
            case 3 -> Parte.API3;
            case 4 -> Parte.API4;
            case 5 -> Parte.API5;
            case 6 -> Parte.API6;
            default -> throw new IllegalArgumentException("API inválida: " + i);
        };
    }

    @FunctionalInterface
    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }
}