## kotlin-merkle-tree

This repository contains a generic Merkle-tree implementation of a merkle tree.

## Usage

#### Gradle (groovy)

gradle.properties:

```properties
spaceUsername = gradle-download
spacePassword = githubgradledownload
```
build.gradle:
```groovy

maven {
    url "https://maven.pkg.jetbrains.space/rpanic/p/njord/njord"
    credentials {
        username = spaceUsername
        password = spacePassword
    }
}

dependencies {
    implementation "com.rpanic:kotlin-merkle-tree:0.1.1"
}

```