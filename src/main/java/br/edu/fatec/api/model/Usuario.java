package br.edu.fatec.api.model;

/**
 * Modelo base para usuários do sistema
 */
public class Usuario {
    private Long id;
    private String nome;
    private String email;
    private String senha;
    private TipoUsuario tipo;
    private boolean ativo;
    
    public enum TipoUsuario {
        ALUNO, ORIENTADOR, COORDENADOR
    }
    
    // Construtores
    public Usuario() {}
    
    public Usuario(String nome, String email, String senha, TipoUsuario tipo) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.tipo = tipo;
        this.ativo = true;
    }
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    
    public TipoUsuario getTipo() { return tipo; }
    public void setTipo(TipoUsuario tipo) { this.tipo = tipo; }
    
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    
    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", email='" + email + '\'' +
                ", tipo=" + tipo +
                ", ativo=" + ativo +
                '}';
    }
}
