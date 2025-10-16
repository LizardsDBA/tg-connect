package br.edu.fatec.api.dao;

import br.edu.fatec.api.model.auth.User;   // <-- FALTAVA ESTE IMPORT
import java.util.Optional;

public interface LoginDao {
    Optional<User> authenticate(String email, String senhaEmTextoClaro);
}
