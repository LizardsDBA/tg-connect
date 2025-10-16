package br.edu.fatec.api.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class Database {
    // URL para executar bootstrap (sem schema)
    private static final String ROOT_URL = "jdbc:mysql://localhost:3306/?serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true&useSSL=false";
    // URL da aplicação (com schema tg_connect)
    private static final String DB_URL   = "jdbc:mysql://localhost:3306/tg_connect?serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true&useSSL=false";

    // credenciais root (apenas p/ bootstrap). Se não tiver, deixe em branco.
    private static final String ROOT_USER = "root";
    private static final String ROOT_PASS = "123456";

    // usuário da aplicação
    private static final String APP_USER = "tguser";
    private static final String APP_PASS = "pass123";

    private Database() {}

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(DB_URL, APP_USER, APP_PASS);
    }

    public static void initialize() {
        // 1) Tentar rodar bootstrap com root (criar DB/usuário/grants). Se falhar por permissão, seguimos.
        runSqlFromResource(ROOT_URL, ROOT_USER, ROOT_PASS, "/db/schema-bootstrap.sql", true);

        // 2) Rodar schema da app (tabelas + seeds) com usuário da app ou com root se preferir
        // Se a app ainda não existir, a primeira conexão com tguser pode falhar; então tentamos root como fallback.
        boolean ok = runSqlFromResource(DB_URL, APP_USER, APP_PASS, "/db/schema-app.sql", false);
        if (!ok) {
            System.err.println("⚠️ Tentando rodar schema-app.sql com root como fallback...");
            runSqlFromResource(DB_URL, ROOT_USER, ROOT_PASS, "/db/schema-app.sql", false);
        }
    }

    private static boolean runSqlFromResource(String url, String user, String pass, String resource, boolean ignoreErrors) {
        InputStream in = Database.class.getResourceAsStream(resource);
        if (in == null) {
            System.err.println("❌ Arquivo não encontrado: " + resource);
            return false;
        }
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            conn.setAutoCommit(true);
            String sql = readAll(in);
            List<String> statements = splitStatements(sql);
            try (Statement st = conn.createStatement()) {
                for (String s : statements) {
                    String q = s.trim();
                    if (!q.isEmpty()) {
                        st.execute(q);
                    }
                }
            }
            System.out.println("✅ Executado com sucesso: " + resource + " (user=" + user + ")");
            return true;
        } catch (Exception e) {
            System.err.println("⚠️ Falha ao executar " + resource + " com " + user + ": " + e.getMessage());
            if (!ignoreErrors) return false;
            return true; // ignora erros no bootstrap quando sem permissão
        }
    }

    private static String readAll(InputStream in) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    // Divisor simples por ';' no final de linha. Evitamos problemas removendo comentários e linhas vazias.
    // Como a trigger usa 1 instrução sem BEGIN/END, não precisamos lidar com DELIMITER.
    private static List<String> splitStatements(String sql) {
        List<String> list = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String rawLine : sql.split("\n")) {
            String line = rawLine.replaceAll("--.*$", "").trim(); // remove comentários '--'
            if (line.isEmpty()) continue;
            cur.append(line).append(' ');
            if (line.endsWith(";")) {
                list.add(cur.toString());
                cur.setLength(0);
            }
        }
        if (cur.length() > 0) list.add(cur.toString());
        return list;
    }
}