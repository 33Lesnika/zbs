void main(String ... args) throws IOException, InterruptedException {
    ZBS.acceptArgs(args);
    ZBS.version();
//    ZBS.classpath("../lib");
    ZBS.compile("ZBS.java");
//    ZBS.run("Hello");
}
