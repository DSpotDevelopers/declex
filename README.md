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
    compile 'com.dspot:declex-api:1.2.1'
    apt 'com.dspot:declex:1.2.1'
}

```

>If you need to use the configuration for the old version of Android Application Plugin with 'android-apt' you can check it in [Old Android Application Plugin Configuration](android_application_old.md)

And that's it!...

-----------
Sponsored by DSpot Sp. z o.o. Contact us at info@dspot.com.pl
