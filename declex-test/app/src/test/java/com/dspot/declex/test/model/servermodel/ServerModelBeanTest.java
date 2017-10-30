package com.dspot.declex.test.model.servermodel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;


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
    public void testDownloadListPosts() {
        {
            bean.downloadListPosts();
            assertNotNull(bean.getListPosts());
        }
    }

    @Test
    public  void testDownloadEnhancedListPosts() {
        {
            bean.downloadEnhancedListPosts();
            assertNotNull(bean.getEnhancedListPosts());
        }
    }

    @Test
    public void testDownloadReadPosts() {
        {
            bean.downloadReadPosts();
            assertNotNull(bean.getReadPost());
        }
    }

    @Test
    public void testDownloadEnhancedReadPost() {
        {
            bean.downloadEnhancedReadPost();
            assertNotNull(bean.getEnhancedReadPost());
        }
    }

    @Test
    public void testPostsRequest() {
        {
            bean.createPost();
            assertNotNull(bean.getListPosts());
        }
    }

    @Test
    public void testPutRequestNotParameters() {
        {
            bean.updatePost(false);
            assertNotNull(bean.getListPosts());
        }
    }

    @Test
    public void testPutRequestWithParameters() {
        {
            bean.updatePost(true);
            assertNotNull(bean.getListPosts());
        }
    }
}