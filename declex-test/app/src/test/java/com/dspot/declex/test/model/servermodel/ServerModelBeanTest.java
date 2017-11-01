package com.dspot.declex.test.model.servermodel;

import static org.junit.Assert.*;
import static com.jayway.awaitility.Awaitility.await;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.hamcrest.Matchers.equalTo;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*", "javax.net.ssl.*"})
@PrepareForTest({ServerModelBean_.class})
public class ServerModelBeanTest {

    private ServerModelBean_ bean;
    private AtomicBoolean executePosts;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() {
        bean =  ServerModelBean_.getInstance_(RuntimeEnvironment.application);
        executePosts= new AtomicBoolean(false);
    }

    private void waitRestService() {
        await().atMost(500, TimeUnit.MILLISECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return executePosts.get();
            }
        });
    }
    
    private Callable<Boolean> waitService() {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return executePosts.get();
            }
        };
    }

    @Test
    public void testDownloadListPosts()  {
        {
            bean.downloadListPosts(new Runnable() {
                @Override
                public void run() {
                    // Done
                    executePosts.set(true);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    // Failed
                    executePosts.set(false);
                }
            });
        }

        waitRestService();
        assertTrue(executePosts.get());
    }

    @Test
    public void testDownloadEnhancedListPosts() {
        {
            bean.downloadEnhancedListPosts(new Runnable() {
                @Override
                public void run() {
                    // Done
                    executePosts.set(true);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    // Failed
                    executePosts.set(false);
                }
            });
        }

        waitRestService();
        assertTrue(executePosts.get());
    }

    @Test
    public void testDownloadReadPosts() {
        {
            bean.downloadReadPosts(new Runnable() {
                @Override
                public void run() {
                    executePosts.set(true);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    executePosts.set(false);
                }
            });

            await().atMost(500, TimeUnit.MILLISECONDS).untilAtomic(executePosts, equalTo(false));
        }
    }
}