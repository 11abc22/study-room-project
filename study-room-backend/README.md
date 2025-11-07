# study-room-backend

## Java 17 build/run note on macOS

This project targets **Java 17** (`pom.xml` already declares `java.version=17`).
If your Mac also has a newer system JDK such as Java 24, Maven Wrapper may still pick that newer JDK by default, which can trigger Lombok/javac compatibility errors during compilation.

On this machine, Homebrew Java 17 is available at:

```bash
/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

### Recommended commands

Compile:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
./mvnw -q -DskipTests compile
```

Run tests:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
./mvnw test
```

Start backend:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
./mvnw spring-boot:run
```

### Shortcut script on this Mac

A helper script is included so you don't need to type `JAVA_HOME=...` every time:

```bash
./run-java17.sh version
./run-java17.sh compile
./run-java17.sh test
./run-java17.sh run
```

It forces Maven Wrapper to use Homebrew Java 17 before running.

### Optional shell helper

If you often work on this backend, you can still enter the project directory and run:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH
```

Then verify with:

```bash
./mvnw -v
```

You should see `Java version: 17.x` before compiling or running the project.
