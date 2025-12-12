import java.io.*;
import java.security.*;

public class CreateCAKey {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java CreateCAKey <output-file>");
            return;
        }

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair caKeyPair = keyGen.generateKeyPair();

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(args[0]))) {
            oos.writeObject(caKeyPair.getPrivate());
        }

        System.out.println("CA private key saved to: " + args[0]);
    }
}