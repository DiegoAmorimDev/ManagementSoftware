package model;

public class Usuario {
    private int idUsuario;
    private String email;
    private String senhaHash;
    private String salt;
    private boolean isAdmin;
    private boolean isActive;

    // Construtores
    public Usuario() {}

    public Usuario(int idUsuario, String email, String senhaHash, String salt, boolean isAdmin, boolean isActive) {
        this.idUsuario = idUsuario;
        this.email = email;
        this.senhaHash = senhaHash;
        this.salt = salt;
        this.isAdmin = isAdmin;
        this.isActive = isActive;
    }

    // Getters e Setters
    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "idUsuario=" + idUsuario +
                ", email=\'" + email + '\'' +
                ", isAdmin=" + isAdmin +
                ", isActive=" + isActive +
                '}';
    }
}

