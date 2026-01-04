# zbs — Minimal Java build system

A small collection of utilities (the `ZBS` class) for compiling, running and cleaning Java projects.

## Features
- Uses actual programming language (Java) for build scripts
- Timestamp-based incremental compilation
- No external dependencies (only requires JDK)
- Single file implementation

## How to use
1. Download the `ZBS.java` file from this repository and place it in your project directory (e.g., in a `src` folder).
2. Create a build script (e.g., `build.java`) that uses the `ZBS` class to define build tasks.
Example of a simple build script can be found within this repository or use the following template:
#### **`build.java`**
``` java
void main(String ... args) throws Exception {
    ZBS.acceptArgs(args);
    ZBS.compile("Hello.java");
    ZBS.run("Hello");
}
```
3. Compile and run your build script using the JDK:
``` bash
java build.java
```

Note: `ZBS.acceptArgs(args)` processes command-line arguments to perform tasks like `run` and `clean` like a typical build tool.
``` bash
java build.java clean run
```
