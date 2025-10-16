USE tg_connect;

-- =========================================================
-- USUÁRIOS E PAPÉIS (sem trigger; usamos ON UPDATE na coluna)
-- =========================================================
CREATE TABLE IF NOT EXISTS usuarios (
                                        id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        nome       VARCHAR(150) NOT NULL,
                                        email      VARCHAR(190) NOT NULL UNIQUE,
                                        senha_hash VARCHAR(255) NOT NULL,
                                        tipo       ENUM('ALUNO','ORIENTADOR','COORDENADOR') NOT NULL,
                                        ativo      BOOLEAN NOT NULL DEFAULT TRUE,
                                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                        INDEX idx_usuarios_tipo (tipo)
) ENGINE=InnoDB;

-- =========================================================
-- ORIENTAÇÕES
-- =========================================================
CREATE TABLE IF NOT EXISTS orientacoes (
                                           id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           aluno_id       BIGINT NOT NULL,
                                           orientador_id  BIGINT NOT NULL,
                                           ativo          BOOLEAN NOT NULL DEFAULT TRUE,
                                           criado_em      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           FOREIGN KEY (aluno_id)      REFERENCES usuarios(id),
                                           FOREIGN KEY (orientador_id) REFERENCES usuarios(id),
                                           UNIQUE KEY uk_orientacao_ativa (aluno_id, orientador_id, ativo)
) ENGINE=InnoDB;

-- =========================================================
-- TRABALHO DE GRADUAÇÃO (TG) + CONTROLE
-- =========================================================
CREATE TABLE IF NOT EXISTS trabalhos_graduacao (
                                                   id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   aluno_id              BIGINT NOT NULL,
                                                   orientador_id         BIGINT NULL,
                                                   titulo                VARCHAR(200) NULL,
                                                   tema                  VARCHAR(200) NULL,
                                                   versao_atual          VARCHAR(20) NULL,
                                                   status                ENUM('EM_ANDAMENTO','ENTREGUE','APROVADO','REPROVADO') NOT NULL DEFAULT 'EM_ANDAMENTO',
                                                   percentual_conclusao  DECIMAL(5,2) NOT NULL DEFAULT 0.00,
                                                   data_inicio           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                   data_limite           DATETIME NULL,
                                                   data_entrega          DATETIME NULL,
                                                   FOREIGN KEY (aluno_id)      REFERENCES usuarios(id),
                                                   FOREIGN KEY (orientador_id) REFERENCES usuarios(id),
                                                   UNIQUE KEY uk_tg_aluno (aluno_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS versoes_trabalho (
                                                id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                trabalho_id  BIGINT NOT NULL,
                                                secao        VARCHAR(100) NOT NULL, -- usar 'COMPLETO'
                                                versao       VARCHAR(20)  NOT NULL,
                                                conteudo_md  LONGTEXT     NOT NULL,
                                                comentario   TEXT NULL,
                                                created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                FOREIGN KEY (trabalho_id) REFERENCES trabalhos_graduacao(id),
                                                KEY idx_vt_trab_secao (trabalho_id, secao),
                                                UNIQUE KEY uk_vt_trab_versao_secao (trabalho_id, versao, secao)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pareceres (
                                         id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         versao_id     BIGINT NOT NULL,
                                         orientador_id BIGINT NOT NULL,
                                         status        ENUM('ACEITO','AJUSTES','REJEITADO') NOT NULL,
                                         comentario    TEXT NULL,
                                         created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         FOREIGN KEY (versao_id)     REFERENCES versoes_trabalho(id),
                                         FOREIGN KEY (orientador_id) REFERENCES usuarios(id)
) ENGINE=InnoDB;

-- =========================================================
-- TABELAS DO TG (versionadas)
-- (removidos ano/semestre_letivo de tg_secao; contatos_email = bloco inteiro)
-- =========================================================
CREATE TABLE IF NOT EXISTS tg_apresentacao (
                                               id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               trabalho_id              BIGINT NOT NULL,
                                               versao                   VARCHAR(20) NOT NULL,
                                               nome_completo            LONGTEXT NULL,
                                               idade                    INT NULL,
                                               curso                    LONGTEXT NULL,
                                               historico_academico      LONGTEXT NULL,
                                               motivacao_fatec          LONGTEXT NULL,
                                               historico_profissional   LONGTEXT NULL,
                                               contatos_email           LONGTEXT NULL,
                                               principais_conhecimentos LONGTEXT NULL,
                                               consideracoes_finais     LONGTEXT NULL,
                                               updated_at               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                               FOREIGN KEY (trabalho_id) REFERENCES trabalhos_graduacao(id),
                                               UNIQUE KEY uk_apresentacao_trab_versao (trabalho_id, versao)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tg_secao (
                                        id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        trabalho_id       BIGINT NOT NULL,
                                        versao            VARCHAR(20) NOT NULL,
                                        semestre_api      TINYINT NOT NULL CHECK (semestre_api BETWEEN 1 AND 6),
                                        empresa_parceira  VARCHAR(150) NULL,
                                        problema          LONGTEXT NULL,
                                        solucao_resumo    LONGTEXT NULL,
                                        link_repositorio  VARCHAR(300) NULL,
                                        tecnologias       LONGTEXT NULL,
                                        contribuicoes     LONGTEXT NULL,
                                        hard_skills       LONGTEXT NULL,
                                        soft_skills       LONGTEXT NULL,
                                        conteudo_md       LONGTEXT NULL,
                                        created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at        DATETIME NULL,
                                        FOREIGN KEY (trabalho_id) REFERENCES trabalhos_graduacao(id),
                                        UNIQUE KEY uk_trab_versao_semestre_api (trabalho_id, versao, semestre_api),
                                        KEY idx_secao_trab_versao (trabalho_id, versao)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tg_resumo (
                                         id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         trabalho_id   BIGINT NOT NULL,
                                         versao        VARCHAR(20) NOT NULL,
                                         resumo_md     LONGTEXT NOT NULL,
                                         kpis          DECIMAL(5,2) NOT NULL DEFAULT 0.00, -- percentual de conclusão
                                         created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                         FOREIGN KEY (trabalho_id) REFERENCES trabalhos_graduacao(id),
                                         UNIQUE KEY uk_resumo_trab_versao (trabalho_id, versao)
) ENGINE=InnoDB;

-- =========================================================
-- MENSAGENS / NOTIFICAÇÕES / ANEXOS / ENTREGAS
-- =========================================================
CREATE TABLE IF NOT EXISTS mensagens (
                                         id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         trabalho_id      BIGINT NOT NULL,
                                         remetente_id     BIGINT NOT NULL,
                                         destinatario_id  BIGINT NOT NULL,
                                         secao            VARCHAR(100) NULL,
                                         tipo             ENUM('TEXTO','SISTEMA') NOT NULL DEFAULT 'TEXTO',
                                         conteudo         LONGTEXT NOT NULL,
                                         lida             BOOLEAN NOT NULL DEFAULT FALSE,
                                         created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         FOREIGN KEY (trabalho_id)     REFERENCES trabalhos_graduacao(id),
                                         FOREIGN KEY (remetente_id)    REFERENCES usuarios(id),
                                         FOREIGN KEY (destinatario_id) REFERENCES usuarios(id),
                                         INDEX idx_msg_trab_secao (trabalho_id, secao)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS anexos (
                                      id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      trabalho_id      BIGINT NOT NULL,
                                      versao_id        BIGINT NULL,
                                      mensagem_id      BIGINT NULL,
                                      arquivo_nome     VARCHAR(255) NOT NULL,
                                      arquivo_mime     VARCHAR(120) NULL,
                                      arquivo_tamanho  BIGINT NULL,
                                      arquivo_path     VARCHAR(500) NOT NULL,
                                      uploaded_by      BIGINT NOT NULL,
                                      created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      FOREIGN KEY (trabalho_id) REFERENCES trabalhos_graduacao(id),
                                      FOREIGN KEY (versao_id)   REFERENCES versoes_trabalho(id),
                                      FOREIGN KEY (mensagem_id) REFERENCES mensagens(id),
                                      FOREIGN KEY (uploaded_by) REFERENCES usuarios(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notificacoes (
                                            id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            usuario_id  BIGINT NOT NULL,
                                            titulo      VARCHAR(150) NOT NULL,
                                            conteudo    TEXT NULL,
                                            tipo        ENUM('INFO','WARN','ACTION') NOT NULL DEFAULT 'INFO',
                                            lida        BOOLEAN NOT NULL DEFAULT FALSE,
                                            created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
                                            INDEX idx_notif_usuario (usuario_id, lida)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS entregas (
                                        id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        trabalho_id  BIGINT NOT NULL,
                                        nome         VARCHAR(150) NOT NULL,
                                        descricao    TEXT NULL,
                                        prazo        DATETIME NULL,
                                        status       ENUM('PENDENTE','EM_REVISAO','CONCLUIDA') NOT NULL DEFAULT 'PENDENTE',
                                        nota         DECIMAL(5,2) NULL,
                                        created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at   DATETIME NULL,
                                        FOREIGN KEY (trabalho_id) REFERENCES trabalhos_graduacao(id)
) ENGINE=InnoDB;

-- =========================================================
-- SEEDS (senha padrão 123456) — usando variáveis para evitar JOINs na seed
-- =========================================================

-- Usuários
INSERT INTO usuarios (nome, email, senha_hash, tipo)
SELECT 'Alice Aluna', 'aluna@exemplo.com', SHA2('123456',256), 'ALUNO'
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email='aluna@exemplo.com');

INSERT INTO usuarios (nome, email, senha_hash, tipo)
SELECT 'Otávio Orientador', 'orientador@exemplo.com', SHA2('123456',256), 'ORIENTADOR'
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email='orientador@exemplo.com');

INSERT INTO usuarios (nome, email, senha_hash, tipo)
SELECT 'Carla Coordenadora', 'coordenador@exemplo.com', SHA2('123456',256), 'COORDENADOR'
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email='coordenador@exemplo.com');

-- Orientação (ativa)
SET @aluno_id := (SELECT id FROM usuarios WHERE email='aluna@exemplo.com');
SET @orientador_id := (SELECT id FROM usuarios WHERE email='orientador@exemplo.com');

INSERT INTO orientacoes (aluno_id, orientador_id, ativo)
SELECT @aluno_id, @orientador_id, TRUE
WHERE @aluno_id IS NOT NULL AND @orientador_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM orientacoes o
    WHERE o.aluno_id = @aluno_id AND o.orientador_id = @orientador_id AND o.ativo = TRUE
);

-- TG do aluno
INSERT INTO trabalhos_graduacao (aluno_id, orientador_id, titulo, tema, versao_atual, percentual_conclusao)
SELECT @aluno_id, @orientador_id, 'Portfólio TG - Alice', 'Histórico de APIs e Portfólio', 'v1', 0.00
WHERE @aluno_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM trabalhos_graduacao t WHERE t.aluno_id = @aluno_id);

SET @tg_id := (SELECT id FROM trabalhos_graduacao WHERE aluno_id=@aluno_id);

-- Apresentação v1 (contatos_email = bloco inteiro)
INSERT INTO tg_apresentacao (
    trabalho_id, versao, nome_completo, idade, curso,
    historico_academico, motivacao_fatec, historico_profissional,
    contatos_email, principais_conhecimentos, consideracoes_finais
)
SELECT @tg_id, 'v1',
       'Fagner Louis Silva Nascimento', 28,
       'Tecnólogo em Banco de Dados (FATEC São José dos Campos – Prof. Jessen Vidal)',
       'Graduação atual: Banco de Dados – FATEC SJC; Pós: Administração de BD – Anhanguera; Curso anterior: ADS – ETEP; Técnico: Informática – ETEC Jacareí',
       'Sempre fui fascinado por dados e pela forma como eles podem contar histórias e orientar decisões...',
       'Atuo na área de tecnologia desde 2017: suporte, análise de BD, SAP HANA...',
       '- E-mail: fagner.louis.dev@gmail.com | GitHub: github.com/FagnerLouis | LinkedIn: linkedin.com/in/fagnerlouis',
       'PostgreSQL, SAP HANA, MySQL, Java, Python, Spring Boot, Git, API REST, Scrum, Kanban',
       'Durante a trajetória na FATEC consolidei conhecimentos técnicos e interpessoais.'
WHERE @tg_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM tg_apresentacao ap WHERE ap.trabalho_id = @tg_id AND ap.versao='v1'
);

-- Seção API 1 (v1)
INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT @tg_id, 'v1', 1,
       'FATEC São José dos Campos – Prof. Fabiano Sabha Walczak',
       'Gestão de solicitações de manutenção dispersa...',
       'Sistema web de abertura e acompanhamento de chamados internos...',
       'https://github.com/LizardsDBA/API-2025-1',
       'Java; Spring Boot; PostgreSQL; JavaFX; HTML/CSS; GitHub',
       'Modelagem BD; entidades; repositórios; autenticação JWT; CI/CD',
       'Java/Spring Boot (ensino); PostgreSQL (ensino); Git/GitHub (autonomia)',
       'Comunicação, proatividade e resiliência',
       'Markdown resumido da API 1.'
WHERE @tg_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM tg_secao s WHERE s.trabalho_id = @tg_id AND s.versao='v1' AND s.semestre_api=1
);

-- Resumo v1: KPI numérico simples (0.00)
INSERT INTO tg_resumo (trabalho_id, versao, resumo_md, kpis)
SELECT @tg_id, 'v1',
       '## Tabela Resumo dos Projetos API

| Semestre | Empresa Parceira | Solução |
|---|---|---|
| 1º | FATEC SJC | Sistema Web de Chamados |
| 2º | FATEC SJC | Sistema Web de Chamados |
| 3º | FATEC SJC | Sistema Web de Chamados |
| 4º | FATEC SJC | Sistema Web de Chamados |
| 5º | FATEC SJC | Sistema Web de Chamados |
| 6º | FATEC SJC | Sistema Web de Chamados |',
       0.00
WHERE @tg_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM tg_resumo r WHERE r.trabalho_id = @tg_id AND r.versao='v1'
);

-- Versão consolidada v1
INSERT INTO versoes_trabalho (trabalho_id, secao, versao, conteudo_md, comentario)
SELECT @tg_id, 'COMPLETO', 'v1',
       '# Portfólio TG - v1

Conteúdo consolidado inicial.',
       'Seed inicial.'
WHERE @tg_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM versoes_trabalho v WHERE v.trabalho_id = @tg_id AND v.secao='COMPLETO' AND v.versao='v1'
);
