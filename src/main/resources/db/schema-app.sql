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
                                           id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           aluno_id      BIGINT NOT NULL,
                                           orientador_id BIGINT NOT NULL,
                                           ativo         BOOLEAN NOT NULL DEFAULT TRUE,
                                           criado_em     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           FOREIGN KEY (aluno_id)      REFERENCES usuarios(id),
    FOREIGN KEY (orientador_id) REFERENCES usuarios(id),
    UNIQUE KEY uk_orientacao_ativa (aluno_id, orientador_id, ativo)
    ) ENGINE=InnoDB;

-- =========================================================
-- TRABALHO DE GRADUAÇÃO (TG) + CONTROLE
-- =========================================================
CREATE TABLE IF NOT EXISTS trabalhos_graduacao (
                                                   id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   aluno_id             BIGINT NOT NULL,
                                                   orientador_id        BIGINT NULL,
                                                   titulo               VARCHAR(200) NULL,
    tema                 VARCHAR(200) NULL,
    versao_atual         VARCHAR(20) NULL,
    status               ENUM('EM_ANDAMENTO','ENTREGUE','APROVADO','REPROVADO') NOT NULL DEFAULT 'EM_ANDAMENTO',
    percentual_conclusao DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    data_inicio          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_limite          DATETIME NULL,
    data_entrega         DATETIME NULL,
    FOREIGN KEY (aluno_id)      REFERENCES usuarios(id),
    FOREIGN KEY (orientador_id) REFERENCES usuarios(id),
    UNIQUE KEY uk_tg_aluno (aluno_id)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS versoes_trabalho (
                                                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                trabalho_id BIGINT NOT NULL,
                                                secao       VARCHAR(100) NOT NULL, -- usar 'COMPLETO'
    versao      VARCHAR(20)  NOT NULL,
    conteudo_md LONGTEXT     NOT NULL,
    comentario  TEXT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (trabalho_id) REFERENCES trabalhos_graduacao(id),
    KEY idx_vt_trab_secao (trabalho_id, secao),
    UNIQUE KEY uk_vt_trab_versao_secao (trabalho_id, versao, secao)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pareceres (
         id            BIGINT AUTO_INCREMENT PRIMARY KEY,
         trabalho_id   BIGINT NOT NULL,
         versao        VARCHAR(20) NOT NULL,
         orientador_id BIGINT NOT NULL,

-- O "Onde" (O campo exato)
         secao         VARCHAR(50) NOT NULL, -- Ex: 'APRESENTACAO', 'API1', 'RESUMO'
         campo_chave   VARCHAR(100) NOT NULL, -- Ex: 'nome_completo', 'problema'

-- O "O quê" (O feedback)
-- (O status 0,1,2 já está na tabela do campo,
-- mas salvá-lo aqui garante o histórico)
         status_campo  TINYINT NOT NULL,
         comentario    TEXT NULL,

         created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

         FOREIGN KEY (trabalho_id) REFERENCES trabalhos_graduacao(id),
         FOREIGN KEY (orientador_id) REFERENCES usuarios(id),

-- Índice para buscar todos os pareceres de uma versão/seção
         INDEX idx_parecer_lookup (trabalho_id, versao, secao)
) ENGINE=InnoDB;

-- =========================================================
-- TABELAS DO TG (versionadas) — NOVO PADRÃO *_status
-- =========================================================

CREATE TABLE IF NOT EXISTS tg_apresentacao (
   id                           BIGINT AUTO_INCREMENT PRIMARY KEY,
   trabalho_id                  BIGINT NOT NULL,
   versao                       VARCHAR(20) NOT NULL,

-- Campos de conteúdo (todos em Markdown)
   nome_completo                LONGTEXT NULL,
   idade                        INT NULL,
   curso                        VARCHAR(150) NULL,
   historico_academico          LONGTEXT NULL,
   motivacao_fatec              LONGTEXT NULL,
   historico_profissional       LONGTEXT NULL,
   contatos_email               LONGTEXT NULL,
   principais_conhecimentos     LONGTEXT NULL,
   consideracoes_finais         LONGTEXT NULL,

-- Status por campo (0 pendente, 1 aprovado, 2 reprovado)
   nome_completo_status            TINYINT NOT NULL DEFAULT 0 CHECK (nome_completo_status IN (0,1,2)),
   idade_status                    TINYINT NOT NULL DEFAULT 0 CHECK (idade_status IN (0,1,2)),
   curso_status                    TINYINT NOT NULL DEFAULT 0 CHECK (curso_status IN (0,1,2)),
   historico_academico_status      TINYINT NOT NULL DEFAULT 0 CHECK (historico_academico_status IN (0,1,2)),
   motivacao_fatec_status          TINYINT NOT NULL DEFAULT 0 CHECK (motivacao_fatec_status IN (0,1,2)),
   historico_profissional_status   TINYINT NOT NULL DEFAULT 0 CHECK (historico_profissional_status IN (0,1,2)),
   contatos_email_status           TINYINT NOT NULL DEFAULT 0 CHECK (contatos_email_status IN (0,1,2)),
   principais_conhecimentos_status TINYINT NOT NULL DEFAULT 0 CHECK (principais_conhecimentos_status IN (0,1,2)),
   consideracoes_finais_status     TINYINT NOT NULL DEFAULT 0 CHECK (consideracoes_finais_status IN (0,1,2)),

   created_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

   FOREIGN KEY (trabalho_id) REFERENCES trabalhos_graduacao(id),
   UNIQUE KEY uk_apresentacao_trab_versao (trabalho_id, versao)
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS tg_secao (
id                BIGINT AUTO_INCREMENT PRIMARY KEY,
trabalho_id       BIGINT NOT NULL,
versao            VARCHAR(20) NOT NULL,

semestre_api      TINYINT NOT NULL CHECK (semestre_api BETWEEN 1 AND 6),

-- Campos de conteúdo (todos em Markdown, exceto link)
empresa_parceira  VARCHAR(150) NULL,
problema          LONGTEXT NULL,
solucao_resumo    LONGTEXT NULL,
link_repositorio  VARCHAR(300) NULL,
tecnologias       LONGTEXT NULL,
contribuicoes     LONGTEXT NULL,
hard_skills       LONGTEXT NULL,
soft_skills       LONGTEXT NULL,
conteudo_md       LONGTEXT NULL,

-- Status por campo (0 pendente, 1 aprovado, 2 reprovado)
empresa_parceira_status  TINYINT NOT NULL DEFAULT 0 CHECK (empresa_parceira_status IN (0,1,2)),
problema_status          TINYINT NOT NULL DEFAULT 0 CHECK (problema_status IN (0,1,2)),
solucao_resumo_status    TINYINT NOT NULL DEFAULT 0 CHECK (solucao_resumo_status IN (0,1,2)),
link_repositorio_status  TINYINT NOT NULL DEFAULT 0 CHECK (link_repositorio_status IN (0,1,2)),
tecnologias_status       TINYINT NOT NULL DEFAULT 0 CHECK (tecnologias_status IN (0,1,2)),
contribuicoes_status     TINYINT NOT NULL DEFAULT 0 CHECK (contribuicoes_status IN (0,1,2)),
hard_skills_status       TINYINT NOT NULL DEFAULT 0 CHECK (hard_skills_status IN (0,1,2)),
soft_skills_status       TINYINT NOT NULL DEFAULT 0 CHECK (soft_skills_status IN (0,1,2)),
conteudo_md_status       TINYINT NOT NULL DEFAULT 0 CHECK (conteudo_md_status IN (0,1,2)),

created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at        DATETIME NULL,

FOREIGN KEY (trabalho_id) REFERENCES trabalhos_graduacao(id),
UNIQUE KEY uk_trab_versao_semestre_api (trabalho_id, versao, semestre_api),
KEY idx_secao_trab_versao (trabalho_id, versao)
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS tg_resumo (
id              BIGINT AUTO_INCREMENT PRIMARY KEY,
trabalho_id     BIGINT NOT NULL,
versao          VARCHAR(20) NOT NULL,

resumo_md       LONGTEXT NOT NULL,

created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

-- Status único por versão (0 pendente, 1 aprovado, 2 reprovado)
versao_validada TINYINT NOT NULL DEFAULT 0 CHECK (versao_validada IN (0,1,2)),

FOREIGN KEY (trabalho_id) REFERENCES trabalhos_graduacao(id),
UNIQUE KEY uk_resumo_trab_versao (trabalho_id, versao)
) ENGINE=InnoDB;

-- =========================================================
-- MENSAGENS / NOTIFICAÇÕES / ANEXOS / ENTREGAS
-- =========================================================
CREATE TABLE IF NOT EXISTS mensagens (
                                         id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         trabalho_id     BIGINT NOT NULL,
                                         remetente_id    BIGINT NOT NULL,
                                         destinatario_id BIGINT NOT NULL,
                                         secao           VARCHAR(100) NULL,
    tipo            ENUM('TEXTO','SISTEMA') NOT NULL DEFAULT 'TEXTO',
    conteudo        LONGTEXT NOT NULL,
    lida            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (trabalho_id)     REFERENCES trabalhos_graduacao(id),
    FOREIGN KEY (remetente_id)    REFERENCES usuarios(id),
    FOREIGN KEY (destinatario_id) REFERENCES usuarios(id),
    INDEX idx_msg_trab_secao (trabalho_id, secao)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS anexos (
                                      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      trabalho_id     BIGINT NOT NULL,
                                      versao_id       BIGINT NULL,
                                      mensagem_id     BIGINT NULL,
                                      arquivo_nome    VARCHAR(255) NOT NULL,
    arquivo_mime    VARCHAR(120) NULL,
    arquivo_tamanho BIGINT NULL,
    arquivo_path    VARCHAR(500) NOT NULL,
    uploaded_by     BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (trabalho_id) REFERENCES trabalhos_graduacao(id),
    FOREIGN KEY (versao_id)   REFERENCES versoes_trabalho(id),
    FOREIGN KEY (mensagem_id) REFERENCES mensagens(id),
    FOREIGN KEY (uploaded_by) REFERENCES usuarios(id)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notificacoes (
                                            id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            usuario_id BIGINT NOT NULL,
                                            titulo     VARCHAR(150) NOT NULL,
    conteudo   TEXT NULL,
    tipo       ENUM('INFO','WARN','ACTION') NOT NULL DEFAULT 'INFO',
    lida       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    INDEX idx_notif_usuario (usuario_id, lida)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS entregas (
                                        id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        trabalho_id BIGINT NOT NULL,
                                        nome        VARCHAR(150) NOT NULL,
    descricao   TEXT NULL,
    prazo       DATETIME NULL,
    status      ENUM('PENDENTE','EM_REVISAO','CONCLUIDA') NOT NULL DEFAULT 'PENDENTE',
    nota        DECIMAL(5,2) NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NULL,
    FOREIGN KEY (trabalho_id) REFERENCES trabalhos_graduacao(id)
    ) ENGINE=InnoDB;

-- =========================================================
-- SEEDS (senha padrão 123456) — idempotentes
-- =========================================================

-- Usuários
INSERT INTO usuarios (nome, email, senha_hash, tipo)
SELECT 'Beatriz Santos Figueiredo', 'aluno@exemplo.com', SHA2('123456',256), 'ALUNO'
    WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email='aluno@exemplo.com');

INSERT INTO usuarios (nome, email, senha_hash, tipo)
SELECT 'Professor Emanuel Mineda', 'orientador@exemplo.com', SHA2('123456',256), 'ORIENTADOR'
    WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email='orientador@exemplo.com');

INSERT INTO usuarios (nome, email, senha_hash, tipo)
SELECT 'Coordenador', 'coordenador@exemplo.com', SHA2('123456',256), 'COORDENADOR'
    WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email='coordenador@exemplo.com');

-- Orientação (ativa)
SET @aluno_id := (SELECT id FROM usuarios WHERE email='aluno@exemplo.com');
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

-- =========================
-- TG v1 (suas novas seeds)
-- =========================

-- Apresentação v1 (bloco inteiro no nome_completo; demais campos conforme você enviou)
INSERT INTO tg_apresentacao (
    trabalho_id, versao, nome_completo, idade, curso,
    historico_academico, motivacao_fatec, historico_profissional,
    contatos_email, principais_conhecimentos, consideracoes_finais
)
SELECT
    @tg_id, 'v1',
    '- **Nome completo:** Aluno exemplo \n
- **Idade:** 28 anos \n
- **Cidade:** São José dos Campos – SP \n
- **Curso:** Tecnólogo em Banco de Dados (FATEC São José dos Campos – Prof. Jessen Vidal)',
    NULL,
    NULL,
    '- **Graduação atual:** Banco de Dados – FATEC SJC \n
- **Pós-graduação:** Administração de Banco de Dados – Anhanguera Educacional \n
- **Curso anterior:** Análise e Desenvolvimento de Sistemas – ETEP Faculdades \n
- **Ensino médio técnico:** Técnico em Informática – ETEC Jacareí \n',
    'FOI ALTERADO \n',
    'Atuo na área de tecnologia desde 2017. \n
- **2017–2020:** Técnico de Suporte N2 na SPS Group — atendimento a usuários e manutenção de sistemas ERP. \n
- **2020–2023:** Analista de Banco de Dados Jr. na LogSmart Brasil — otimização de queries e acompanhamento de performance em PostgreSQL e SAP HANA. \n
- **2023–Presente:** Consultor SAP B1 Trainee — desenvolvimento de procedures e integração de dados com Service Layer. \n',
    '- **E-mail:** [aluna.exemplo.dev@gmail.com](mailto:aluna.exemplo.dev@gmail.com) \n
- **GitHub:** [github.com/alunaexemplo](https://github.com/alunaexemplo) \n
- **LinkedIn:** [linkedin.com/in/alunaexemplo](https://linkedin.com/in/alunaexemplo) \n',
    '- Banco de Dados: PostgreSQL, SAP HANA, MySQL \n
- Linguagens: SQL, Java, Python, JavaScript \n
- Frameworks: Spring Boot, Hibernate, Node.js \n
- Ferramentas: DBeaver, pgAdmin, IntelliJ IDEA, Postman, Git \n
- Metodologias: Scrum, Kanban \n
- Versionamento: Git/GitHub \n
- Outras habilidades: modelagem DER, análise de performance, API REST, integração de sistemas \n',
    'Durante a trajetória na FATEC, pude consolidar conhecimentos técnicos em modelagem de dados, desenvolvimento de APIs e versionamento de código, além de aprimorar habilidades interpessoais em comunicação, empatia e trabalho em equipe. \n \n
O modelo de aprendizado baseado em **APIs semestrais** foi essencial para vivenciar o ciclo completo de desenvolvimento — da análise de requisitos à entrega final. \n \n \n

Acredito que o aprendizado obtido, aliado à prática profissional, me prepara para atuar de forma sólida como Analista e Desenvolvedor de Banco de Dados, contribuindo para soluções de valor e impacto real no ambiente corporativo. \n'
    WHERE @tg_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM tg_apresentacao ap WHERE ap.trabalho_id = @tg_id AND ap.versao='v1'
);

-- Resumo v1
INSERT INTO tg_resumo (trabalho_id, versao, resumo_md)
SELECT
    @tg_id, 'v1',
    '## Tabela Resumo dos Projetos API\n
\n
| Semestre | Empresa Parceira | Solução Desenvolvida |\n
|---|---|---|\n
| 1º Semestre | FATEC | Calculadora matematica em portugol |\n
| 2º Semestre | FATEC | controle de tg |\n
| 3º Semestre | Futuro | Futuro |\n
| 4º Semestre | Futuro | Futuro |\n
| 5º Semestre | Futuro | Futuro |\n
| 6º Semestre | Futuro | Futuro |\n
\n
\n'
    WHERE @tg_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_resumo r WHERE r.trabalho_id=@tg_id AND r.versao='v1');

-- Seções API 1..6 (v1)
INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg_id, 'v1', 1,
    'FATEC São José dos Campos – Prof. Fabiano Sabha Walczak',
    'Gestão de solicitações de manutenção dispersa...',
    'Sistema web de abertura e acompanhamento de chamados internos...',
    'https://github.com/LizardsDBA/API-2025-1',
    'Java; Spring Boot; PostgreSQL; JavaFX; HTML/CSS; GitHub',
    'Modelagem BD; entidades; repositórios; autenticação JWT; CI/CD',
    'Java/Spring Boot (ensino); PostgreSQL (ensino); Git/GitHub (autonomia)',
    'Comunicação, proatividade e resiliência',
    NULL
    WHERE @tg_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg_id AND s.versao='v1' AND s.semestre_api=1);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg_id, 'v1', 2,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint.',
    NULL
    WHERE @tg_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg_id AND s.versao='v1' AND s.semestre_api=2);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg_id, 'v1', 3,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg_id AND s.versao='v1' AND s.semestre_api=3);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg_id, 'v1', 4,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg_id AND s.versao='v1' AND s.semestre_api=4);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg_id, 'v1', 5,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg_id AND s.versao='v1' AND s.semestre_api=5);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg_id, 'v1', 6,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg_id AND s.versao='v1' AND s.semestre_api=6);

-- Versão consolidada v1 (COMPLETO)
INSERT INTO versoes_trabalho (trabalho_id, secao, versao, conteudo_md, comentario)
SELECT
    @tg_id, 'COMPLETO', 'v1',
    '# APRESENTAÇÃO DO ALUNO \n
\n
## Informações Pessoais \n
- **Nome completo:** Aluna exemplo \n
- **Idade:** 28 anos \n
- **Cidade:** São José dos Campos – SP \n
- **Curso:** Tecnólogo em Banco de Dados (FATEC São José dos Campos – Prof. Jessen Vidal) \n
\n
## Histórico Acadêmico \n
- **Graduação atual:** Banco de Dados – FATEC SJC \n
- **Pós-graduação:** Administração de Banco de Dados – Anhanguera Educacional \n
- **Curso anterior:** Análise e Desenvolvimento de Sistemas – ETEP Faculdades \n
- **Ensino médio técnico:** Técnico em Informática – ETEC Jacareí \n
\n
## Motivação para Ingressar na FATEC \n
FOI ALTERADO \n
\n
## Histórico Profissional \n
Atuo na área de tecnologia desde 2017. \n
- **2017–2020:** Técnico de Suporte N2 na SPS Group — atendimento a usuários e manutenção de sistemas ERP. \n
- **2020–2023:** Analista de Banco de Dados Jr. na LogSmart Brasil — otimização de queries e acompanhamento de performance em PostgreSQL e SAP HANA. \n
- **2023–Presente:** Consultor SAP B1 Trainee — desenvolvimento de procedures e integração de dados com Service Layer. \n
\n
## Contatos \n
- **E-mail:** [aluna.exemplo.dev@gmail.com](mailto:aluna.exemplo.dev@gmail.com) \n
- **GitHub:** [github.com/alunaexemplo](https://github.com/alunaexemplo) \n
- **LinkedIn:** [linkedin.com/in/alunaexemplo](https://linkedin.com/in/alunaexemplo) \n
\n
## Principais Conhecimentos \n
- Banco de Dados: PostgreSQL, SAP HANA, MySQL \n
- Linguagens: SQL, Java, Python, JavaScript \n
- Frameworks: Spring Boot, Hibernate, Node.js \n
- Ferramentas: DBeaver, pgAdmin, IntelliJ IDEA, Postman, Git \n
- Metodologias: Scrum, Kanban \n
- Versionamento: Git/GitHub \n
- Outras habilidades: modelagem DER, análise de performance, API REST, integração de sistemas \n
\n
## API 1º Semestre \n
\n
**Empresa Parceira:** FATEC São José dos Campos – Prof. Fabiano Sabha Walczak \n
\n
### Problema \n
Gestão de solicitações de manutenção dispersa... \n
\n
### Solução \n
Sistema web de abertura e acompanhamento de chamados internos... \n
\n
**Repositório:** https://github.com/LizardsDBA/API-2025-1 \n
\n
### Tecnologias \n
Java; Spring Boot; PostgreSQL; JavaFX; HTML/CSS; GitHub \n
\n
### Contribuições Pessoais \n
Modelagem BD; entidades; repositórios; autenticação JWT; CI/CD \n
\n
### Hard Skills \n
Java/Spring Boot (ensino); PostgreSQL (ensino); Git/GitHub (autonomia) \n
\n
### Soft Skills \n
Comunicação, proatividade e resiliência \n
\n
## API 2º Semestre \n
\n
**Empresa Parceira:** FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak* \n
\n
### Problema \n
A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações. \n
\n
### Solução \n
Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade. \n
\n
**Repositório:** [https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1) \n
\n
### Tecnologias \n
| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n
\n
### Contribuições Pessoais \n
Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n
\n
### Hard Skills \n
| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n
\n
### Soft Skills \n
Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n
\n
## API 3º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## API 4º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## API 5º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## API 6º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## Tabela Resumo dos Projetos API \n
\n
| Semestre | Empresa Parceira | Solução Desenvolvida | \n
|---|---|---| \n
| 1º Semestre (2023-2) | FATEC São José dos Campos | calculadora | \n
| 2º Semestre (2024-1) | FATEC São José dos Campos | api | \n
| 3º Semestre (2024-2) | futuro | futuro | \n
| 4º Semestre (2025-1) | futuro | futuro | \n
| 5º Semestre (2025-2) | futuro | futuro | \n
| 6º Semestre (2026-1) | futuro | futuro | \n
\n
## Considerações Finais \n
\n
Durante a trajetória na FATEC, pude consolidar conhecimentos técnicos em modelagem de dados, desenvolvimento de APIs e versionamento de código, além de aprimorar habilidades interpessoais em comunicação, empatia e trabalho em equipe. \n
O modelo de aprendizado baseado em **APIs semestrais** foi essencial para vivenciar o ciclo completo de desenvolvimento — da análise de requisitos à entrega final. \n
\n
Acredito que o aprendizado obtido, aliado à prática profissional, me prepara para atuar de forma sólida como Analista e Desenvolvedor de Banco de Dados, contribuindo para soluções de valor e impacto real no ambiente corporativo. \n',
    NULL
    WHERE @tg_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM versoes_trabalho v WHERE v.trabalho_id=@tg_id AND v.secao='COMPLETO' AND v.versao='v1'
);


-- =========================================================
-- =========================================================
-- INÍCIO DOS NOVOS SEEDS (ALUNO 2, 3, 4 + ORIENTADOR 2)
-- =========================================================
-- =========================================================

-- Novos Usuários
INSERT INTO usuarios (nome, email, senha_hash, tipo)
SELECT 'Mariana Lopes Andrade', 'aluno2@exemplo.com', SHA2('123456',256), 'ALUNO'
    WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email='aluno2@exemplo.com');

INSERT INTO usuarios (nome, email, senha_hash, tipo)
SELECT 'Rafael Sousa Menezes', 'aluno3@exemplo.com', SHA2('123456',256), 'ALUNO'
    WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email='aluno3@exemplo.com');

INSERT INTO usuarios (nome, email, senha_hash, tipo)
SELECT 'Camila Ferreira Duarte', 'aluno4@exemplo.com', SHA2('123456',256), 'ALUNO'
    WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email='aluno4@exemplo.com');

INSERT INTO usuarios (nome, email, senha_hash, tipo)
SELECT 'Professora Adriana Jacinto', 'orientador2@exemplo.com', SHA2('123456',256), 'ORIENTADOR'
    WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email='orientador2@exemplo.com');

-- =========================
-- SEEDS Mariana Lopes Andrade (orientador@exemplo.com)
-- =========================
SET @aluno2_id := (SELECT id FROM usuarios WHERE email='aluno2@exemplo.com');
SET @orientador1_id := (SELECT id FROM usuarios WHERE email='orientador@exemplo.com'); -- Usando o orientador existente

-- Orientação
INSERT INTO orientacoes (aluno_id, orientador_id, ativo)
SELECT @aluno2_id, @orientador1_id, TRUE
    WHERE @aluno2_id IS NOT NULL AND @orientador1_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM orientacoes o
    WHERE o.aluno_id = @aluno2_id AND o.orientador_id = @orientador1_id AND o.ativo = TRUE
);

-- TG
INSERT INTO trabalhos_graduacao (aluno_id, orientador_id, titulo, tema, versao_atual, percentual_conclusao)
SELECT @aluno2_id, @orientador1_id, 'Portfólio TG - Mariana Lopes Andrade', 'Histórico de APIs - Mariana Lopes Andrade', 'v1', 0.00
    WHERE @aluno2_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM trabalhos_graduacao t WHERE t.aluno_id = @aluno2_id);

SET @tg2_id := (SELECT id FROM trabalhos_graduacao WHERE aluno_id=@aluno2_id);

-- =========================
-- SEEDS Rafael Sousa Menezes (orientador2@exemplo.com)
-- =========================
SET @aluno3_id := (SELECT id FROM usuarios WHERE email='aluno3@exemplo.com');
SET @orientador2_id := (SELECT id FROM usuarios WHERE email='orientador2@exemplo.com'); -- Usando o novo orientador

-- Orientação
INSERT INTO orientacoes (aluno_id, orientador_id, ativo)
SELECT @aluno3_id, @orientador2_id, TRUE
    WHERE @aluno3_id IS NOT NULL AND @orientador2_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM orientacoes o
    WHERE o.aluno_id = @aluno3_id AND o.orientador_id = @orientador2_id AND o.ativo = TRUE
);

-- TG
INSERT INTO trabalhos_graduacao (aluno_id, orientador_id, titulo, tema, versao_atual, percentual_conclusao)
SELECT @aluno3_id, @orientador2_id, 'Portfólio TG - Rafael Sousa Menezes', 'Histórico de APIs - Rafael Sousa Menezes', 'v1', 0.00
    WHERE @aluno3_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM trabalhos_graduacao t WHERE t.aluno_id = @aluno3_id);

SET @tg3_id := (SELECT id FROM trabalhos_graduacao WHERE aluno_id=@aluno3_id);

-- =========================
-- SEEDS Camila Ferreira Duarte (orientador2@exemplo.com)
-- =========================
SET @aluno4_id := (SELECT id FROM usuarios WHERE email='aluno4@exemplo.com');
-- @orientador2_id já foi definido acima

-- Orientação
INSERT INTO orientacoes (aluno_id, orientador_id, ativo)
SELECT @aluno4_id, @orientador2_id, TRUE
    WHERE @aluno4_id IS NOT NULL AND @orientador2_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM orientacoes o
    WHERE o.aluno_id = @aluno4_id AND o.orientador_id = @orientador2_id AND o.ativo = TRUE
);

-- TG
INSERT INTO trabalhos_graduacao (aluno_id, orientador_id, titulo, tema, versao_atual, percentual_conclusao)
SELECT @aluno4_id, @orientador2_id, 'Portfólio TG - Camila Ferreira Duarte', 'Histórico de APIs - Camila Ferreira Duarte', 'v1', 0.00
    WHERE @aluno4_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM trabalhos_graduacao t WHERE t.aluno_id = @aluno4_id);

SET @tg4_id := (SELECT id FROM trabalhos_graduacao WHERE aluno_id=@aluno4_id);


-- =========================================================
-- =========================================================
-- SEEDS TG v1 (Mariana Lopes Andrade)
-- =========================================================
-- =========================================================

-- Apresentação v1 (Mariana Lopes Andrade)
INSERT INTO tg_apresentacao (
    trabalho_id, versao, nome_completo, idade, curso,
    historico_academico, motivacao_fatec, historico_profissional,
    contatos_email, principais_conhecimentos, consideracoes_finais
)
SELECT
    @tg2_id, 'v1',
    '- **Nome completo:** Mariana Lopes Andrade \n
- **Idade:** 28 anos \n
- **Cidade:** São José dos Campos – SP \n
- **Curso:** Tecnólogo em Banco de Dados (FATEC São José dos Campos – Prof. Jessen Vidal)',
    NULL,
    NULL,
    '- **Graduação atual:** Banco de Dados – FATEC SJC \n
- **Pós-graduação:** Administração de Banco de Dados – Anhanguera Educacional \n
- **Curso anterior:** Análise e Desenvolvimento de Sistemas – ETEP Faculdades \n
- **Ensino médio técnico:** Técnico em Informática – ETEC Jacareí \n',
    'FOI ALTERADO \n',
    'Atuo na área de tecnologia desde 2017. \n
- **2017–2020:** Técnico de Suporte N2 na SPS Group — atendimento a usuários e manutenção de sistemas ERP. \n
- **2020–2023:** Analista de Banco de Dados Jr. na LogSmart Brasil — otimização de queries e acompanhamento de performance em PostgreSQL e SAP HANA. \n
- **2023–Presente:** Consultor SAP B1 Trainee — desenvolvimento de procedures e integração de dados com Service Layer. \n',
    '- **E-mail:** [aluno2.exemplo.dev@gmail.com](mailto:aluno2.exemplo.dev@gmail.com) \n
- **GitHub:** [github.com/aluno2exemplo](https://github.com/aluno2exemplo) \n
- **LinkedIn:** [linkedin.com/in/aluno2exemplo](https://linkedin.com/in/aluno2exemplo) \n',
    '- Banco de Dados: PostgreSQL, SAP HANA, MySQL \n
- Linguagens: SQL, Java, Python, JavaScript \n
- Frameworks: Spring Boot, Hibernate, Node.js \n
- Ferramentas: DBeaver, pgAdmin, IntelliJ IDEA, Postman, Git \n
- Metodologias: Scrum, Kanban \n
- Versionamento: Git/GitHub \n
- Outras habilidades: modelagem DER, análise de performance, API REST, integração de sistemas \n',
    'Durante a trajetória na FATEC, pude consolidar conhecimentos técnicos em modelagem de dados, desenvolvimento de APIs e versionamento de código, além de aprimorar habilidades interpessoais em comunicação, empatia e trabalho em equipe. \n \n
O modelo de aprendizado baseado em **APIs semestrais** foi essencial para vivenciar o ciclo completo de desenvolvimento — da análise de requisitos à entrega final. \n \n \n

Acredito que o aprendizado obtido, aliado à prática profissional, me prepara para atuar de forma sólida como Analista e Desenvolvedor de Banco de Dados, contribuindo para soluções de valor e impacto real no ambiente corporativo. \n'
    WHERE @tg2_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM tg_apresentacao ap WHERE ap.trabalho_id = @tg2_id AND ap.versao='v1'
);

-- Resumo v1 (Mariana Lopes Andrade)
INSERT INTO tg_resumo (trabalho_id, versao, resumo_md)
SELECT
    @tg2_id, 'v1',
    '## Tabela Resumo dos Projetos API\n
\n
| Semestre | Empresa Parceira | Solução Desenvolvida |\n
|---|---|---|\n
| 1º Semestre | FATEC | Calculadora matematica em portugol |\n
| 2º Semestre | FATEC | controle de tg |\n
| 3º Semestre | Futuro | Futuro |\n
| 4º Semestre | Futuro | Futuro |\n
| 5º Semestre | Futuro | Futuro |\n
| 6º Semestre | Futuro | Futuro |\n
\n
\n'
    WHERE @tg2_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_resumo r WHERE r.trabalho_id=@tg2_id AND r.versao='v1');

-- Seções API 1..6 (v1) (Mariana Lopes Andrade)
INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg2_id, 'v1', 1,
    'FATEC São José dos Campos – Prof. Fabiano Sabha Walczak',
    'Gestão de solicitações de manutenção dispersa...',
    'Sistema web de abertura e acompanhamento de chamados internos...',
    'https://github.com/LizardsDBA/API-2025-1',
    'Java; Spring Boot; PostgreSQL; JavaFX; HTML/CSS; GitHub',
    'Modelagem BD; entidades; repositórios; autenticação JWT; CI/CD',
    'Java/Spring Boot (ensino); PostgreSQL (ensino); Git/GitHub (autonomia)',
    'Comunicação, proatividade e resiliência',
    NULL
    WHERE @tg2_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg2_id AND s.versao='v1' AND s.semestre_api=1);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg2_id, 'v1', 2,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint.',
    NULL
    WHERE @tg2_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg2_id AND s.versao='v1' AND s.semestre_api=2);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg2_id, 'v1', 3,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg2_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg2_id AND s.versao='v1' AND s.semestre_api=3);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg2_id, 'v1', 4,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg2_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg2_id AND s.versao='v1' AND s.semestre_api=4);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg2_id, 'v1', 5,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg2_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg2_id AND s.versao='v1' AND s.semestre_api=5);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg2_id, 'v1', 6,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg2_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg2_id AND s.versao='v1' AND s.semestre_api=6);

-- Versão consolidada v1 (COMPLETO) (Mariana Lopes Andrade)
INSERT INTO versoes_trabalho (trabalho_id, secao, versao, conteudo_md, comentario)
SELECT
    @tg2_id, 'COMPLETO', 'v1',
    '# APRESENTAÇÃO DO ALUNO \n
\n
## Informações Pessoais \n
- **Nome completo:** Mariana Lopes Andrade \n
- **Idade:** 28 anos \n
- **Cidade:** São José dos Campos – SP \n
- **Curso:** Tecnólogo em Banco de Dados (FATEC São José dos Campos – Prof. Jessen Vidal) \n
\n
## Histórico Acadêmico \n
- **Graduação atual:** Banco de Dados – FATEC SJC \n
- **Pós-graduação:** Administração de Banco de Dados – Anhanguera Educacional \n
- **Curso anterior:** Análise e Desenvolvimento de Sistemas – ETEP Faculdades \n
- **Ensino médio técnico:** Técnico em Informática – ETEC Jacareí \n
\n
## Motivação para Ingressar na FATEC \n
FOI ALTERADO \n
\n
## Histórico Profissional \n
Atuo na área de tecnologia desde 2017. \n
- **2017–2020:** Técnico de Suporte N2 na SPS Group — atendimento a usuários e manutenção de sistemas ERP. \n
- **2020–2023:** Analista de Banco de Dados Jr. na LogSmart Brasil — otimização de queries e acompanhamento de performance em PostgreSQL e SAP HANA. \n
- **2023–Presente:** Consultor SAP B1 Trainee — desenvolvimento de procedures e integração de dados com Service Layer. \n
\n
## Contatos \n
- **E-mail:** [aluno2.exemplo.dev@gmail.com](mailto:aluno2.exemplo.dev@gmail.com) \n
- **GitHub:** [github.com/aluno2exemplo](https://github.com/aluno2exemplo) \n
- **LinkedIn:** [linkedin.com/in/aluno2exemplo](https://linkedin.com/in/aluno2exemplo) \n
\n
## Principais Conhecimentos \n
- Banco de Dados: PostgreSQL, SAP HANA, MySQL \n
- Linguagens: SQL, Java, Python, JavaScript \n
- Frameworks: Spring Boot, Hibernate, Node.js \n
- Ferramentas: DBeaver, pgAdmin, IntelliJ IDEA, Postman, Git \n
- Metodologias: Scrum, Kanban \n
- Versionamento: Git/GitHub \n
- Outras habilidades: modelagem DER, análise de performance, API REST, integração de sistemas \n
\n
## API 1º Semestre \n
\n
**Empresa Parceira:** FATEC São José dos Campos – Prof. Fabiano Sabha Walczak \n
\n
### Problema \n
Gestão de solicitações de manutenção dispersa... \n
\n
### Solução \n
Sistema web de abertura e acompanhamento de chamados internos... \n
\n
**Repositório:** https://github.com/LizardsDBA/API-2025-1 \n
\n
### Tecnologias \n
Java; Spring Boot; PostgreSQL; JavaFX; HTML/CSS; GitHub \n
\n
### Contribuições Pessoais \n
Modelagem BD; entidades; repositórios; autenticação JWT; CI/CD \n
\n
### Hard Skills \n
Java/Spring Boot (ensino); PostgreSQL (ensino); Git/GitHub (autonomia) \n
\n
### Soft Skills \n
Comunicação, proatividade e resiliência \n
\n
## API 2º Semestre \n
\n
**Empresa Parceira:** FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak* \n
\n
### Problema \n
A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações. \n
\n
### Solução \n
Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade. \n
\n
**Repositório:** [https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1) \n
\n
### Tecnologias \n
| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n
\n
### Contribuições Pessoais \n
Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n
\n
### Hard Skills \n
| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n
\n
### Soft Skills \n
Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n
\n
## API 3º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## API 4º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## API 5º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## API 6º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## Tabela Resumo dos Projetos API \n
\n
| Semestre | Empresa Parceira | Solução Desenvolvida | \n
|---|---|---| \n
| 1º Semestre (2023-2) | FATEC São José dos Campos | calculadora | \n
| 2º Semestre (2024-1) | FATEC São José dos Campos | api | \n
| 3º Semestre (2024-2) | futuro | futuro | \n
| 4º Semestre (2025-1) | futuro | futuro | \n
| 5º Semestre (2025-2) | futuro | futuro | \n
| 6º Semestre (2026-1) | futuro | futuro | \n
\n
## Considerações Finais \n
\n
Durante a trajetória na FATEC, pude consolidar conhecimentos técnicos em modelagem de dados, desenvolvimento de APIs e versionamento de código, além de aprimorar habilidades interpessoais em comunicação, empatia e trabalho em equipe. \n
O modelo de aprendizado baseado em **APIs semestrais** foi essencial para vivenciar o ciclo completo de desenvolvimento — da análise de requisitos à entrega final. \n
\n
Acredito que o aprendizado obtido, aliado à prática profissional, me prepara para atuar de forma sólida como Analista e Desenvolvedor de Banco de Dados, contribuindo para soluções de valor e impacto real no ambiente corporativo. \n',
    NULL
    WHERE @tg2_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM versoes_trabalho v WHERE v.trabalho_id=@tg2_id AND v.secao='COMPLETO' AND v.versao='v1'
);


-- =========================================================
-- =========================================================
-- SEEDS TG v1 (Rafael Sousa Menezes)
-- =========================================================
-- =========================================================

-- Apresentação v1 (Rafael Sousa Menezes)
INSERT INTO tg_apresentacao (
    trabalho_id, versao, nome_completo, idade, curso,
    historico_academico, motivacao_fatec, historico_profissional,
    contatos_email, principais_conhecimentos, consideracoes_finais
)
SELECT
    @tg3_id, 'v1',
    '- **Nome completo:** Rafael Sousa Menezes \n
- **Idade:** 28 anos \n
- **Cidade:** São José dos Campos – SP \n
- **Curso:** Tecnólogo em Banco de Dados (FATEC São José dos Campos – Prof. Jessen Vidal)',
    NULL,
    NULL,
    '- **Graduação atual:** Banco de Dados – FATEC SJC \n
- **Pós-graduação:** Administração de Banco de Dados – Anhanguera Educacional \n
- **Curso anterior:** Análise e Desenvolvimento de Sistemas – ETEP Faculdades \n
- **Ensino médio técnico:** Técnico em Informática – ETEC Jacareí \n',
    'FOI ALTERADO \n',
    'Atuo na área de tecnologia desde 2017. \n
- **2017–2020:** Técnico de Suporte N2 na SPS Group — atendimento a usuários e manutenção de sistemas ERP. \n
- **2020–2023:** Analista de Banco de Dados Jr. na LogSmart Brasil — otimização de queries e acompanhamento de performance em PostgreSQL e SAP HANA. \n
- **2023–Presente:** Consultor SAP B1 Trainee — desenvolvimento de procedures e integração de dados com Service Layer. \n',
    '- **E-mail:** [aluno3.exemplo.dev@gmail.com](mailto:aluno3.exemplo.dev@gmail.com) \n
- **GitHub:** [github.com/aluno3exemplo](https://github.com/aluno3exemplo) \n
- **LinkedIn:** [linkedin.com/in/aluno3exemplo](https://linkedin.com/in/aluno3exemplo) \n',
    '- Banco de Dados: PostgreSQL, SAP HANA, MySQL \n
- Linguagens: SQL, Java, Python, JavaScript \n
- Frameworks: Spring Boot, Hibernate, Node.js \n
- Ferramentas: DBeaver, pgAdmin, IntelliJ IDEA, Postman, Git \n
- Metodologias: Scrum, Kanban \n
- Versionamento: Git/GitHub \n
- Outras habilidades: modelagem DER, análise de performance, API REST, integração de sistemas \n',
    'Durante a trajetória na FATEC, pude consolidar conhecimentos técnicos em modelagem de dados, desenvolvimento de APIs e versionamento de código, além de aprimorar habilidades interpessoais em comunicação, empatia e trabalho em equipe. \n \n
O modelo de aprendizado baseado em **APIs semestrais** foi essencial para vivenciar o ciclo completo de desenvolvimento — da análise de requisitos à entrega final. \n \n \n

Acredito que o aprendizado obtido, aliado à prática profissional, me prepara para atuar de forma sólida como Analista e Desenvolvedor de Banco de Dados, contribuindo para soluções de valor e impacto real no ambiente corporativo. \n'
    WHERE @tg3_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM tg_apresentacao ap WHERE ap.trabalho_id = @tg3_id AND ap.versao='v1'
);

-- Resumo v1 (Rafael Sousa Menezes)
INSERT INTO tg_resumo (trabalho_id, versao, resumo_md)
SELECT
    @tg3_id, 'v1',
    '## Tabela Resumo dos Projetos API\n
\n
| Semestre | Empresa Parceira | Solução Desenvolvida |\n
|---|---|---|\n
| 1º Semestre | FATEC | Calculadora matematica em portugol |\n
| 2º Semestre | FATEC | controle de tg |\n
| 3º Semestre | Futuro | Futuro |\n
| 4º Semestre | Futuro | Futuro |\n
| 5º Semestre | Futuro | Futuro |\n
| 6º Semestre | Futuro | Futuro |\n
\n
\n'
    WHERE @tg3_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_resumo r WHERE r.trabalho_id=@tg3_id AND r.versao='v1');

-- Seções API 1..6 (v1) (Rafael Sousa Menezes)
INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg3_id, 'v1', 1,
    'FATEC São José dos Campos – Prof. Fabiano Sabha Walczak',
    'Gestão de solicitações de manutenção dispersa...',
    'Sistema web de abertura e acompanhamento de chamados internos...',
    'https://github.com/LizardsDBA/API-2025-1',
    'Java; Spring Boot; PostgreSQL; JavaFX; HTML/CSS; GitHub',
    'Modelagem BD; entidades; repositórios; autenticação JWT; CI/CD',
    'Java/Spring Boot (ensino); PostgreSQL (ensino); Git/GitHub (autonomia)',
    'Comunicação, proatividade e resiliência',
    NULL
    WHERE @tg3_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg3_id AND s.versao='v1' AND s.semestre_api=1);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg3_id, 'v1', 2,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint.',
    NULL
    WHERE @tg3_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg3_id AND s.versao='v1' AND s.semestre_api=2);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg3_id, 'v1', 3,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg3_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg3_id AND s.versao='v1' AND s.semestre_api=3);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg3_id, 'v1', 4,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg3_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg3_id AND s.versao='v1' AND s.semestre_api=4);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg3_id, 'v1', 5,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg3_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg3_id AND s.versao='v1' AND s.semestre_api=5);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg3_id, 'v1', 6,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg3_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg3_id AND s.versao='v1' AND s.semestre_api=6);

-- Versão consolidada v1 (COMPLETO) (Rafael Sousa Menezes)
INSERT INTO versoes_trabalho (trabalho_id, secao, versao, conteudo_md, comentario)
SELECT
    @tg3_id, 'COMPLETO', 'v1',
    '# APRESENTAÇÃO DO ALUNO \n
\n
## Informações Pessoais \n
- **Nome completo:** Rafael Sousa Menezes \n
- **Idade:** 28 anos \n
- **Cidade:** São José dos Campos – SP \n
- **Curso:** Tecnólogo em Banco de Dados (FATEC São José dos Campos – Prof. Jessen Vidal) \n
\n
## Histórico Acadêmico \n
- **Graduação atual:** Banco de Dados – FATEC SJC \n
- **Pós-graduação:** Administração de Banco de Dados – Anhanguera Educacional \n
- **Curso anterior:** Análise e Desenvolvimento de Sistemas – ETEP Faculdades \n
- **Ensino médio técnico:** Técnico em Informática – ETEC Jacareí \n
\n
## Motivação para Ingressar na FATEC \n
FOI ALTERADO \n
\n
## Histórico Profissional \n
Atuo na área de tecnologia desde 2017. \n
- **2017–2020:** Técnico de Suporte N2 na SPS Group — atendimento a usuários e manutenção de sistemas ERP. \n
- **2020–2023:** Analista de Banco de Dados Jr. na LogSmart Brasil — otimização de queries e acompanhamento de performance em PostgreSQL e SAP HANA. \n
- **2023–Presente:** Consultor SAP B1 Trainee — desenvolvimento de procedures e integração de dados com Service Layer. \n
\n
## Contatos \n
- **E-mail:** [aluno3.exemplo.dev@gmail.com](mailto:aluno3.exemplo.dev@gmail.com) \n
- **GitHub:** [github.com/aluno3exemplo](https://github.com/aluno3exemplo) \n
- **LinkedIn:** [linkedin.com/in/aluno3exemplo](https://linkedin.com/in/aluno3exemplo) \n
\n
## Principais Conhecimentos \n
- Banco de Dados: PostgreSQL, SAP HANA, MySQL \n
- Linguagens: SQL, Java, Python, JavaScript \n
- Frameworks: Spring Boot, Hibernate, Node.js \n
- Ferramentas: DBeaver, pgAdmin, IntelliJ IDEA, Postman, Git \n
- Metodologias: Scrum, Kanban \n
- Versionamento: Git/GitHub \n
- Outras habilidades: modelagem DER, análise de performance, API REST, integração de sistemas \n
\n
## API 1º Semestre \n
\n
**Empresa Parceira:** FATEC São José dos Campos – Prof. Fabiano Sabha Walczak \n
\n
### Problema \n
Gestão de solicitações de manutenção dispersa... \n
\n
### Solução \n
Sistema web de abertura e acompanhamento de chamados internos... \n
\n
**Repositório:** https://github.com/LizardsDBA/API-2025-1 \n
\n
### Tecnologias \n
Java; Spring Boot; PostgreSQL; JavaFX; HTML/CSS; GitHub \n
\n
### Contribuições Pessoais \n
Modelagem BD; entidades; repositórios; autenticação JWT; CI/CD \n
\n
### Hard Skills \n
Java/Spring Boot (ensino); PostgreSQL (ensino); Git/GitHub (autonomia) \n
\n
### Soft Skills \n
Comunicação, proatividade e resiliência \n
\n
## API 2º Semestre \n
\n
**Empresa Parceira:** FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak* \n
\n
### Problema \n
A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações. \n
\n
### Solução \n
Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade. \n
\n
**Repositório:** [https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1) \n
\n
### Tecnologias \n
| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n
\n
### Contribuições Pessoais \n
Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n
\n
### Hard Skills \n
| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n
\n
### Soft Skills \n
Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n
\n
## API 3º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## API 4º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## API 5º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## API 6º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## Tabela Resumo dos Projetos API \n
\n
| Semestre | Empresa Parceira | Solução Desenvolvida | \n
|---|---|---| \n
| 1º Semestre (2023-2) | FATEC São José dos Campos | calculadora | \n
| 2º Semestre (2024-1) | FATEC São José dos Campos | api | \n
| 3º Semestre (2024-2) | futuro | futuro | \n
| 4º Semestre (2025-1) | futuro | futuro | \n
| 5º Semestre (2025-2) | futuro | futuro | \n
| 6º Semestre (2026-1) | futuro | futuro | \n
\n
## Considerações Finais \n
\n
Durante a trajetória na FATEC, pude consolidar conhecimentos técnicos em modelagem de dados, desenvolvimento de APIs e versionamento de código, além de aprimorar habilidades interpessoais em comunicação, empatia e trabalho em equipe. \n
O modelo de aprendizado baseado em **APIs semestrais** foi essencial para vivenciar o ciclo completo de desenvolvimento — da análise de requisitos à entrega final. \n
\n
Acredito que o aprendizado obtido, aliado à prática profissional, me prepara para atuar de forma sólida como Analista e Desenvolvedor de Banco de Dados, contribuindo para soluções de valor e impacto real no ambiente corporativo. \n',
    NULL
    WHERE @tg3_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM versoes_trabalho v WHERE v.trabalho_id=@tg3_id AND v.secao='COMPLETO' AND v.versao='v1'
);


-- =========================================================
-- =========================================================
-- SEEDS TG v1 (Camila Ferreira Duarte)
-- =========================================================
-- =========================================================

-- Apresentação v1 (Camila Ferreira Duarte)
INSERT INTO tg_apresentacao (
    trabalho_id, versao, nome_completo, idade, curso,
    historico_academico, motivacao_fatec, historico_profissional,
    contatos_email, principais_conhecimentos, consideracoes_finais
)
SELECT
    @tg4_id, 'v1',
    '- **Nome completo:** Camila Ferreira Duarte \n
- **Idade:** 28 anos \n
- **Cidade:** São José dos Campos – SP \n
- **Curso:** Tecnólogo em Banco de Dados (FATEC São José dos Campos – Prof. Jessen Vidal)',
    NULL,
    NULL,
    '- **Graduação atual:** Banco de Dados – FATEC SJC \n
- **Pós-graduação:** Administração de Banco de Dados – Anhanguera Educacional \n
- **Curso anterior:** Análise e Desenvolvimento de Sistemas – ETEP Faculdades \n
- **Ensino médio técnico:** Técnico em Informática – ETEC Jacareí \n',
    'FOI ALTERADO \n',
    'Atuo na área de tecnologia desde 2017. \n
- **2017–2020:** Técnico de Suporte N2 na SPS Group — atendimento a usuários e manutenção de sistemas ERP. \n
- **2020–2023:** Analista de Banco de Dados Jr. na LogSmart Brasil — otimização de queries e acompanhamento de performance em PostgreSQL e SAP HANA. \n
- **2023–Presente:** Consultor SAP B1 Trainee — desenvolvimento de procedures e integração de dados com Service Layer. \n',
    '- **E-mail:** [aluno4.exemplo.dev@gmail.com](mailto:aluno4.exemplo.dev@gmail.com) \n
- **GitHub:** [github.com/aluno4exemplo](https://github.com/aluno4exemplo) \n
- **LinkedIn:** [linkedin.com/in/aluno4exemplo](https://linkedin.com/in/aluno4exemplo) \n',
    '- Banco de Dados: PostgreSQL, SAP HANA, MySQL \n
- Linguagens: SQL, Java, Python, JavaScript \n
- Frameworks: Spring Boot, Hibernate, Node.js \n
- Ferramentas: DBeaver, pgAdmin, IntelliJ IDEA, Postman, Git \n
- Metodologias: Scrum, Kanban \n
- Versionamento: Git/GitHub \n
- Outras habilidades: modelagem DER, análise de performance, API REST, integração de sistemas \n',
    'Durante a trajetória na FATEC, pude consolidar conhecimentos técnicos em modelagem de dados, desenvolvimento de APIs e versionamento de código, além de aprimorar habilidades interpessoais em comunicação, empatia e trabalho em equipe. \n \n
O modelo de aprendizado baseado em **APIs semestrais** foi essencial para vivenciar o ciclo completo de desenvolvimento — da análise de requisitos à entrega final. \n \n \n

Acredito que o aprendizado obtido, aliado à prática profissional, me prepara para atuar de forma sólida como Analista e Desenvolvedor de Banco de Dados, contribuindo para soluções de valor e impacto real no ambiente corporativo. \n'
    WHERE @tg4_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM tg_apresentacao ap WHERE ap.trabalho_id = @tg4_id AND ap.versao='v1'
);

-- Resumo v1 (Camila Ferreira Duarte)
INSERT INTO tg_resumo (trabalho_id, versao, resumo_md)
SELECT
    @tg4_id, 'v1',
    '## Tabela Resumo dos Projetos API\n
\n
| Semestre | Empresa Parceira | Solução Desenvolvida |\n
|---|---|---|\n
| 1º Semestre | FATEC | Calculadora matematica em portugol |\n
| 2º Semestre | FATEC | controle de tg |\n
| 3º Semestre | Futuro | Futuro |\n
| 4º Semestre | Futuro | Futuro |\n
| 5º Semestre | Futuro | Futuro |\n
| 6º Semestre | Futuro | Futuro |\n
\n
\n'
    WHERE @tg4_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_resumo r WHERE r.trabalho_id=@tg4_id AND r.versao='v1');

-- Seções API 1..6 (v1) (Camila Ferreira Duarte)
INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg4_id, 'v1', 1,
    'FATEC São José dos Campos – Prof. Fabiano Sabha Walczak',
    'Gestão de solicitações de manutenção dispersa...',
    'Sistema web de abertura e acompanhamento de chamados internos...',
    'https://github.com/LizardsDBA/API-2025-1',
    'Java; Spring Boot; PostgreSQL; JavaFX; HTML/CSS; GitHub',
    'Modelagem BD; entidades; repositórios; autenticação JWT; CI/CD',
    'Java/Spring Boot (ensino); PostgreSQL (ensino); Git/GitHub (autonomia)',
    'Comunicação, proatividade e resiliência',
    NULL
    WHERE @tg4_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg4_id AND s.versao='v1' AND s.semestre_api=1);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg4_id, 'v1', 2,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint.',
    NULL
    WHERE @tg4_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg4_id AND s.versao='v1' AND s.semestre_api=2);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg4_id, 'v1', 3,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg4_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg4_id AND s.versao='v1' AND s.semestre_api=3);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg4_id, 'v1', 4,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principalis, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg4_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg4_id AND s.versao='v1' AND s.semestre_api=4);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg4_id, 'v1', 5,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg4_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg4_id AND s.versao='v1' AND s.semestre_api=5);

INSERT INTO tg_secao (
    trabalho_id, versao, semestre_api, empresa_parceira, problema,
    solucao_resumo, link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md
)
SELECT
    @tg4_id, 'v1', 6,
    'FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak*',
    'A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações.',
    'Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade.',
    '[https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1)',
    '| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n',
    'Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n',
    '| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n',
    'Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n',
    NULL
    WHERE @tg4_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tg_secao s WHERE s.trabalho_id=@tg4_id AND s.versao='v1' AND s.semestre_api=6);

-- Versão consolidada v1 (COMPLETO) (Camila Ferreira Duarte)
INSERT INTO versoes_trabalho (trabalho_id, secao, versao, conteudo_md, comentario)
SELECT
    @tg4_id, 'COMPLETO', 'v1',
    '# APRESENTAÇÃO DO ALUNO \n
\n
## Informações Pessoais \n
- **Nome completo:** Camila Ferreira Duarte \n
- **Idade:** 28 anos \n
- **Cidade:** São José dos Campos – SP \n
- **Curso:** Tecnólogo em Banco de Dados (FATEC São José dos Campos – Prof. Jessen Vidal) \n
\n
## Histórico Acadêmico \n
- **Graduação atual:** Banco de Dados – FATEC SJC \n
- **Pós-graduação:** Administração de Banco de Dados – Anhanguera Educacional \n
- **Curso anterior:** Análise e Desenvolvimento de Sistemas – ETEP Faculdades \n
- **Ensino médio técnico:** Técnico em Informática – ETEC Jacareí \n
\n
## Motivação para Ingressar na FATEC \n
FOI ALTERADO \n
\n
## Histórico Profissional \n
Atuo na área de tecnologia desde 2017. \n
- **2017–2020:** Técnico de Suporte N2 na SPS Group — atendimento a usuários e manutenção de sistemas ERP. \n
- **2020–2023:** Analista de Banco de Dados Jr. na LogSmart Brasil — otimização de queries e acompanhamento de performance em PostgreSQL e SAP HANA. \n
- **2023–Presente:** Consultor SAP B1 Trainee — desenvolvimento de procedures e integração de dados com Service Layer. \n
\n
## Contatos \n
- **E-mail:** [aluno4.exemplo.dev@gmail.com](mailto:aluno4.exemplo.dev@gmail.com) \n
- **GitHub:** [github.com/aluno4exemplo](https://github.com/aluno4exemplo) \n
- **LinkedIn:** [linkedin.com/in/aluno4exemplo](https://linkedin.com/in/aluno4exemplo) \n
\n
## Principais Conhecimentos \n
- Banco de Dados: PostgreSQL, SAP HANA, MySQL \n
- Linguagens: SQL, Java, Python, JavaScript \n
- Frameworks: Spring Boot, Hibernate, Node.js \n
- Ferramentas: DBeaver, pgAdmin, IntelliJ IDEA, Postman, Git \n
- Metodologias: Scrum, Kanban \n
- Versionamento: Git/GitHub \n
- Outras habilidades: modelagem DER, análise de performance, API REST, integração de sistemas \n
\n
## API 1º Semestre \n
\n
**Empresa Parceira:** FATEC São José dos Campos – Prof. Fabiano Sabha Walczak \n
\n
### Problema \n
Gestão de solicitações de manutenção dispersa... \n
\n
### Solução \n
Sistema web de abertura e acompanhamento de chamados internos... \n
\n
**Repositório:** https://github.com/LizardsDBA/API-2025-1 \n
\n
### Tecnologias \n
Java; Spring Boot; PostgreSQL; JavaFX; HTML/CSS; GitHub \n
\n
### Contribuições Pessoais \n
Modelagem BD; entidades; repositórios; autenticação JWT; CI/CD \n
\n
### Hard Skills \n
Java/Spring Boot (ensino); PostgreSQL (ensino); Git/GitHub (autonomia) \n
\n
### Soft Skills \n
Comunicação, proatividade e resiliência \n
\n
## API 2º Semestre \n
\n
**Empresa Parceira:** FATEC São José dos Campos – Professor responsável: *Fabiano Sabha Walczak* \n
\n
### Problema \n
A Fatec enfrentava dificuldades na gestão de solicitações de manutenção em seus laboratórios. As ocorrências eram registradas em planilhas separadas por setor, sem controle de prioridade ou rastreabilidade. Isso gerava retrabalho, atrasos e perda de informações sobre o status das solicitações. \n
\n
### Solução \n
Foi desenvolvido um sistema **web** de abertura e acompanhamento de chamados internos, com autenticação, histórico de solicitações e dashboards de controle. O sistema permite o registro, atualização e encerramento de chamados com rastreamento por usuário e prioridade. \n
\n
**Repositório:** [https://github.com/LizardsDBA/API-2025-1](https://github.com/LizardsDBA/API-2025-1) \n
\n
### Tecnologias \n
| Tecnologia | Utilização | \n
|-------------|-------------| \n
| Java | Lógica de negócio (backend) | \n
| Spring Boot | Framework principal da aplicação | \n
| PostgreSQL | Banco de dados relacional | \n
| JavaFX | Protótipo desktop para testes locais | \n
| HTML/CSS | Interface web | \n
| GitHub | Versionamento de código e controle de versões | \n
\n
### Contribuições Pessoais \n
Atuei como **Desenvolvedor** na equipe Scrum. \n
Fui responsável pela modelagem do banco de dados, criação das entidades principais, configuração da camada de persistência e integração com o backend utilizando **Spring Data JPA**. \n
Implementei o módulo de autenticação via **JWT**, as regras de negócio de abertura e encerramento de chamados e configurei o CI/CD do repositório. \n
\n
<details> \n
<summary>Detalhes Técnicos</summary> \n
\n
- Modelagem ER normalizada até 3FN \n
- Scripts SQL para geração de schema \n
- Configuração de rotas REST e endpoints `/chamados` e `/usuarios` \n
- Implementação do controle de status com enums \n
- Integração de logs e auditoria com JPA Events \n
\n
</details> \n
\n
### Hard Skills \n
| Tecnologia | Nível | \n
|-------------|--------| \n
| Java / Spring Boot | Consigo ensinar | \n
| PostgreSQL | Consigo ensinar | \n
| Git / GitHub | Faço com autonomia | \n
| HTML / CSS | Faço com ajuda | \n
| Docker | Ouvi falar | \n
\n
### Soft Skills \n
Durante uma **daily meeting**, foi discutido um problema de lentidão em consultas SQL. Usei minha **comunicação e proatividade** para propor o uso de índices compostos e expliquei ao time como isso impactaria na performance. \n
Além disso, demonstrei **colaboração** ao revisar códigos de outros membros via pull requests e **resiliência** durante o período de entregas críticas do sprint. \n
\n
## API 3º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## API 4º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## API 5º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## API 6º Semestre \n
(Conteúdo igual ao da API 2 na sua seed) \n
\n
## Tabela Resumo dos Projetos API \n
\n
| Semestre | Empresa Parceira | Solução Desenvolvida | \n
|---|---|---| \n
| 1º Semestre (2023-2) | FATEC São José dos Campos | calculadora | \n
| 2º Semestre (2024-1) | FATEC São José dos Campos | api | \n
| 3º Semestre (2024-2) | futuro | futuro | \n
| 4º Semestre (2025-1) | futuro | futuro | \n
| 5º Semestre (2025-2) | futuro | futuro | \n
| 6º Semestre (2026-1) | futuro | futuro | \n
\n
## Considerações Finais \n
\n
Durante a trajetória na FATEC, pude consolidar conhecimentos técnicos em modelagem de dados, desenvolvimento de APIs e versionamento de código, além de aprimorar habilidades interpessoais em comunicação, empatia e trabalho em equipe. \n
O modelo de aprendizado baseado em **APIs semestrais** foi essencial para vivenciar o ciclo completo de desenvolvimento — da análise de requisitos à entrega final. \n
\n
Acredito que o aprendizado obtido, aliado à prática profissional, me prepara para atuar de forma sólida como Analista e Desenvolvedor de Banco de Dados, contribuindo para soluções de valor e impacto real no ambiente corporativo. \n',
    NULL
    WHERE @tg4_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM versoes_trabalho v WHERE v.trabalho_id=@tg4_id AND v.secao='COMPLETO' AND v.versao='v1'
);