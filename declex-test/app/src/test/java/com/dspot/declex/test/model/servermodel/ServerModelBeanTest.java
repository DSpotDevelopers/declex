package com.dspot.declex.test.model.servermodel;

import static org.junit.Assert.*;
import static com.jayway.awaitility.Awaitility.await;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.TimeUnit;


@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*", "javax.net.ssl.*"})
@PrepareForTest({ServerModelBean_.class})
public class ServerModelBeanTest {

    private ServerModelBean_ bean;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() {
        bean =  ServerModelBean_.getInstance_(RuntimeEnvironment.application);
    }

    @Test
    public void testDownloadListPosts() throws Exception {
        {
            bean.downloadListPosts();
            await().atMost(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public  void testDownloadEnhancedListPosts() {
        {
            bean.downloadEnhancedListPosts();
            await().atMost(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testDownloadReadPosts() {
        {
            bean.downloadReadPosts();
            await().atMost(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testDownloadEnhancedReadPost() {
        {
            bean.downloadEnhancedReadPost();
            await().atMost(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testPostsRequest() {
        {
            bean.createPost();
            await().atMost(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testPutRequestNotParameters() {
        {
            bean.updatePost(false);
            await().atMost(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testPutRequestWithParameters() {
        {
            bean.updatePost(true);
            await().atMost(5, TimeUnit.SECONDS);
        }
    }
}