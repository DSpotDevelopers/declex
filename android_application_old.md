 
## Old Android Application Plugin Configuration

If you are developping with a previous version of Android Application Plugin, and you should use 'android-apt' plugin, this is the correct configuration:

```gradle

apply plugin: 'android-apt'

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'
    }
}

apt {
    arguments {
        // you should set your package name here if you are using different application IDs
        // resourcePackageName "YOUR PACKAGE NAME HERE"

        // You can set optional annotation processing options here, like these commented options:
        // logLevel 'INFO'
    }
}

dependencies {
    compile 'com.dspot:declex-api:1.2.1'
    apt 'com.dspot:declex:1.2.1'
}

```
