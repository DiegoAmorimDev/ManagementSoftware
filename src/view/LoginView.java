package view;

import dao.UsuarioDAO;
import model.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

public class LoginView extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;
    private UsuarioDAO usuarioDAO;

    public LoginView() {
        try {
            usuarioDAO = new UsuarioDAO();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar ao banco de dados: " + e.getMessage(), "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
            System.exit(1); // Encerra se não puder conectar
        }

        setTitle("Login - Sistema de Gestão de Estoque");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centraliza na tela
        setLayout(new BorderLayout(10, 10)); // Layout principal com espaçamento

        // Painel para os campos de entrada
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Espaçamento interno
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("E-mail (@uepa.br): "), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        emailField = new JTextField(20);
        inputPanel.add(emailField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        inputPanel.add(new JLabel("Senha:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        inputPanel.add(passwordField, gbc);

        // Painel para o botão e status
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        loginButton = new JButton("Entrar");
        statusLabel = new JLabel(" "); // Espaço inicial para status
        statusLabel.setForeground(Color.RED);
        bottomPanel.add(loginButton);

        // Adiciona painéis ao frame
        add(inputPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH); // Status acima

        // Ação do botão de login
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });
        
        // Permite pressionar Enter para logar
        getRootPane().setDefaultButton(loginButton);

        setVisible(true);
    }

    private void performLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Validação básica de entrada
        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("E-mail e senha são obrigatórios.");
            return;
        }
        
        // Validação do domínio @uepa.br (RF01)
        if (!email.toLowerCase().endsWith("@uepa.br")) {
             statusLabel.setText("E-mail inválido. Use seu e-mail @uepa.br.");
             return;
        }

        try {
            Usuario usuarioAutenticado = usuarioDAO.autenticar(email, password);

            if (usuarioAutenticado != null) {
                statusLabel.setText("Login bem-sucedido!");
                // Abrir a tela principal e fechar a de login
                dispose(); // Fecha a janela de login
                // Passa o usuário autenticado para a próxima tela
                new MainMenuView(usuarioAutenticado); 
            } else {
                statusLabel.setText("E-mail ou senha inválidos.");
            }
        } catch (Exception ex) {
            statusLabel.setText("Erro durante o login.");
            JOptionPane.showMessageDialog(this, "Erro ao tentar fazer login: " + ex.getMessage(), "Erro de Login", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace(); // Log detalhado no console
        }
    }

    // Método main para iniciar a aplicação pela tela de login
    public static void main(String[] args) {
        // Define o Look and Feel para uma aparência mais moderna (Nimbus)
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Se Nimbus não estiver disponível, usa o padrão do sistema
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        // Garante que a criação da GUI ocorra na Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new LoginView();
            }
        });
    }
}

