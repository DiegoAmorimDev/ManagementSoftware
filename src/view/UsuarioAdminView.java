package view;

import dao.UsuarioDAO;
import model.Usuario;
import util.PasswordUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;

public class UsuarioAdminView extends JDialog {

    private UsuarioDAO usuarioDAO;
    private JTable usuarioTable;
    private DefaultTableModel tableModel;
    private JTextField idField, emailField;
    private JPasswordField passwordField;
    private JCheckBox adminCheckBox, ativoCheckBox;
    private JButton addButton, editButton, saveButton, cancelButton, toggleActiveButton, resetPasswordButton;
    private JPanel formPanel, buttonPanel;
    private boolean editando = false;

    public UsuarioAdminView(Frame owner) {
        super(owner, "Gerenciamento de Usuários (Admin - RF15)", true);
        try {
            usuarioDAO = new UsuarioDAO();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar ao banco de dados: " + e.getMessage(), "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        setSize(700, 550);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        // --- Tabela de Usuários ---
        tableModel = new DefaultTableModel(new Object[]{"ID", "Email", "Admin", "Ativo"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                 if (columnIndex == 0) return Integer.class; // ID
                 if (columnIndex == 2 || columnIndex == 3) return Boolean.class; // Admin e Ativo
                 return String.class; // Email
            }
        };
        usuarioTable = new JTable(tableModel);
        usuarioTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        usuarioTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    selecionarUsuario();
                }
            }
        });
        // Ajustar larguras
        usuarioTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        usuarioTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        usuarioTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        usuarioTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        
        JScrollPane scrollPane = new JScrollPane(usuarioTable);
        add(scrollPane, BorderLayout.CENTER);

        // --- Formulário de Edição/Adição ---
        formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Detalhes do Usuário"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;

        // Linha 0
        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; idField = new JTextField(5); idField.setEditable(false); formPanel.add(idField, gbc);
        gbc.gridx = 2; gbc.gridy = 0; formPanel.add(new JLabel("Admin:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; adminCheckBox = new JCheckBox(); formPanel.add(adminCheckBox, gbc);
        gbc.gridx = 4; gbc.gridy = 0; formPanel.add(new JLabel("Ativo:"), gbc);
        gbc.gridx = 5; gbc.gridy = 0; ativoCheckBox = new JCheckBox(); ativoCheckBox.setEnabled(false); formPanel.add(ativoCheckBox, gbc);

        // Linha 1
        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Email (@uepa.br):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 5; gbc.fill = GridBagConstraints.HORIZONTAL; emailField = new JTextField(30); formPanel.add(emailField, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;

        // Linha 2 (Senha - apenas para adicionar ou resetar)
        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Senha (nova):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; passwordField = new JPasswordField(20); formPanel.add(passwordField, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 4; gbc.gridy = 2; gbc.gridwidth = 2; resetPasswordButton = new JButton("Resetar Senha"); formPanel.add(resetPasswordButton, gbc);
        gbc.gridwidth = 1;

        habilitarFormulario(false);
        passwordField.setEnabled(false);
        resetPasswordButton.setEnabled(false);
        add(formPanel, BorderLayout.SOUTH);

        // --- Painel de Botões ---
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        addButton = new JButton("Adicionar Novo");
        editButton = new JButton("Editar Selecionado");
        saveButton = new JButton("Salvar");
        cancelButton = new JButton("Cancelar");
        toggleActiveButton = new JButton("Ativar/Inativar");

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(toggleActiveButton);
        
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(formPanel, BorderLayout.CENTER);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);

        configurarEstadoInicialBotoes();

        // --- Ações dos Botões ---
        addButton.addActionListener(e -> adicionarNovoUsuario());
        editButton.addActionListener(e -> editarUsuarioSelecionado());
        saveButton.addActionListener(e -> salvarUsuario());
        cancelButton.addActionListener(e -> cancelarEdicao());
        toggleActiveButton.addActionListener(e -> toggleAtivoUsuarioSelecionado());
        resetPasswordButton.addActionListener(e -> resetarSenhaUsuarioSelecionado());

        // Carregar dados iniciais
        carregarUsuarios();

        //setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void carregarUsuarios() {
        try {
            List<Usuario> usuarios = usuarioDAO.listarTodos();
            atualizarTabela(usuarios);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar usuários: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void atualizarTabela(List<Usuario> usuarios) {
        tableModel.setRowCount(0); // Limpa tabela
        for (Usuario u : usuarios) {
            tableModel.addRow(new Object[]{
                    u.getIdUsuario(),
                    u.getEmail(),
                    u.isAdmin(),
                    u.isActive()
            });
        }
    }

    private void selecionarUsuario() {
        int selectedRow = usuarioTable.getSelectedRow();
        if (selectedRow >= 0) {
            idField.setText(tableModel.getValueAt(selectedRow, 0).toString());
            emailField.setText(tableModel.getValueAt(selectedRow, 1).toString());
            adminCheckBox.setSelected((Boolean) tableModel.getValueAt(selectedRow, 2));
            ativoCheckBox.setSelected((Boolean) tableModel.getValueAt(selectedRow, 3));

            habilitarFormulario(false);
            passwordField.setText("");
            passwordField.setEnabled(false);
            resetPasswordButton.setEnabled(true);
            editButton.setEnabled(true);
            toggleActiveButton.setEnabled(true);
            cancelButton.setEnabled(false);
            saveButton.setEnabled(false);
            addButton.setEnabled(true);
        } else {
            limparFormulario();
            configurarEstadoInicialBotoes();
        }
    }

    private void adicionarNovoUsuario() {
        editando = false;
        limparFormulario();
        habilitarFormulario(true);
        passwordField.setEnabled(true);
        ativoCheckBox.setSelected(true); // Novo usuário começa ativo
        ativoCheckBox.setEnabled(false); // Status não editável na criação
        resetPasswordButton.setEnabled(false);
        emailField.requestFocus();

        addButton.setEnabled(false);
        editButton.setEnabled(false);
        saveButton.setEnabled(true);
        cancelButton.setEnabled(true);
        toggleActiveButton.setEnabled(false);
        usuarioTable.clearSelection();
    }

    private void editarUsuarioSelecionado() {
        if (usuarioTable.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um usuário na tabela para editar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        editando = true;
        habilitarFormulario(true);
        passwordField.setText("");
        passwordField.setEnabled(false); // Senha só muda via reset
        ativoCheckBox.setEnabled(false); // Status muda pelo botão Ativar/Inativar
        resetPasswordButton.setEnabled(true);
        emailField.requestFocus();

        addButton.setEnabled(false);
        editButton.setEnabled(false);
        saveButton.setEnabled(true);
        cancelButton.setEnabled(true);
        toggleActiveButton.setEnabled(false);
    }

    private void salvarUsuario() {
        // Validações
        String email = emailField.getText().trim();
        if (email.isEmpty() || !email.toLowerCase().endsWith("@uepa.br")) {
            JOptionPane.showMessageDialog(this, "E-mail inválido. Deve ser um endereço @uepa.br.", "Erro de Validação", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String password = new String(passwordField.getPassword());
        if (!editando && password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "A senha é obrigatória ao criar um novo usuário.", "Erro de Validação", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setAdmin(adminCheckBox.isSelected());
        usuario.setActive(ativoCheckBox.isSelected()); // Pega status atual

        boolean sucesso = false;
        try {
            if (editando) {
                usuario.setIdUsuario(Integer.parseInt(idField.getText()));
                sucesso = usuarioDAO.atualizarUsuario(usuario);
            } else {
                // Cria o usuário com a senha fornecida
                sucesso = usuarioDAO.criarUsuario(usuario, password);
            }

            if (sucesso) {
                JOptionPane.showMessageDialog(this, "Usuário " + (editando ? "atualizado" : "cadastrado") + " com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                limparFormulario();
                habilitarFormulario(false);
                configurarEstadoInicialBotoes();
                carregarUsuarios(); // Recarrega a tabela
            } else {
                // Mensagem de erro específica (ex: email duplicado) pode ter sido logada pelo DAO
                JOptionPane.showMessageDialog(this, "Erro ao salvar o usuário. Verifique o console/logs.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar usuário: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void cancelarEdicao() {
        limparFormulario();
        habilitarFormulario(false);
        configurarEstadoInicialBotoes();
        usuarioTable.clearSelection();
    }

    private void toggleAtivoUsuarioSelecionado() {
        int selectedRow = usuarioTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um usuário na tabela para ativar/inativar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        boolean statusAtual = (boolean) tableModel.getValueAt(selectedRow, 3);
        String emailUsuario = tableModel.getValueAt(selectedRow, 1).toString();

        int confirm = JOptionPane.showConfirmDialog(this,
                "Deseja " + (statusAtual ? "INATIVAR" : "ATIVAR") + " o usuário \'" + emailUsuario + "\'",
                "Confirmar Alteração de Status",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean sucesso = usuarioDAO.definirStatusAtivo(id, !statusAtual);
                if (sucesso) {
                    JOptionPane.showMessageDialog(this, "Status do usuário alterado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    carregarUsuarios(); // Recarrega para mostrar o novo status
                    limparFormulario();
                    configurarEstadoInicialBotoes();
                } else {
                     JOptionPane.showMessageDialog(this, "Erro ao alterar status do usuário.", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                 JOptionPane.showMessageDialog(this, "Erro ao alterar status: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void resetarSenhaUsuarioSelecionado() {
         int selectedRow = usuarioTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um usuário na tabela para resetar a senha.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int id = (int) tableModel.getValueAt(selectedRow, 0);
        String emailUsuario = tableModel.getValueAt(selectedRow, 1).toString();
        
        // Solicita a nova senha
        JPasswordField novaSenhaField = new JPasswordField(20);
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Digite a nova senha para " + emailUsuario + ":"));
        panel.add(novaSenhaField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Resetar Senha", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String novaSenha = new String(novaSenhaField.getPassword());
            if (novaSenha.isEmpty()) {
                 JOptionPane.showMessageDialog(this, "A nova senha não pode estar vazia.", "Erro", JOptionPane.ERROR_MESSAGE);
                 return;
            }
            
            try {
                boolean sucesso = usuarioDAO.atualizarSenha(id, novaSenha);
                 if (sucesso) {
                    JOptionPane.showMessageDialog(this, "Senha do usuário " + emailUsuario + " resetada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                } else {
                     JOptionPane.showMessageDialog(this, "Erro ao resetar a senha.", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                 JOptionPane.showMessageDialog(this, "Erro ao resetar senha: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void habilitarFormulario(boolean habilitar) {
        emailField.setEditable(habilitar);
        adminCheckBox.setEnabled(habilitar);
        // ativoCheckBox não é editável diretamente
        // passwordField é habilitado separadamente
    }

    private void limparFormulario() {
        idField.setText("");
        emailField.setText("");
        passwordField.setText("");
        adminCheckBox.setSelected(false);
        ativoCheckBox.setSelected(false);
        editando = false;
    }

    private void configurarEstadoInicialBotoes() {
        addButton.setEnabled(true);
        editButton.setEnabled(false);
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        toggleActiveButton.setEnabled(false);
        resetPasswordButton.setEnabled(false);
        passwordField.setEnabled(false);
    }
}

