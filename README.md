# DecleX

DecleX is a framework that aims to get closer to the idea of a fully Declarative Language for Android Development. 
It is totally based on <a href="https://github.com/excilys/androidannotations/wiki" target="_blank">AndroidAnnotations</a>; in a similar fashion, we want to facilitate the writing and the maintenance of 
Android applications to the highest level which has not been achieved up to now.


## Documentation

Please consult our Wiki in https://github.com/smaugho/declex/wiki

## Configuration

Download the library jar files from [here](https://github.com/smaugho/declex/releases/tag/v1.0):

* Place the core processor declex-v1.0.jar in a folder named compile-libs in your project.

* Place the API file declex-api.jar in a folder named libs in your project.

* Place the following lines in your build.graddle file:

```graddle
apply plugin: 'com.neenbedankt.android-apt'

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.neenbedankt.gradle.plugins:android-apt:1.4'
    }
}

apt {
    arguments {
        androidManifestFile variant.outputs[0].processResources.manifestFile
        resourcePackageName 'YOUR PROJECT PACKAGE'
    }
}

```
You should replace "YOUR PROJECT PACKAGE" by your project package name (Ex. com.company.example).

* And finally add to your dependencies the framework libraries:

```graddle
dependencies {
    compile 'com.dspot:declex-api:1.0'
    apt 'com.dspot:declex:1.0'
    
    ...
```

And that's it!...

-----------
Sponsored by DSpot Sp. z o.o. Contact us at info@dspot.com.pl
