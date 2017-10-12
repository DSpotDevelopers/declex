package com.dspot.declex.test.model.servermodel;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.OkHttpClient;

/**
 * Here are the configurations use all throughout the program.
 */
public class Config {
    /**
     * The constant SERVER.
     */
    public static final String SERVER = "http://jsonplaceholder.typicode.com/";

    /**
     * Default OkHttpClient to use in whole the app
     * from here it can be controlled parameters as timeouts, SSL and more
     * related to the connection with the Back-End
     */
    public final static OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
        .hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        })
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build();
}