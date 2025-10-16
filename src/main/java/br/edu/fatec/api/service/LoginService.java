package br.edu.fatec.api.service;

import br.edu.fatec.api.dao.LoginDao;
import br.edu.fatec.api.model.auth.User;     // <-- importe o User
import java.util.Optional;

public class LoginService {
    private final LoginDao loginDao;

    public LoginService(LoginDao loginDao) {
        this.loginDao = loginDao;
    }

    public Optional<User> login(String email, String senha) {  // <-- tipo correto
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
            return Optional.empty();
        }
        return loginDao.authenticate(email.trim(), senha);
    }
}
