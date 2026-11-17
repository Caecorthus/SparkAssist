# Wathe Development Dependency

SparkAssist compiles against the Spark Wathe fork version
`1.5.6-spark-1.21.1`. The development jar is tracked here, as it is in the
other SparkDLC repositories:

```text
libs/wathe-1.5.6-spark-1.21.1.jar
```

`verifyWatheDependency` checks its SHA-256 against the value pinned in
`build.gradle`. To move to another Wathe build, replace the jar and update
`wathe_version` in `gradle.properties` together with `supportedWatheVersion`
and `supportedWatheSha256` in `build.gradle`.

The jar is built from the matching
[TrainMurderMystery source](https://github.com/XruiDD/TrainMurderMystery).
