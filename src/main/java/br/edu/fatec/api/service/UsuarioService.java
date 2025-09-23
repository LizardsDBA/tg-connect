package br.edu.fatec.api.service;

import br.edu.fatec.api.dao.UsuarioDAO;
import br.edu.fatec.api.dao.impl.UsuarioDAOImpl;
import br.edu.fatec.api.model.Usuario;

import java.util.List;
import java.util.Optional;

/**
 * Serviço para gerenciar operações de usuários
 */
public class UsuarioService {
    
    private final UsuarioDAO usuarioDAO;
    
    public UsuarioService() {
        this.usuarioDAO = new UsuarioDAOImpl();
    }
    
    public UsuarioService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }
    
    /**
     * Cria um novo usuário
     */
    public Usuario criarUsuario(String nome, String email, String senha, Usuario.TipoUsuario tipo) {
        // Validações
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email é obrigatório");
        }
        if (senha == null || senha.trim().isEmpty()) {
            throw new IllegalArgumentException("Senha é obrigatória");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de usuário é obrigatório");
        }
        
        // Verifica se email já existe
        if (usuarioDAO.emailExists(email)) {
            throw new IllegalArgumentException("Email já está em uso");
        }
        
        Usuario usuario = new Usuario(nome.trim(), email.trim().toLowerCase(), senha, tipo);
        return usuarioDAO.save(usuario);
    }
    
    /**
     * Autentica um usuário
     */
    public Optional<Usuario> autenticar(String email, String senha) {
        if (email == null || email.trim().isEmpty() || senha == null || senha.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return usuarioDAO.authenticate(email.trim().toLowerCase(), senha);
    }
    
    /**
     * Busca usuário por ID
     */
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioDAO.findById(id);
    }
    
    /**
     * Busca usuário por email
     */
    public Optional<Usuario> buscarPorEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        return usuarioDAO.findByEmail(email.trim().toLowerCase());
    }
    
    /**
     * Lista todos os usuários
     */
    public List<Usuario> listarTodos() {
        return usuarioDAO.findAll();
    }
    
    /**
     * Lista usuários por tipo
     */
    public List<Usuario> listarPorTipo(Usuario.TipoUsuario tipo) {
        return usuarioDAO.findByTipo(tipo);
    }
    
    /**
     * Lista usuários ativos
     */
    public List<Usuario> listarAtivos() {
        return usuarioDAO.findAtivos();
    }
    
    /**
     * Atualiza um usuário
     */
    public Usuario atualizar(Usuario usuario) {
        if (usuario.getId() == null) {
            throw new IllegalArgumentException("ID do usuário é obrigatório para atualização");
        }
        
        // Verifica se usuário existe
        if (!usuarioDAO.exists(usuario.getId())) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }
        
        // Verifica se email já existe para outro usuário
        Optional<Usuario> usuarioExistente = usuarioDAO.findByEmail(usuario.getEmail());
        if (usuarioExistente.isPresent() && !usuarioExistente.get().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Email já está em uso por outro usuário");
        }
        
        return usuarioDAO.update(usuario);
    }
    
    /**
     * Desativa um usuário
     */
    public boolean desativar(Long id) {
        Optional<Usuario> usuario = usuarioDAO.findById(id);
        if (usuario.isPresent()) {
            Usuario u = usuario.get();
            u.setAtivo(false);
            usuarioDAO.update(u);
            return true;
        }
        return false;
    }
    
    /**
     * Ativa um usuário
     */
    public boolean ativar(Long id) {
        Optional<Usuario> usuario = usuarioDAO.findById(id);
        if (usuario.isPresent()) {
            Usuario u = usuario.get();
            u.setAtivo(true);
            usuarioDAO.update(u);
            return true;
        }
        return false;
    }
    
    /**
     * Remove um usuário permanentemente
     */
    public boolean remover(Long id) {
        return usuarioDAO.delete(id);
    }
    
    /**
     * Conta total de usuários
     */
    public long contarUsuarios() {
        return usuarioDAO.count();
    }
}
