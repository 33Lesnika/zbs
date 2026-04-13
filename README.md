# ZBS — Zero Build System for Java

## Why this exists
I got tired of build tools that break across environments or require learning yet another DSL. ZBS lets you write build
scripts in plain Java, using the same language you already know. Vendor the whole thing in your project - it's just one
file - so it works everywhere there's a JDK. Bonus point for making your project future-proof against the next big build
tool trend.

## Features
- Build scripts in Java, not some yet-another-DSL in god-knows-what language
- Incremental compilation based on file timestamps (duh)
- Zero external dependencies
- Everything in a single file you can modify (because you vendor it)

## Usage
1. Drop `ZBS.java` somewhere into your project (e.g., in `src/` or even in the root project directory).
2. Write a build script like `build.java`:

```java
void main(String... args) throws Exception {
    ZBS.acceptArgs(args);
    ZBS.compile("Hello.java");
    ZBS.run("Hello");
}
```

3. Run it:
```bash
java build.java
```

`ZBS.acceptArgs(args)` handles flags like `clean` and `run`, just like you'd expect:
```bash
java build.java clean run
```

That's it. No setup, no config files, no plugins. If you need more, just edit `ZBS.java` - it's yours now.

