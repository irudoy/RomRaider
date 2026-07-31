# Building RomRaider with Visual Studio Code

## Requirements

Install the following tools:

- [Git](https://git-scm.com/downloads)
- An OpenJDK 17 distribution such as [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=17)
- [Apache Ant 1.10.x](https://ant.apache.org/bindownload.cgi), including its optional tasks
- [Visual Studio Code](https://code.visualstudio.com/)
- The [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)

Set `JAVA_HOME` to the JDK 17 installation. Set `ANT_HOME` to the Ant
installation, then add `%JAVA_HOME%\bin` and `%ANT_HOME%\bin` to `PATH` on
Windows, or the corresponding `bin` directories on Linux and macOS.

Verify that Java and the compiler both use version 17:

```text
java -version
javac -version
ant -version
```

## Build and test

Open the repository folder in Visual Studio Code, then run this command in the
integrated terminal:

```text
ant all
```

This command performs a clean build, runs the tests, and writes the Windows
and Linux packages below `build/dist`. To run only the tests, use:

```text
ant unittest
```

The optional XDF integration test requires a local directory of XDF files:

```text
ant -Dromraider.test.xdfDir=/path/to/xdf-definitions unittest
```

## Debug configuration

Open the Run and Debug panel and create `.vscode/launch.json`. Use these two
launch configurations for the editor and logger:

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "RomRaider Editor",
            "request": "launch",
            "mainClass": "com.romraider.ECUExec",
            "projectName": "romraider"
        },
        {
            "type": "java",
            "name": "RomRaider Logger",
            "request": "launch",
            "mainClass": "com.romraider.logger.ecu.EcuLoggerExec",
            "projectName": "romraider"
        }
    ]
}
```

Run `ant all` after source changes before starting either configuration. If
Visual Studio Code reports unresolved generated sources, reload the Java
projects after the Ant build completes.
