void main(String ... args) throws IOException, InterruptedException {
    IO.println(System.getProperty("user.home"));
    ZBS.acceptArgs(args);
    ZBS.version();
//    ZBS.classpath("../lib");
    ZBS.compile("ZBS.java");
//    ZBS.run("Hello");
}
