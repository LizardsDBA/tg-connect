package br.edu.fatec.api.nav;

import br.edu.fatec.api.model.auth.User;

public final class Session {
    private static volatile User currentUser;

    private Session() {}

    public static void setUser(User u) { currentUser = u; }
    public static User getUser() { return currentUser; }
    public static void clear() { currentUser = null; }
}