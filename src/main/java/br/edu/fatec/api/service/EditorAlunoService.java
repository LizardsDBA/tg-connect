package br.edu.fatec.api.service;

import br.edu.fatec.api.config.Database;
import br.edu.fatec.api.dao.*;
import br.edu.fatec.api.nav.Session;
import br.edu.fatec.api.model.auth.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class EditorAlunoService {

    // Flag para facilitar desenvolvimento: validacao global ON/OFF
    private static final boolean VALIDAR_CAMPOS_OBRIGATORIOS = false;

    private final JdbcTrabalhosGraduacaoDao tgDao;
    private final JdbcTgApresentacaoDao apDao;
    private final JdbcTgSecaoDao secaoDao;
    private final JdbcTgResumoDao resumoDao;
    private final JdbcVersoesTrabalhoDao versoesDao;

    public EditorAlunoService() {
        this.tgDao = new JdbcTrabalhosGraduacaoDao();
        this.apDao = new JdbcTgApresentacaoDao();
        this.secaoDao = new JdbcTgSecaoDao();
        this.resumoDao = new JdbcTgResumoDao();
        this.versoesDao = new JdbcVersoesTrabalhoDao();
    }

    // Resolve trabalho_id do aluno logado (SQL direto para evitar método ambíguo)
    public long resolveTrabalhoIdDoAlunoLogado() {
        User u = Session.getUser();
        if (u == null) throw new IllegalStateException("Usuário não logado.");

        final String sql = "SELECT id FROM trabalhos_graduacao WHERE aluno_id=? LIMIT 1";
        try (var con = Database.get(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, u.getId());
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao resolver trabalho do aluno: " + e.getMessage(), e);
        }
        throw new IllegalStateException("Trabalho de graduação não encontrado para o aluno.");
    }

    // Calcula próxima versão com base no que já existe no banco (v1, v1.1, v1.2, ...)
    private String proximaVersaoFromDb(long trabalhoId) {
        String sql =
                "WITH v AS ( " +
                        "  SELECT versao FROM versoes_trabalho WHERE trabalho_id=? " +
                        "  UNION ALL " +
                        "  SELECT versao FROM tg_apresentacao WHERE trabalho_id=? " +
                        "  UNION ALL " +
                        "  SELECT versao FROM tg_resumo WHERE trabalho_id=? " +
                        "  UNION ALL " +
                        "  SELECT versao FROM tg_secao WHERE trabalho_id=? " +
                        ") " +
                        "SELECT CASE " +
                        "  WHEN COUNT(*)=0 THEN 'v1' " +
                        "  WHEN SUM(versao='v1')>0 OR SUM(versao LIKE 'v1.%')>0 THEN " +
                        "       CONCAT('v1.', COALESCE(MAX(CASE WHEN versao LIKE 'v1.%' " +
                        "              THEN CAST(SUBSTRING(versao,4) AS UNSIGNED) END), 0) + 1) " +
                        "  ELSE 'v1' " +
                        "END AS prox FROM v";

        try (var con = Database.get(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setLong(2, trabalhoId);
            ps.setLong(3, trabalhoId);
            ps.setLong(4, trabalhoId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception e) {
            // fallback seguro
            return "v1";
        }
        return "v1";
    }

    // DTO com todos os campos das abas (1..9)
    public static final class DadosEditor {
        // Aba 1
        public String infoPessoais;
        public String historicoAcad;
        public String motivacao;
        public String historicoProf;
        public String contatos;
        public String conhecimentos;
        public String consideracoesFinais; // Aba 9

        // Abas 2..7 (API 1..6)
        public String api1Empresa, api1Problema, api1Solucao, api1Repo, api1Tecnologias, api1Contrib, api1Hard, api1Soft;
        public String api2Empresa, api2Problema, api2Solucao, api2Repo, api2Tecnologias, api2Contrib, api2Hard, api2Soft;
        public String api3Empresa, api3Problema, api3Solucao, api3Repo, api3Tecnologias, api3Contrib, api3Hard, api3Soft;
        public String api4Empresa, api4Problema, api4Solucao, api4Repo, api4Tecnologias, api4Contrib, api4Hard, api4Soft;
        public String api5Empresa, api5Problema, api5Solucao, api5Repo, api5Tecnologias, api5Contrib, api5Hard, api5Soft;
        public String api6Empresa, api6Problema, api6Solucao, api6Repo, api6Tecnologias, api6Contrib, api6Hard, api6Soft;

        // Aba 8
        public String resumoMd;

        // MD consolidado (preview)
        public String mdCompleto;
    }

    // Validação simples (respeita a flag)
    private void validarObrigatorios(DadosEditor d) {
        if (!VALIDAR_CAMPOS_OBRIGATORIOS) return;

        if (isBlank(d.infoPessoais) || isBlank(d.historicoAcad) || isBlank(d.motivacao)
                || isBlank(d.historicoProf) || isBlank(d.contatos) || isBlank(d.conhecimentos)
                || isBlank(d.api1Empresa) || isBlank(d.api1Problema) || isBlank(d.api1Solucao)
                || isBlank(d.api2Empresa) || isBlank(d.api2Problema) || isBlank(d.api2Solucao)
                || isBlank(d.api3Empresa) || isBlank(d.api3Problema) || isBlank(d.api3Solucao)
                || isBlank(d.api4Empresa) || isBlank(d.api4Problema) || isBlank(d.api4Solucao)
                || isBlank(d.api5Empresa) || isBlank(d.api5Problema) || isBlank(d.api5Solucao)
                || isBlank(d.api6Empresa) || isBlank(d.api6Problema) || isBlank(d.api6Solucao)
                || isBlank(d.resumoMd) || isBlank(d.consideracoesFinais)) {
            throw new IllegalArgumentException("Existem campos obrigatórios vazios. Preencha todas as abas.");
        }
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    public String salvarTudo(long trabalhoId, DadosEditor d) {
        Objects.requireNonNull(d, "Dados do editor são obrigatórios.");

        validarObrigatorios(d);

        String novaVersao = proximaVersaoFromDb(trabalhoId);

        try (Connection con = Database.get()) {
            con.setAutoCommit(false);

            // 1) Apresentação (Aba 1 + Aba 9)
            apDao.insertVersao(con, trabalhoId, novaVersao,
                    safe(d.infoPessoais), safe(d.historicoAcad), safe(d.motivacao),
                    safe(d.historicoProf), safe(d.contatos), safe(d.conhecimentos),
                    safe(d.consideracoesFinais));

            // 2) Seções API 1..6
            secaoDao.insertVersao(con, trabalhoId, novaVersao, 1,
                    d.api1Empresa, d.api1Problema, d.api1Solucao, d.api1Repo,
                    d.api1Tecnologias, d.api1Contrib, d.api1Hard, d.api1Soft, null);
            secaoDao.insertVersao(con, trabalhoId, novaVersao, 2,
                    d.api2Empresa, d.api2Problema, d.api2Solucao, d.api2Repo,
                    d.api2Tecnologias, d.api2Contrib, d.api2Hard, d.api2Soft, null);
            secaoDao.insertVersao(con, trabalhoId, novaVersao, 3,
                    d.api3Empresa, d.api3Problema, d.api3Solucao, d.api3Repo,
                    d.api3Tecnologias, d.api3Contrib, d.api3Hard, d.api3Soft, null);
            secaoDao.insertVersao(con, trabalhoId, novaVersao, 4,
                    d.api4Empresa, d.api4Problema, d.api4Solucao, d.api4Repo,
                    d.api4Tecnologias, d.api4Contrib, d.api4Hard, d.api4Soft, null);
            secaoDao.insertVersao(con, trabalhoId, novaVersao, 5,
                    d.api5Empresa, d.api5Problema, d.api5Solucao, d.api5Repo,
                    d.api5Tecnologias, d.api5Contrib, d.api5Hard, d.api5Soft, null);
            secaoDao.insertVersao(con, trabalhoId, novaVersao, 6,
                    d.api6Empresa, d.api6Problema, d.api6Solucao, d.api6Repo,
                    d.api6Tecnologias, d.api6Contrib, d.api6Hard, d.api6Soft, null);

            // 3) Resumo (Aba 8) — versionado agora
            resumoDao.insertVersao(con, trabalhoId, novaVersao, d.resumoMd);

            // 4) Versão consolidada (para comparação)
            versoesDao.insertCompleto(con, trabalhoId, "COMPLETO", novaVersao, d.mdCompleto, null);

            // 5) Copiar flags de validação da última versão para a nova (se existirem)
            copiarFlagsValidacaoDaUltimaVersaoParaNova(con, trabalhoId, novaVersao);

            // 6) Atualiza versao_atual no TG
            tgDao.updateVersaoAtual(con, trabalhoId, novaVersao);

            con.commit();
            return novaVersao;
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao salvar tudo (rollback executado): " + e.getMessage(), e);
        }
    }

    private String safe(String s){ return s == null ? "" : s.trim(); }

    // ====== Leitura para preencher a UI ======

    public static final class DadosEditorLeitura {
        // Aba 1
        public String infoPessoais, historicoAcad, motivacao, historicoProf, contatos, conhecimentos, consideracoes;
        // Abas 2..7
        public String api1Empresa, api1Problema, api1Solucao, api1Repo, api1Tecnologias, api1Contrib, api1Hard, api1Soft;
        public String api2Empresa, api2Problema, api2Solucao, api2Repo, api2Tecnologias, api2Contrib, api2Hard, api2Soft;
        public String api3Empresa, api3Problema, api3Solucao, api3Repo, api3Tecnologias, api3Contrib, api3Hard, api3Soft;
        public String api4Empresa, api4Problema, api4Solucao, api4Repo, api4Tecnologias, api4Contrib, api4Hard, api4Soft;
        public String api5Empresa, api5Problema, api5Solucao, api5Repo, api5Tecnologias, api5Contrib, api5Hard, api5Soft;
        public String api6Empresa, api6Problema, api6Solucao, api6Repo, api6Tecnologias, api6Contrib, api6Hard, api6Soft;
        // Aba 8
        public String resumoMd;
    }

    public Optional<DadosEditorLeitura> carregarTudo(long trabalhoId) {
        String versao = fetchVersaoAtual(trabalhoId);
        if (versao == null || versao.isBlank()) return Optional.empty();

        DadosEditorLeitura d = new DadosEditorLeitura();

        // apresentação
        apDao.findByTrabalhoIdAndVersao(trabalhoId, versao).ifPresent(a -> {
            d.infoPessoais  = nz(a.nomeCompleto);
            d.historicoAcad = nz(a.historicoAcad);
            d.motivacao     = nz(a.motivacao);
            d.historicoProf = nz(a.historicoProf);
            d.contatos      = nz(a.contatosEmailOuLivre);
            d.conhecimentos = nz(a.conhecimentos);
            d.consideracoes = nz(a.consideracoes);
        });

        // seções API
        var secoes = secaoDao.findByTrabalhoIdAndVersao(trabalhoId, versao);
        for (var s : secoes) {
            switch (s.semestreApi) {
                case 1 -> { d.api1Empresa=s.empresa; d.api1Problema=s.problema; d.api1Solucao=s.solucao; d.api1Repo=s.repo;
                    d.api1Tecnologias=s.tecnologias; d.api1Contrib=s.contrib; d.api1Hard=s.hard; d.api1Soft=s.soft; }
                case 2 -> { d.api2Empresa=s.empresa; d.api2Problema=s.problema; d.api2Solucao=s.solucao; d.api2Repo=s.repo;
                    d.api2Tecnologias=s.tecnologias; d.api2Contrib=s.contrib; d.api2Hard=s.hard; d.api2Soft=s.soft; }
                case 3 -> { d.api3Empresa=s.empresa; d.api3Problema=s.problema; d.api3Solucao=s.solucao; d.api3Repo=s.repo;
                    d.api3Tecnologias=s.tecnologias; d.api3Contrib=s.contrib; d.api3Hard=s.hard; d.api3Soft=s.soft; }
                case 4 -> { d.api4Empresa=s.empresa; d.api4Problema=s.problema; d.api4Solucao=s.solucao; d.api4Repo=s.repo;
                    d.api4Tecnologias=s.tecnologias; d.api4Contrib=s.contrib; d.api4Hard=s.hard; d.api4Soft=s.soft; }
                case 5 -> { d.api5Empresa=s.empresa; d.api5Problema=s.problema; d.api5Solucao=s.solucao; d.api5Repo=s.repo;
                    d.api5Tecnologias=s.tecnologias; d.api5Contrib=s.contrib; d.api5Hard=s.hard; d.api5Soft=s.soft; }
                case 6 -> { d.api6Empresa=s.empresa; d.api6Problema=s.problema; d.api6Solucao=s.solucao; d.api6Repo=s.repo;
                    d.api6Tecnologias=s.tecnologias; d.api6Contrib=s.contrib; d.api6Hard=s.hard; d.api6Soft=s.soft; }
            }
        }

        // resumo
        resumoDao.findByTrabalhoIdAndVersao(trabalhoId, versao)
                .ifPresent(r -> d.resumoMd = nz(r.resumoMd()));

        return Optional.of(d);
    }

    // ===== helpers =====
    private String nz(String s){ return s == null ? "" : s; }

    private String fetchVersaoAtual(long trabalhoId) {
        final String sql = "SELECT versao_atual FROM trabalhos_graduacao WHERE id=? LIMIT 1";
        try (var con = Database.get(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception e) {
            // silencioso
        }
        return null;
    }

    // ====== NOVO: indicador de validação por aba ======
    public boolean isAbaValidada(long trabalhoId, int abaNumero) throws SQLException {
        String versao = fetchVersaoAtual(trabalhoId);
        if (versao == null || versao.isBlank()) return false;

        // 1: apresentação.apresentacao_versao_validada
        if (abaNumero == 1) {
            return apDao.getApresentacaoValidada(trabalhoId, versao) == 1;
        }
        // 9: apresentação.consideracao_versao_validada
        if (abaNumero == 9) {
            return apDao.getConsideracaoValidada(trabalhoId, versao) == 1;
        }
        // 8: resumo.versao_validada
        if (abaNumero == 8) {
            return resumoDao.getValidacaoResumo(trabalhoId, versao) == 1;
        }
        // 2..7: secao(semestre=1..6).versao_validada
        if (abaNumero >= 2 && abaNumero <= 7) {
            int semestre = abaNumero - 1;
            Integer v = secaoDao.getValidacaoSecao(trabalhoId, versao, semestre);
            return v != null && v == 1;
        }
        return false;
    }

    // ====== NOVO: copiar flags de validação da última versão para a nova ======
    private void copiarFlagsValidacaoDaUltimaVersaoParaNova(Connection con, long trabalhoId, String novaVersao) throws SQLException {
        // Apresentação (aba 1 e 9)
        apDao.copyValidacaoFromUltimaVersao(con, trabalhoId, novaVersao);
        // Resumo (aba 8)
        resumoDao.copyValidacaoFromUltimaVersao(con, trabalhoId, novaVersao);
        // Seções API (abas 2..7)
        secaoDao.copyValidacaoFromUltimaVersao(con, trabalhoId, novaVersao);
    }
}
