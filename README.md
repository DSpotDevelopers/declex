# DecleX

DecleX is a framework that aims to get closer to the idea of a fully Declarative Language for Android Development. 
It is totally based on <a href="https://github.com/excilys/androidannotations/wiki" target="_blank">AndroidAnnotations</a>; in a similar fashion, we want to facilitate the writing and the maintenance of 
Android applications to the highest level which has not been achieved up to now.


## Documentation

Please consult our Wiki in https://github.com/smaugho/declex/wiki

## Configuration

Place the following lines in your app module build.gradle file:

```gradle

dependencies {
    annotationProcessor 'com.dspot:declex:1.2.1'
    compile 'com.dspot:declex-api:1.2.1'
}

```

And that's it!...

>If you need to use the configuration for the old version of Android Application Plugin with 'android-apt' you can check it here: [Old Android Application Plugin Configuration](android_application_old.md)

## Examples

See our example projects in [Examples](https://github.com/smaugho/declex/wiki/Examples)

## Articles

 * Article written by one of our users in Medium, really nice as a quick introduction to the framework: [The Ultimate Android Development Framework](https://android.jlelse.eu/the-ultimate-android-development-framework-f4382677e0c6#.hgzs2jiqs)
 * About Populating and Recollecting in the framework: [Dependency Injection Into Views in Android with DecleX](https://medium.com/@smaugho/dependency-injection-into-views-in-android-with-declex-5e7b6537c3a2#.59mbry4bv)

-----------
Sponsored by DSpot Sp. z o.o. Contact us at info@dspot.com.pl
