# Gradle Wrapper bootstrap

This project includes the standard Gradle wrapper scripts plus a small bootstrap source file.
If `gradle-wrapper.jar` is absent, `gradlew`/`gradlew.bat` run `WrapperDownloader.java` using Java 11+.
The bootstrap downloads the official Gradle 8.2 wrapper JAR from services.gradle.org and verifies its SHA-256 before installing it.

Official wrapper SHA-256:
`a8451eeda314d0568b5340498b36edf147a8f0d692c5ff58082d477abe9146e4`

After the first successful bootstrap, the normal Gradle wrapper flow is used.
