package factory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    
    
    public Connection getConnection() throws SQLException {
        try {
            
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Use as variáveis de ambiente ou um arquivo de configuração para armazenar credenciais em um cenário real.
            // Para este exemplo, usamos os dados fornecidos.
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/projetojava", "root", "root");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL não encontrado! Verifique se o conector JDBC está no classpath.", e);
        } catch (SQLException e) {
            // Adiciona mais detalhes ao erro de conexão
            throw new SQLException("Não foi possível conectar ao banco de dados. Verifique URL, usuário, senha e se o servidor MySQL está ativo. Erro original: " + e.getMessage(), e);
        }
    }
}

