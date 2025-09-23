package br.edu.fatec.api.dao.impl;

import br.edu.fatec.api.config.Database;
import br.edu.fatec.api.dao.UsuarioDAO;
import br.edu.fatec.api.model.Usuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementação SQLite do UsuarioDAO
 */
public class UsuarioDAOImpl implements UsuarioDAO {
    
    private static final String INSERT_SQL = 
        "INSERT INTO usuarios (nome, email, senha, tipo, ativo) VALUES (?, ?, ?, ?, ?)";
    
    private static final String UPDATE_SQL = 
        "UPDATE usuarios SET nome = ?, email = ?, senha = ?, tipo = ?, ativo = ? WHERE id = ?";
    
    private static final String DELETE_SQL = 
        "DELETE FROM usuarios WHERE id = ?";
    
    private static final String SELECT_BY_ID_SQL = 
        "SELECT * FROM usuarios WHERE id = ?";
    
    private static final String SELECT_ALL_SQL = 
        "SELECT * FROM usuarios ORDER BY nome";
    
    private static final String SELECT_BY_EMAIL_SQL = 
        "SELECT * FROM usuarios WHERE email = ?";
    
    private static final String SELECT_BY_TIPO_SQL = 
        "SELECT * FROM usuarios WHERE tipo = ? ORDER BY nome";
    
    private static final String SELECT_ATIVOS_SQL = 
        "SELECT * FROM usuarios WHERE ativo = 1 ORDER BY nome";
    
    private static final String COUNT_SQL = 
        "SELECT COUNT(*) FROM usuarios";
    
    private static final String EXISTS_SQL = 
        "SELECT 1 FROM usuarios WHERE id = ?";
    
    private static final String EMAIL_EXISTS_SQL = 
        "SELECT 1 FROM usuarios WHERE email = ?";
    
    private static final String AUTHENTICATE_SQL = 
        "SELECT * FROM usuarios WHERE email = ? AND senha = ? AND ativo = 1";
    
    @Override
    public Usuario save(Usuario usuario) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getSenha());
            stmt.setString(4, usuario.getTipo().name());
            stmt.setBoolean(5, usuario.isAtivo());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Falha ao criar usuário, nenhuma linha afetada.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    usuario.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Falha ao criar usuário, ID não obtido.");
                }
            }
            
            return usuario;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar usuário", e);
        }
    }
    
    @Override
    public Usuario update(Usuario usuario) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            
            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getSenha());
            stmt.setString(4, usuario.getTipo().name());
            stmt.setBoolean(5, usuario.isAtivo());
            stmt.setLong(6, usuario.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Falha ao atualizar usuário, usuário não encontrado.");
            }
            
            return usuario;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar usuário", e);
        }
    }
    
    @Override
    public boolean delete(Long id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {
            
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao deletar usuário", e);
        }
    }
    
    @Override
    public Optional<Usuario> findById(Long id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {
            
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUsuario(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário por ID", e);
        }
    }
    
    @Override
    public List<Usuario> findAll() {
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                usuarios.add(mapResultSetToUsuario(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar todos os usuários", e);
        }
        return usuarios;
    }
    
    @Override
    public long count() {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(COUNT_SQL);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar usuários", e);
        }
    }
    
    @Override
    public boolean exists(Long id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(EXISTS_SQL)) {
            
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar existência do usuário", e);
        }
    }
    
    @Override
    public Optional<Usuario> findByEmail(String email) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_EMAIL_SQL)) {
            
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUsuario(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário por email", e);
        }
    }
    
    @Override
    public List<Usuario> findByTipo(Usuario.TipoUsuario tipo) {
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_TIPO_SQL)) {
            
            stmt.setString(1, tipo.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    usuarios.add(mapResultSetToUsuario(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuários por tipo", e);
        }
        return usuarios;
    }
    
    @Override
    public List<Usuario> findAtivos() {
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ATIVOS_SQL);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                usuarios.add(mapResultSetToUsuario(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuários ativos", e);
        }
        return usuarios;
    }
    
    @Override
    public boolean emailExists(String email) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(EMAIL_EXISTS_SQL)) {
            
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar existência do email", e);
        }
    }
    
    @Override
    public Optional<Usuario> authenticate(String email, String senha) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(AUTHENTICATE_SQL)) {
            
            stmt.setString(1, email);
            stmt.setString(2, senha);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUsuario(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao autenticar usuário", e);
        }
    }
    
    private Usuario mapResultSetToUsuario(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getLong("id"));
        usuario.setNome(rs.getString("nome"));
        usuario.setEmail(rs.getString("email"));
        usuario.setSenha(rs.getString("senha"));
        usuario.setTipo(Usuario.TipoUsuario.valueOf(rs.getString("tipo")));
        usuario.setAtivo(rs.getBoolean("ativo"));
        return usuario;
    }
}
