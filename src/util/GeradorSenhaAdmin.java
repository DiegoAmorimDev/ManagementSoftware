/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package util;

/**
 *
 * @author diego-amorim
 */
import util.PasswordUtil;

public class GeradorSenhaAdmin {
    public static void main(String[] args) {
        
        String senhaAdmin = "admin123"; // 
        

        
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(senhaAdmin, salt);

        
        System.out.println("--- Use estes valores no seu comando INSERT SQL ---");
        System.out.println("Email: admin@uepa.br");
        System.out.println("Senha Hash (para senha_hash): " + hash);
        System.out.println("Salt (para salt): " + salt);
        System.out.println("is_admin: TRUE");
        System.out.println("is_active: TRUE");
        System.out.println("\nComando SQL pronto para copiar e colar (substitua a senha se necessário):");
        // Linha corrigida abaixo:
        System.out.println("INSERT INTO usuarios (email, senha_hash, salt, is_admin, is_active) VALUES ('admin@uepa.br', '" + hash + "', '" + salt + "', TRUE, TRUE);");
    }
}


