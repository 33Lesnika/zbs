import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public final class ZBS {
    public static final String VERSION = "1.0.0";
    
    public static OutputStream os = System.out;
    public static OutputStream es = System.err;
    public static LogLevel logLevel = LogLevel.INFO;
    public static String configFilePath = "zbs.properties";
    private static Properties configProperties = null;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static String toolchainDir = initToolchainDir();
    private static String classpath = "";

    private static String initToolchainDir() {
        ConfigValue zbsToolchain = readConfig("ZBS_TOOLCHAIN");
        if (zbsToolchain instanceof ConfigValue.OK(String value, _)) {
            Path path = Path.of(value);
            if (Files.isDirectory(path)) {
                return path + File.separator;
            }
        }

        ConfigValue javaHome = readConfig("JAVA_HOME");
        if (javaHome instanceof ConfigValue.OK(String value, _)) {
            Path binPath = Path.of(value).resolve("bin");
            if (Files.isDirectory(binPath)) {
                return binPath + File.separator;
            }
        }

        ConfigValue userHome = readConfig("user.home");
        if (userHome instanceof ConfigValue.OK(String value, _)) {
            Path toolchain = Path.of(value)
                    .resolve(".jdks")
                    .resolve("openjdk-25.0.1")
                    .resolve("bin");
            return toolchain + File.separator;
        }

        return "";
    }


    private ZBS() {
        throw new Error("no instances");
    }

    public static void version() throws IOException, InterruptedException {
        Process java = Runtime.getRuntime().exec(new String[]{toolchainDir + "java", "--version"});
        var threads = startRedirectThreads(java);
        int exitCode = java.waitFor();
        for (var thread : threads) {
            thread.join();
        }
        if (exitCode != 0) {
            throw new IOException("Version check failed with exit code " + exitCode);
        }
    }

    // TODO: add support for multiple source files; use Java Compiler API
    public static void compile(String sourceFile) throws IOException, InterruptedException {
        if (!shouldCompile(sourceFile)) {
            log("Skipping compilation for " + sourceFile + " as it is up to date.");
            return;
        }
        String[] cmdarray = getCompileArgs(sourceFile);
        Process javac = Runtime.getRuntime().exec(cmdarray);
        var threads = startRedirectThreads(javac);
        int exitCode = javac.waitFor();
        for (var thread : threads) {
            thread.join();
        }
        if (exitCode != 0) {
            throw new IOException("Compilation failed with exit code " + exitCode);
        }
    }

    private static String[] getCompileArgs(String sourceFile) {
        List<String> cmd = new ArrayList<>();
        cmd.add(toolchainDir + "javac");
        if (!classpath.isEmpty()) {
            cmd.add("-cp");
            cmd.add(classpath);
        }
        cmd.add(sourceFile);
        return cmd.toArray(new String[0]);
    }

    public static void mavenProject() throws IOException, InterruptedException {
        mavenProject(".");
    }

    public static void mavenProject(String projectDir) throws IOException, InterruptedException {
        Path baseDir = projectDir.equals(".") ? Path.of(".") : Path.of(projectDir);
        Path srcDir = baseDir.resolve("src/main/java");
        Path targetDir = baseDir.resolve("target/classes");
        Files.createDirectories(targetDir);

        List<String> sourceFiles = findMavenSources(srcDir);

        if (sourceFiles.isEmpty()) {
            log("No source files found in " + srcDir);
            return;
        }
        compileSources(sourceFiles, targetDir.toString());
        classpath(targetDir.toString());
    }

    private static List<String> findMavenSources(Path srcDir) throws IOException {
        List<String> sourceFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(srcDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(p -> sourceFiles.add(p.toString()));
        }
        return sourceFiles;
    }

    private static void compileSources(List<String> sourceFiles, String outputDir) throws IOException, InterruptedException {
        String[] cmdarray = getMavenCompileArgs(sourceFiles, outputDir);
        Process javac = Runtime.getRuntime().exec(cmdarray);
        var threads = startRedirectThreads(javac);
        int exitCode = javac.waitFor();
        for (var thread : threads) {
            thread.join();
        }
        if (exitCode != 0) {
            throw new IOException("Maven project compilation failed with exit code " + exitCode);
        }
    }

    private static String[] getMavenCompileArgs(List<String> sourceFiles, String outputDir) {
        List<String> cmd = new ArrayList<>();
        cmd.add(toolchainDir + "javac");
        cmd.add("-d");
        cmd.add(outputDir);
        if (!classpath.isEmpty()) {
            cmd.add("-cp");
            cmd.add(classpath);
        }
        cmd.addAll(sourceFiles);
        return cmd.toArray(new String[0]);
    }

    public static void run(String cmd) throws IOException, InterruptedException {
        log("=".repeat(20) + " Running: " + cmd + " " + "=".repeat(20));
        Process java = Runtime.getRuntime().exec(getRunArgs(cmd));
        var threads = startRedirectThreads(java);
        int exitCode = java.waitFor();
        for (var thread : threads) {
            thread.join();
        }
        if (exitCode != 0) {
            throw new IOException("Execution failed with exit code " + exitCode);
        }
    }

    public static String[] getRunArgs(String sourceFile) {
        List<String> cmd = new ArrayList<>();
        cmd.add(toolchainDir + "java");
        if (!classpath.isEmpty()) {
            cmd.add("-cp");
            cmd.add(classpath);
        }
        cmd.add(sourceFile);
        return cmd.toArray(new String[0]);
    }

    private static boolean shouldCompile(String sourceFile) {
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
            log(e.getMessage());
        }
        return true;
    }

    public static void exec(String... cmd) throws IOException, InterruptedException {
        log("=".repeat(20));
        Process java = Runtime.getRuntime().exec(cmd);
        var threads = startRedirectThreads(java);
        int exitCode = java.waitFor();
        for (var thread : threads) {
            thread.join();
        }
        if (exitCode != 0) {
            throw new IOException("Execution failed with exit code " + exitCode);
        }
    }

    public static void acceptArgs(String... args) {
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

    /**
     * Copy InputStream to OutputStream until EOF. Used for redirecting process output to console.
     * Do not use to redirect large files or streams of data because there is no buffering.
     */
    public static void redirect(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        os.flush();
    }

    private static List<Thread> startRedirectThreads(Process p) {
        var stdoutThread = Thread.ofVirtual().start(() -> {
            try {
                redirect(p.getInputStream(), os);
            } catch (IOException e) {
                log("Error redirecting stdout: " + e.getMessage(), LogLevel.ERROR);
            }
        });
        var stderrThread = Thread.ofVirtual().start(() -> {
            try {
                redirect(p.getErrorStream(), es);
            } catch (IOException e) {
                log("Error redirecting stderr: " + e.getMessage(), LogLevel.ERROR);
            }
        });
        return List.of(stdoutThread, stderrThread);
    }

    public static void classpath(String path) {
        Path cp = Path.of(path).normalize();
        if (Files.isDirectory(cp)) {
            String newPath = cp.toAbsolutePath() + File.separator + "*";
            if (classpath.isEmpty()) {
                classpath = newPath;
            } else {
                classpath = String.join(File.pathSeparator, classpath, newPath);
            }
        }
    }

    /**
     * Loads configuration properties from the configured file.
     * Called automatically on first readConfig() call, can be invoked explicitly to reload.
     */
    public static void loadConfigProperties() {
        configProperties = new Properties();
        if (configFilePath == null || configFilePath.isEmpty()) {
            return;
        }
        try {
            Path configPath = Path.of(configFilePath);
            if (Files.exists(configPath)) {
                try (InputStream input = Files.newInputStream(configPath)) {
                    configProperties.load(input);
                    log("Loaded configuration from: " + configFilePath, LogLevel.INFO);
                }
            }
        } catch (IOException e) {
            log("Error reading config file: " + configFilePath + " - " + e.getMessage(), LogLevel.WARN);
        }
    }

    /**
     * Reads a config value from environment, config file, or system properties.
     * Precedence: environment > config file > system property.
     * Properties cached after first load.
     *
     * @param key configuration key
     * @return ConfigValue (either Ok or Empty)
     */
    public static ConfigValue readConfig(String key) {
        if (configProperties == null) {
            loadConfigProperties();
        }

        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isEmpty()) {
            return ConfigValue.ok(envValue, ConfigSource.ENV);
        }

        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isEmpty()) {
            return ConfigValue.ok(sysProp, ConfigSource.ENV);
        }

        if (configProperties != null) {
            String fileValue = configProperties.getProperty(key);
            if (fileValue != null && !fileValue.isEmpty()) {
                return ConfigValue.ok(fileValue, ConfigSource.FILE);
            }
        }

        return ConfigValue.empty();
    }

    public enum ConfigSource {
        ENV("Environment Variable"),
        FILE("Configuration File");

        private final String description;

        ConfigSource(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

    }

    public sealed interface ConfigValue {

        record OK(String value, ConfigSource source) implements ConfigValue {}
        record Empty() implements ConfigValue {}
        static OK ok(String value, ConfigSource source) {
            return new OK(value, source);
        }

        static Empty empty() {
            return new Empty();
        }

    }

    public static void log(String message) {
        log(message, LogLevel.INFO);
    }

    public static void log(String message, LogLevel level) {
        if (level.ordinal() < logLevel.ordinal()) {
            return;
        }
        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String levelStr = String.format("[%-5s]", level);
            String formattedMessage = String.format("%s %s %s\n", timestamp, levelStr, message);
            os.write(formattedMessage.getBytes());
            os.flush();
        } catch (IOException _) {
        }
    }

    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
