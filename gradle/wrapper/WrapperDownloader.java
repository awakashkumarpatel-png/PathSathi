import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * Bootstrap for Path Sathi's Gradle wrapper when gradle-wrapper.jar is absent.
 * Downloads the official Gradle 8.2 wrapper JAR and verifies its SHA-256 before installing it.
 */
public final class WrapperDownloader {
    private static final String URL = "https://services.gradle.org/distributions/gradle-8.2-wrapper.jar";
    private static final String SHA256 = "a8451eeda314d0568b5340498b36edf147a8f0d692c5ff58082d477abe9146e4";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java WrapperDownloader.java <project-root>");
            System.exit(2);
        }
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Path jar = root.resolve("gradle/wrapper/gradle-wrapper.jar");
        if (Files.isRegularFile(jar) && Files.size(jar) > 0) return;
        Files.createDirectories(jar.getParent());

        Path tmp = jar.resolveSibling("gradle-wrapper.jar.download");
        try {
            HttpURLConnection c = (HttpURLConnection) URI.create(URL).toURL().openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setInstanceFollowRedirects(true);
            c.setRequestMethod("GET");
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code + " while downloading Gradle wrapper");
            try (InputStream in = c.getInputStream(); OutputStream out = Files.newOutputStream(tmp)) {
                in.transferTo(out);
            } finally { c.disconnect(); }

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(tmp)) {
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
            }
            StringBuilder actual = new StringBuilder();
            for (byte b : md.digest()) actual.append(String.format("%02x", b));
            if (!SHA256.equals(actual.toString())) {
                Files.deleteIfExists(tmp);
                throw new SecurityException("Gradle wrapper checksum mismatch. Expected " + SHA256 + " but received " + actual);
            }
            Files.move(tmp, jar, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            System.out.println("Downloaded and verified official Gradle 8.2 wrapper JAR.");
        } catch (Exception e) {
            Files.deleteIfExists(tmp);
            System.err.println("Could not bootstrap gradle-wrapper.jar: " + e.getMessage());
            System.err.println("Internet access is required only for the first wrapper bootstrap.");
            System.exit(1);
        }
    }
}
