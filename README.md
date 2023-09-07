# brainCloud Java Examples

This repository contains examples projects using the [Java brainCloud client](https://github.com/getbraincloud/braincloud-java).

In order to get these projects up and running, you will need to create an app via the [brainCloud portal](https://portalx.braincloudservers.com/). The ***App ID*** and ***App Secret*** associated with the created app will be needed to initialize the brainCloud client SDK: `_bc.initialize(appId, secret, "1.0.0", serverUrl);`

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
