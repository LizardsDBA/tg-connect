package br.edu.fatec.api.dao;

import br.edu.fatec.api.model.Usuario;
import java.util.Optional;
import java.util.List;

/**
 * DAO específico para operações com Usuario
 */
public interface UsuarioDAO extends GenericDAO<Usuario, Long> {
    
    /**
     * Busca usuário por email
     * @param email Email do usuário
     * @return Optional contendo o usuário se encontrado
     */
    Optional<Usuario> findByEmail(String email);
    
    /**
     * Busca usuários por tipo
     * @param tipo Tipo do usuário
     * @return Lista de usuários do tipo especificado
     */
    List<Usuario> findByTipo(Usuario.TipoUsuario tipo);
    
    /**
     * Busca usuários ativos
     * @return Lista de usuários ativos
     */
    List<Usuario> findAtivos();
    
    /**
     * Verifica se email já existe
     * @param email Email a ser verificado
     * @return true se email já existe
     */
    boolean emailExists(String email);
    
    /**
     * Autentica usuário
     * @param email Email do usuário
     * @param senha Senha do usuário
     * @return Optional contendo o usuário se autenticado
     */
    Optional<Usuario> authenticate(String email, String senha);
}
