# brainCloud Java Examples

This repository contains examples projects using the [Java brainCloud client](https://github.com/getbraincloud/braincloud-java).

To run these projects you need a brainCloud app. Connect each one with the **brainCloud Java** plugin for IntelliJ IDEA / Android Studio:

1. Open the project and install the plugin when the IDE offers it.
2. In the **brainCloud** tool window, log in and pick your team and app (or create one).
3. The plugin writes `braincloud.cfg` (gitignored), which the examples read with `init()` / `init(context)`. No app ID or secret in the code.

---

The [Java brainCloud client](https://github.com/getbraincloud/braincloud-java) is published on [Maven Central](https://central.sonatype.com/artifact/com.bitheads/braincloud-java), so projects can easily add it as a dependency:
### Maven Dependency

Add the following to a pom.xml:

```
<dependency>
    <groupId>com.bitheads</groupId>
    <artifactId>braincloud-java</artifactId>
    <version>x.x.x</version>
</dependency>
```

### Gradle Dependency
Add the following to a build.gradle:

```
implementation 'com.bitheads:braincloud-java:x.x.x'
```

For Android projects there is a ```braincloud-java-android``` artifact published on Maven Central containing a separate Android version of the BrainCloudWrapper.java:

https://central.sonatype.com/artifact/com.bitheads/braincloud-java-android/

***NOTE:*** *The examples in this repository are already configured to use the latest version of brainCloud.*
