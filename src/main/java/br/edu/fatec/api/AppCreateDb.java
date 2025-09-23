package br.edu.fatec.api;

import br.edu.fatec.api.config.Database;

/**
 * Aplicação utilitária para criar e inicializar o banco de dados
 */
public class AppCreateDb {
    
    public static void main(String[] args) {
        System.out.println("=== Inicializando Banco de Dados ===");
        
        try {
            // Testa a conexão
            if (Database.testConnection()) {
                System.out.println("✓ Conexão com banco de dados estabelecida");
            } else {
                System.err.println("✗ Falha na conexão com banco de dados");
                return;
            }
            
            // Inicializa as tabelas
            Database.initDatabase();
            System.out.println("✓ Estrutura do banco de dados criada");
            
            // Insere dados de exemplo
            Database.insertSampleData();
            System.out.println("✓ Dados de exemplo inseridos");
            
            System.out.println("\n=== Banco de Dados Pronto para Uso ===");
            System.out.println("Usuários de exemplo criados:");
            System.out.println("- Aluno: joao.aluno@fatec.sp.gov.br / 123456");
            System.out.println("- Aluna: maria.aluna@fatec.sp.gov.br / 123456");
            System.out.println("- Orientadora: ana.orientadora@fatec.sp.gov.br / 123456");
            System.out.println("- Orientador: carlos.orientador@fatec.sp.gov.br / 123456");
            System.out.println("- Coordenador: roberto.coordenador@fatec.sp.gov.br / 123456");
            
        } catch (Exception e) {
            System.err.println("✗ Erro ao inicializar banco de dados: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
