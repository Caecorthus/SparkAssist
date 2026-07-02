# Building SparkAssist

## 中文

请从 SparkAssist 的源码根目录构建。这个目录里应该能看到：

```text
build.gradle
settings.gradle
gradle.properties
gradlew
gradlew.bat
src/
```

Windows:

```bat
gradlew.bat clean build
```

macOS / Linux:

```sh
./gradlew clean build
```

项目可以放在任意父级路径下，源码根目录本身也可以改名；但不要从缺少这些文件的外层目录直接运行 `gradle build`。如果外层目录有自己的 `settings.gradle`，那外层必须真的包含它声明的子项目目录。

## English

Build from the SparkAssist source root. Use the checked-in Gradle Wrapper:

```sh
./gradlew clean build
```

On Windows:

```bat
gradlew.bat clean build
```

The project may live under any parent path, and the source root folder may be renamed. Do not run `gradle build` from an outer folder unless that outer folder is itself a valid Gradle build.
