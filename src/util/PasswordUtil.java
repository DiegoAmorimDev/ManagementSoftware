package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {

    private static final int SALT_LENGTH = 16; // Comprimento do salt em bytes
    private static final String HASH_ALGORITHM = "SHA-256"; // Algoritmo de hash (RNF03)

    // Gera um salt aleatório
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // Gera o hash da senha usando o salt fornecido
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            // Combina salt e senha antes de hashear
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            // Em um cenário real, logar o erro e talvez lançar uma exceção customizada
            System.err.println("Erro: Algoritmo de hash não encontrado: " + HASH_ALGORITHM);
            throw new RuntimeException("Erro interno ao gerar hash da senha.", e);
        }
    }

    // Verifica se a senha fornecida corresponde ao hash armazenado, usando o salt
    public static boolean verifyPassword(String providedPassword, String storedHash, String salt) {
        String newHash = hashPassword(providedPassword, salt);
        return newHash.equals(storedHash);
    }

    // Exemplo de uso (pode ser removido ou comentado)
    /*
    public static void main(String[] args) {
        String password = "senha123";
        String salt = generateSalt();
        String hash = hashPassword(password, salt);

        System.out.println("Senha Original: " + password);
        System.out.println("Salt Gerado (Base64): " + salt);
        System.out.println("Hash Gerado (Base64): " + hash);

        // Verificação
        boolean match = verifyPassword("senha123", hash, salt);
        System.out.println("Senha corresponde? " + match);

        boolean mismatch = verifyPassword("senhaErrada", hash, salt);
        System.out.println("Senha errada corresponde? " + mismatch);
    }
    */
}

