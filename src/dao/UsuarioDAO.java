package dao;

import factory.ConnectionFactory;
import model.Usuario;
import util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    private Connection connection;

    public UsuarioDAO() throws SQLException {
        this.connection = new ConnectionFactory().getConnection();
    }

    // Método para autenticar usuário (RF01, RF02)
    public Usuario autenticar(String email, String senhaFornecida) {
        String sql = "SELECT id_usuario, email, senha_hash, salt, is_admin, is_active FROM usuarios WHERE email = ? AND is_active = TRUE";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("senha_hash");
                String salt = rs.getString("salt");

                // Verifica a senha usando o utilitário (RNF03)
                if (PasswordUtil.verifyPassword(senhaFornecida, storedHash, salt)) {
                    Usuario usuario = new Usuario();
                    usuario.setIdUsuario(rs.getInt("id_usuario"));
                    usuario.setEmail(rs.getString("email"));
                    usuario.setSenhaHash(storedHash); // Não retornar hash e salt para a aplicação principal por segurança
                    usuario.setSalt(salt);           // Idealmente, a sessão teria apenas ID e permissões
                    usuario.setAdmin(rs.getBoolean("is_admin"));
                    usuario.setActive(rs.getBoolean("is_active"));
                    return usuario; // Autenticação bem-sucedida
                }
            }
            return null; // Usuário não encontrado ou senha incorreta
        } catch (SQLException e) {
            System.err.println("Erro ao autenticar usuário: " + e.getMessage());
            // Lançar exceção ou tratar o erro conforme necessário
            return null;
        }
    }

    // Método para criar um novo usuário (RF15)
    public boolean criarUsuario(Usuario usuario, String senha) {
        // Validar domínio @uepa.br na aplicação antes de chamar o DAO (RF01)
        if (!usuario.getEmail().toLowerCase().endsWith("@uepa.br")) {
            System.err.println("Erro: Email inválido. Deve pertencer ao domínio @uepa.br.");
            return false;
        }

        String sql = "INSERT INTO usuarios (email, senha_hash, salt, is_admin, is_active) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword(senha, salt);

            stmt.setString(1, usuario.getEmail());
            stmt.setString(2, hash);
            stmt.setString(3, salt);
            stmt.setBoolean(4, usuario.isAdmin());
            stmt.setBoolean(5, usuario.isActive()); // Por padrão, ativo

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        usuario.setIdUsuario(generatedKeys.getInt(1));
                        return true; // Usuário criado com sucesso
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Erro ao criar usuário: " + e.getMessage());
            // Verificar se é erro de e-mail duplicado (UNIQUE constraint)
            if (e.getMessage().contains("Duplicate entry")) {
                 System.err.println("E-mail já cadastrado.");
            }
            return false;
        }
    }

    // Método para buscar um usuário por ID
    public Usuario buscarPorId(int id) {
        String sql = "SELECT id_usuario, email, is_admin, is_active FROM usuarios WHERE id_usuario = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setIdUsuario(rs.getInt("id_usuario"));
                usuario.setEmail(rs.getString("email"));
                usuario.setAdmin(rs.getBoolean("is_admin"));
                usuario.setActive(rs.getBoolean("is_active"));
                // Não retornar hash e salt
                return usuario;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário por ID: " + e.getMessage());
        }
        return null;
    }
    
    // Método para buscar um usuário por Email
    public Usuario buscarPorEmail(String email) {
        String sql = "SELECT id_usuario, email, is_admin, is_active FROM usuarios WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setIdUsuario(rs.getInt("id_usuario"));
                usuario.setEmail(rs.getString("email"));
                usuario.setAdmin(rs.getBoolean("is_admin"));
                usuario.setActive(rs.getBoolean("is_active"));
                // Não retornar hash e salt
                return usuario;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário por Email: " + e.getMessage());
        }
        return null;
    }

    // Método para listar todos os usuários (para admin - RF15)
    public List<Usuario> listarTodos() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT id_usuario, email, is_admin, is_active FROM usuarios ORDER BY email";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setIdUsuario(rs.getInt("id_usuario"));
                usuario.setEmail(rs.getString("email"));
                usuario.setAdmin(rs.getBoolean("is_admin"));
                usuario.setActive(rs.getBoolean("is_active"));
                usuarios.add(usuario);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar usuários: " + e.getMessage());
        }
        return usuarios;
    }

    // Método para atualizar dados do usuário (exceto senha) (RF15)
    public boolean atualizarUsuario(Usuario usuario) {
         // Validar domínio @uepa.br na aplicação
        if (!usuario.getEmail().toLowerCase().endsWith("@uepa.br")) {
            System.err.println("Erro: Email inválido. Deve pertencer ao domínio @uepa.br.");
            return false;
        }
        String sql = "UPDATE usuarios SET email = ?, is_admin = ?, is_active = ? WHERE id_usuario = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, usuario.getEmail());
            stmt.setBoolean(2, usuario.isAdmin());
            stmt.setBoolean(3, usuario.isActive());
            stmt.setInt(4, usuario.getIdUsuario());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar usuário: " + e.getMessage());
             if (e.getMessage().contains("Duplicate entry")) {
                 System.err.println("E-mail já cadastrado por outro usuário.");
            }
            return false;
        }
    }

    // Método para atualizar a senha de um usuário (pode ser usado por admin ou pelo próprio usuário)
    public boolean atualizarSenha(int idUsuario, String novaSenha) {
        String sqlSelect = "SELECT salt FROM usuarios WHERE id_usuario = ?";
        String sqlUpdate = "UPDATE usuarios SET senha_hash = ?, salt = ? WHERE id_usuario = ?";
        
        try {
             // Primeiro, obtemos o salt atual ou geramos um novo se necessário (embora o ideal seja sempre ter um)
            String salt;
            try (PreparedStatement stmtSelect = connection.prepareStatement(sqlSelect)) {
                stmtSelect.setInt(1, idUsuario);
                ResultSet rs = stmtSelect.executeQuery();
                if (rs.next() && rs.getString("salt") != null) {
                    salt = rs.getString("salt");
                } else {
                    // Se não houver salt (improvável com a lógica de criação), gera um novo
                    salt = PasswordUtil.generateSalt(); 
                }
            }

            String novoHash = PasswordUtil.hashPassword(novaSenha, salt);

            try (PreparedStatement stmtUpdate = connection.prepareStatement(sqlUpdate)) {
                stmtUpdate.setString(1, novoHash);
                stmtUpdate.setString(2, salt); // Atualiza o salt caso tenha sido gerado um novo
                stmtUpdate.setInt(3, idUsuario);
                int affectedRows = stmtUpdate.executeUpdate();
                return affectedRows > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar senha: " + e.getMessage());
            return false;
        }
    }

    // Método para inativar/reativar usuário (remoção lógica - RF15)
    public boolean definirStatusAtivo(int idUsuario, boolean isActive) {
        String sql = "UPDATE usuarios SET is_active = ? WHERE id_usuario = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, isActive);
            stmt.setInt(2, idUsuario);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao definir status ativo do usuário: " + e.getMessage());
            return false;
        }
    }

    // Fechar conexão (opcional, depende da gestão do ciclo de vida da conexão)
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Erro ao fechar conexão DAO: " + e.getMessage());
        }
    }
}

