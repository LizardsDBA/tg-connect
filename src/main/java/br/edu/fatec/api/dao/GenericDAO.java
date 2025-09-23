package br.edu.fatec.api.dao;

import java.util.List;
import java.util.Optional;

/**
 * Interface genérica para operações de banco de dados
 * @param <T> Tipo da entidade
 * @param <ID> Tipo do identificador
 */
public interface GenericDAO<T, ID> {
    
    /**
     * Salva uma entidade no banco de dados
     * @param entity Entidade a ser salva
     * @return Entidade salva com ID gerado
     */
    T save(T entity);
    
    /**
     * Atualiza uma entidade existente
     * @param entity Entidade a ser atualizada
     * @return Entidade atualizada
     */
    T update(T entity);
    
    /**
     * Remove uma entidade pelo ID
     * @param id ID da entidade a ser removida
     * @return true se removida com sucesso
     */
    boolean delete(ID id);
    
    /**
     * Busca uma entidade pelo ID
     * @param id ID da entidade
     * @return Optional contendo a entidade se encontrada
     */
    Optional<T> findById(ID id);
    
    /**
     * Lista todas as entidades
     * @return Lista de todas as entidades
     */
    List<T> findAll();
    
    /**
     * Conta o número total de entidades
     * @return Número total de entidades
     */
    long count();
    
    /**
     * Verifica se existe uma entidade com o ID especificado
     * @param id ID a ser verificado
     * @return true se existe
     */
    boolean exists(ID id);
}
