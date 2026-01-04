import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class ZBS {
    public static OutputStream os = System.out;
    public static OutputStream es = System.err;
    public static String toolchainDir = System.getProperty("user.home") + "\\.jdks\\openjdk-25.0.1\\bin\\";
    public static Optional<String> classpath = Optional.empty();


    private ZBS() {
        throw new Error("no instances");
    }

    public static void version() throws IOException, InterruptedException {
        Process java = Runtime.getRuntime().exec(new String[]{toolchainDir + "java", "--version"});
        redirect(java.getInputStream(), os);
        redirect(java.getErrorStream(), es);
        int exitCode = java.waitFor();
        if (exitCode != 0) {
            throw new IOException("Version check failed with exit code " + exitCode);
        }
    }

    public static void compile(String sourceFile) throws IOException, InterruptedException {
        if (!shouldCompile(sourceFile)) {
            log("Skipping compilation for " + sourceFile + " as it is up to date.");
            return;
        }
        String[] cmdarray = getCompileArgs(sourceFile);
        Process javac = Runtime.getRuntime().exec(cmdarray);
        redirect(javac.getInputStream(), os);
        redirect(javac.getErrorStream(), es);
        int exitCode = javac.waitFor();
        if (exitCode != 0) {
            throw new IOException("Compilation failed with exit code " + exitCode);
        }
    }

    private static String[] getCompileArgs(String sourceFile) {
        List<String> cmd = new ArrayList<>();
        cmd.add(toolchainDir + "javac");
        classpath.ifPresent(cp -> {
            cmd.add("-cp");
            cmd.add(cp);
        });
        cmd.add(sourceFile);
//        cmd.forEach(ZBS::log);
        return cmd.toArray(new String[0]);
    }

    public static void run(String cmd) throws IOException, InterruptedException {
        log("=".repeat(20) + " Running: " + cmd + " " + "=".repeat(20));
        Process java = Runtime.getRuntime().exec(getRunArgs(cmd));
        redirect(java.getInputStream(), os);
        redirect(java.getErrorStream(), es);
        int exitCode = java.waitFor();
        if (exitCode != 0) {
            throw new IOException("Execution failed with exit code " + exitCode);
        }
    }

    public static String[] getRunArgs(String sourceFile) {
        List<String> cmd = new ArrayList<>();
        cmd.add(toolchainDir + "java");
        classpath.ifPresent(cp -> {
            cmd.add("-cp");
            cmd.add(cp);
        });
        cmd.add(sourceFile);
//        cmd.forEach(ZBS::log);
        return cmd.toArray(new String[0]);
    }

    private static boolean shouldCompile(String sourceFile) {
        // Simple check: if the .class file is newer than the .java file, skip compilation
        Path javaFilePath = Path.of(sourceFile);
        String classFileName = sourceFile.replace(".java", ".class");
        Path classFilePath = Path.of(classFileName);
        try {
            if (Files.exists(classFilePath)) {
                long javaFileTime = Files.getLastModifiedTime(javaFilePath).toMillis();
                long classFileTime = Files.getLastModifiedTime(classFilePath).toMillis();
                return javaFileTime > classFileTime;
            }
        } catch (IOException e) {
            // TODO: log error
        }
        return true;
    }

    public static void exec(String... cmd) throws IOException, InterruptedException {
        log("=".repeat(20));
        Process java = Runtime.getRuntime().exec(cmd);
        redirect(java.getInputStream(), os);
        redirect(java.getErrorStream(), es);
        int exitCode = java.waitFor();
        if (exitCode != 0) {
            throw new IOException("Execution failed with exit code " + exitCode);
        }
    }

    public static void acceptArgs(String... args) throws IOException, InterruptedException {
        if (args.length == 0) {
            return;
        }
        boolean shouldContinue = false;
        for (String arg : args) {
            if (arg.equals("--help")) {
                log("Usage: java <buildscript.java> [options]");
                log("Options:");
                log("  --help        Show this help message");
                log("  run           Continue execution");
                log("  clean         Clean compiled .class files");
                System.exit(0);
            }
            if (arg.equals("run")) {
                shouldContinue = true;
            }
            if (arg.equals("clean")) {
                // Implement clean logic if needed
                log("Clean command received");
                clean();
            }
        }
        if (!shouldContinue) {
            System.exit(0);
        }
    }

    public static void clean() {
        try (Stream<Path> paths = Files.walk(Path.of("."))) {
            paths
                .filter(path -> path.toString().endsWith(".class"))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        log("Deleted: " + path);
                    } catch (IOException e) {
                        log("Failed to delete: " + path);
                    }
                });
        } catch (IOException e) {
            log("Error during cleaning: " + e.getMessage());
        }
    }

    public static void redirect(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        os.flush();
    }

    public static void log(String message) {
        try {
            os.write((message + "\n").getBytes());
            os.flush();
        } catch (IOException ignored) {
        }
    }

    public static void classpath(String path) {
        Path cp = Path.of(path).normalize();
        if (Files.isDirectory(cp)) {
            classpath = Optional.of(String.join(File.pathSeparator, classpath.orElse(""), cp.toAbsolutePath() + File.separator + "*"));
        }
    }
}
