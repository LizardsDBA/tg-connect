package br.edu.fatec.api.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Configuração e gerenciamento do banco de dados SQLite
 */
public class Database {
    
    private static final String DB_URL = "jdbc:sqlite:api.db";
    private static final String DRIVER_CLASS = "org.sqlite.JDBC";
    
    static {
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver SQLite não encontrado", e);
        }
    }
    
    /**
     * Obtém uma conexão com o banco de dados
     * @deprecated Use getConnection() instead
     */
    @Deprecated
    public static Connection get() throws SQLException {
        return getConnection();
    }
    
    /**
     * Obtém uma conexão com o banco de dados
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
    
    /**
     * Inicializa o banco de dados criando as tabelas necessárias
     */
    public static void initDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Habilita foreign keys no SQLite
            stmt.execute("PRAGMA foreign_keys = ON");
            
            // Tabela de usuários
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    senha TEXT NOT NULL,
                    tipo TEXT NOT NULL CHECK (tipo IN ('ALUNO', 'ORIENTADOR', 'COORDENADOR')),
                    ativo BOOLEAN NOT NULL DEFAULT 1,
                    data_criacao DATETIME DEFAULT CURRENT_TIMESTAMP,
                    data_atualizacao DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Tabela de trabalhos de graduação
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS trabalhos_graduacao (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    titulo TEXT NOT NULL,
                    tema TEXT,
                    conteudo TEXT,
                    versao TEXT NOT NULL DEFAULT '1.0',
                    status TEXT NOT NULL DEFAULT 'INICIADO' 
                        CHECK (status IN ('INICIADO', 'EM_ANDAMENTO', 'AGUARDANDO_REVISAO', 
                                         'EM_REVISAO', 'APROVADO', 'REPROVADO', 'CONCLUIDO')),
                    percentual_conclusao REAL NOT NULL DEFAULT 0.0 CHECK (percentual_conclusao >= 0 AND percentual_conclusao <= 100),
                    aluno_id INTEGER NOT NULL,
                    orientador_id INTEGER NOT NULL,
                    data_inicio DATETIME DEFAULT CURRENT_TIMESTAMP,
                    data_ultima_modificacao DATETIME DEFAULT CURRENT_TIMESTAMP,
                    data_entrega DATETIME,
                    FOREIGN KEY (aluno_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                    FOREIGN KEY (orientador_id) REFERENCES usuarios(id) ON DELETE CASCADE
                )
            """);
            
            // Tabela de mensagens/chat
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mensagens (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    conteudo TEXT NOT NULL,
                    remetente_id INTEGER NOT NULL,
                    destinatario_id INTEGER NOT NULL,
                    trabalho_id INTEGER,
                    secao TEXT,
                    tipo TEXT NOT NULL DEFAULT 'COMENTARIO' 
                        CHECK (tipo IN ('COMENTARIO', 'FEEDBACK', 'SOLICITACAO', 'APROVACAO', 'REJEICAO')),
                    lida BOOLEAN NOT NULL DEFAULT 0,
                    data_envio DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (remetente_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                    FOREIGN KEY (destinatario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                    FOREIGN KEY (trabalho_id) REFERENCES trabalhos_graduacao(id) ON DELETE CASCADE
                )
            """);
            
            // Tabela de versões do trabalho (histórico)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS versoes_trabalho (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    trabalho_id INTEGER NOT NULL,
                    versao TEXT NOT NULL,
                    conteudo TEXT NOT NULL,
                    comentario TEXT,
                    data_criacao DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (trabalho_id) REFERENCES trabalhos_graduacao(id) ON DELETE CASCADE
                )
            """);
            
            // Tabela de notificações
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notificacoes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario_id INTEGER NOT NULL,
                    titulo TEXT NOT NULL,
                    conteudo TEXT NOT NULL,
                    tipo TEXT NOT NULL DEFAULT 'INFO' 
                        CHECK (tipo IN ('INFO', 'SUCESSO', 'AVISO', 'ERRO')),
                    lida BOOLEAN NOT NULL DEFAULT 0,
                    data_criacao DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
                )
            """);
            
            // Índices para melhor performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_usuarios_tipo ON usuarios(tipo)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_trabalhos_aluno ON trabalhos_graduacao(aluno_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_trabalhos_orientador ON trabalhos_graduacao(orientador_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_mensagens_destinatario ON mensagens(destinatario_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_mensagens_trabalho ON mensagens(trabalho_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_notificacoes_usuario ON notificacoes(usuario_id)");
            
            // Triggers para atualizar data_atualizacao
            stmt.execute("""
                CREATE TRIGGER IF NOT EXISTS update_usuarios_timestamp 
                AFTER UPDATE ON usuarios
                BEGIN
                    UPDATE usuarios SET data_atualizacao = CURRENT_TIMESTAMP WHERE id = NEW.id;
                END
            """);
            
            stmt.execute("""
                CREATE TRIGGER IF NOT EXISTS update_trabalhos_timestamp 
                AFTER UPDATE ON trabalhos_graduacao
                BEGIN
                    UPDATE trabalhos_graduacao SET data_ultima_modificacao = CURRENT_TIMESTAMP WHERE id = NEW.id;
                END
            """);
            
            System.out.println("Banco de dados inicializado com sucesso!");
            
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inicializar banco de dados", e);
        }
    }
    
    /**
     * Insere dados de exemplo para desenvolvimento
     */
    public static void insertSampleData() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Verifica se já existem dados
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM usuarios");
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Dados de exemplo já existem no banco.");
                return;
            }
            
            // Usuários de exemplo
            stmt.execute("""
                INSERT INTO usuarios (nome, email, senha, tipo) VALUES 
                ('João Silva', 'joao.aluno@fatec.sp.gov.br', '123456', 'ALUNO'),
                ('Maria Santos', 'maria.aluna@fatec.sp.gov.br', '123456', 'ALUNO'),
                ('Prof. Ana Costa', 'ana.orientadora@fatec.sp.gov.br', '123456', 'ORIENTADOR'),
                ('Prof. Carlos Lima', 'carlos.orientador@fatec.sp.gov.br', '123456', 'ORIENTADOR'),
                ('Coord. Roberto Souza', 'roberto.coordenador@fatec.sp.gov.br', '123456', 'COORDENADOR')
            """);
            
            // Trabalhos de exemplo
            stmt.execute("""
                INSERT INTO trabalhos_graduacao (titulo, tema, aluno_id, orientador_id, percentual_conclusao, status) VALUES 
                ('Sistema de Gestão Acadêmica', 'Desenvolvimento de Software', 1, 3, 48.0, 'EM_ANDAMENTO'),
                ('Aplicativo Mobile para Delivery', 'Desenvolvimento Mobile', 2, 4, 25.0, 'INICIADO')
            """);
            
            // Mensagens de exemplo
            stmt.execute("""
                INSERT INTO mensagens (conteudo, remetente_id, destinatario_id, trabalho_id, tipo, secao) VALUES 
                ('Ótimo progresso na introdução! Continue assim.', 3, 1, 1, 'FEEDBACK', 'Introdução'),
                ('Precisa revisar a metodologia conforme as normas ABNT.', 3, 1, 1, 'SOLICITACAO', 'Metodologia'),
                ('Quando podemos agendar uma reunião para discutir o projeto?', 2, 4, 2, 'COMENTARIO', null)
            """);
            
            System.out.println("Dados de exemplo inseridos com sucesso!");
            
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir dados de exemplo", e);
        }
    }
    
    /**
     * Testa a conexão com o banco de dados
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Erro ao testar conexão: " + e.getMessage());
            return false;
        }
    }
}
