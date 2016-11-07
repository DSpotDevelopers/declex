# DecleX

DecleX is a framework that aims to get closer to the idea of a fully Declarative Language for Android Development. 
It is totally based on <a href="https://github.com/excilys/androidannotations/wiki" target="_blank">AndroidAnnotations</a>; in a similar fashion, we want to facilitate the writing and the maintenance of 
Android applications to the highest level which has not been achieved up to now.


## Documentation

Please consult our Wiki in https://github.com/smaugho/declex/wiki

## Configuration

* Place the following lines in your project build.graddle, in the repositories section of all projects.

```gradle
    maven {
            url 'https://dspot.bintray.com/declex'
    }
````

The section will look like this:

```gradle
allprojects {
    repositories {
        maven {
            url 'https://dspot.bintray.com/declex'
        }
        jcenter()
    }
}
```

* Place the following lines in your app module build.graddle file:

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
    compile 'com.dspot:declex-api:1.0.1'
    apt 'com.dspot:declex:1.0.1'
}

```


And that's it!...

-----------
Sponsored by DSpot Sp. z o.o. Contact us at info@dspot.com.pl
