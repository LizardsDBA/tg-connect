package br.edu.fatec.api.controller;

import br.edu.fatec.api.nav.SceneManager;
import javafx.fxml.FXML;

/**
 * Controller para a tela de Login
 */
public class LoginController {
    
    @FXML
    public void initialize() {
        System.out.println("Login carregado");
    }
    
    @FXML
    public void goAluno() { 
        SceneManager.go("aluno/Dashboard.fxml"); 
    }
    
    @FXML
    public void goOrientador() { 
        SceneManager.go("orientador/VisaoGeral.fxml");
    }
    
    @FXML
    public void goCoordenador() { 
        SceneManager.go("coordenacao/VisaoGeral.fxml"); 
    }
}
