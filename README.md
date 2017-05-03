# DecleX

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-DecleX-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/5504)
[![Join the chat at https://gitter.im/declex/Lobby](https://badges.gitter.im/declex/Lobby.svg)](https://gitter.im/declex/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![DecleX](https://raw.githubusercontent.com/wiki/smaugho/declex/img/Declex144.png)](https://github.com/smaugho/declex/wiki) 

DecleX is a framework that aims to get closer to the idea of a fully Declarative Language for Android Development. 
It is totally based on <a href="https://github.com/excilys/androidannotations/wiki" target="_blank">AndroidAnnotations</a>; in a similar fashion, we want to facilitate the writing and the maintenance of 
Android applications to the highest level which has not been achieved up to now.


## Documentation

Please consult our Wiki in https://github.com/smaugho/declex/wiki

## Configuration

Place the following lines in your app module build.gradle file:

```gradle

dependencies {
    annotationProcessor 'com.dspot:declex:1.3'
    compile 'com.dspot:declex-api:1.3'
}

```

And that's it!...

>If you need to use the configuration for the old version of Android Application Plugin with 'android-apt' you can check it here: [Old Android Application Plugin Configuration](android_application_old.md)

## Examples

See our example projects in [Examples](https://github.com/smaugho/declex/wiki/Examples)

## Articles

 * About Populating and Recollecting in the framework: [Dependency Injection Into Views in Android with DecleX](https://medium.com/@smaugho/dependency-injection-into-views-in-android-with-declex-5e7b6537c3a2)
 * Article written by one of our users in Medium, really nice as a quick introduction to the framework: [The Ultimate Android Development Framework](https://android.jlelse.eu/the-ultimate-android-development-framework-f4382677e0c6#.hgzs2jiqs)

-----------
Sponsored by DSpot Sp. z o.o. Contact us at info@dspot.com.pl
