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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*", "javax.net.ssl.*"})
@PrepareForTest({ServerModelBean_.class})
public class ServerModelBeanTest {

    private ServerModelBean_ bean;
    private CountDownLatch lock = new CountDownLatch(1);
    private PrintStream outPrintStream;
    private final ByteArrayOutputStream outContentStream = new ByteArrayOutputStream();

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() {
        bean =  ServerModelBean_.getInstance_(RuntimeEnvironment.application);
        outPrintStream = System.out;
        System.setOut(new PrintStream(outContentStream));
    }

    @After
    public void revertStreams() {
        System.setOut(outPrintStream);
    }

    @Test
    public void testDownloadListPosts() throws Exception {
        {
            bean.downloadListPosts();
            lock.await(30, TimeUnit.SECONDS);
            assertThat(outContentStream.toString(), containsString("successfully"));
        }
    }

    @Test
    public  void testDownloadEnhancedListPosts() throws Exception  {
        {
            bean.downloadEnhancedListPosts();
            lock.await(30, TimeUnit.SECONDS);
            assertThat(outContentStream.toString(), containsString("successfully"));
        }
    }

    @Test
    public void testDownloadReadPosts() throws Exception {
        {
            bean.downloadReadPosts();
            lock.await(30, TimeUnit.SECONDS);
            assertThat(outContentStream.toString(), containsString("successfully"));
        }
    }

    @Test
    public void testDownloadEnhancedReadPost() throws Exception  {
        {
            bean.downloadEnhancedReadPost();
            lock.await(30, TimeUnit.SECONDS);
            assertThat(outContentStream.toString(), containsString("successfully"));
        }
    }

    @Test
    public void testPostsRequest() throws Exception  {
        {
            bean.createPost();
            lock.await(30, TimeUnit.SECONDS);
            assertThat(outContentStream.toString(), containsString("successfully"));
        }
    }

    @Test
    public void testPutRequestNotParameters() throws Exception  {
        {
            bean.updatePost(false);
            lock.await(30, TimeUnit.SECONDS);
            assertThat(outContentStream.toString(), containsString("successfully"));
        }
    }

    @Test
    public void testPutRequestWithParameters() throws Exception  {
        {
            bean.updatePost(true);
            lock.await(30, TimeUnit.SECONDS);
            assertThat(outContentStream.toString(), containsString("successfully"));
        }
    }
}