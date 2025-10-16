package br.edu.fatec.api.model.auth;

public class User {
    private long id;
    private String nome;
    private String email;
    private Role role;

    public User(long id, String nome, String email, Role role) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.role = role;
    }
    public long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
}