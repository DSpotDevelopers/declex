package com.dspot.declex.test.model.loadmodel;

import com.dspot.declex.actions.LoadModelActionHolder_;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;
import com.dspot.declex.test.model.loadmodel.model.ServerModelEntity_;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import static com.jayway.awaitility.Awaitility.*;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "javax.net.ssl.*", "org.powermock.*" })
@PrepareForTest({ModelBean_.class, LoadModelActionHolder_.class})
public class TestLoadModel {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testLoadModelMockAction() {
        String title = "Testing";
        Map<String, Object> args = new HashMap();
        args.put("title", "testing");

        LoadModelActionHolder_ loadModel = mock(LoadModelActionHolder_.class);
        when(loadModel.getQuery()).thenReturn("title="+title+"");
        when(loadModel.getFields()).thenReturn("title, body");
        when(loadModel.getArgs()).thenReturn(args);

        assertEquals("title=Testing", loadModel.getQuery());
        assertEquals("title, body", loadModel.getFields());
        assertEquals("testing", loadModel.getArgs().get("title"));
    }

    @Test
    public void testLoadModelAction() {
        String title = "Testing";

        LoadModelActionHolder_ loadModel = LoadModelActionHolder_.getInstance_(RuntimeEnvironment.application);
        loadModel.query("title="+title+"");
        loadModel.fields("title, body");
        loadModel.orderBy("Id ASC");

        assertEquals("title=Testing", loadModel.getQuery());
        assertEquals("title, body", loadModel.getFields());
        assertEquals("Id ASC", loadModel.getOrderBy());
    }

    @Test
    public void testLoadModelModelQuery() {
        final AtomicBoolean Done = new AtomicBoolean(false);
        final AtomicBoolean Failed = new AtomicBoolean(false);

        {
            ModelBean_ bean = ModelBean_.getInstance_(RuntimeEnvironment.application);
            bean.$loadModelQuery(new Runnable() {
                @Override
                public void run() {
                    Done.set(true);
                }
            }, new OnFailedRunnable() {
                @Override
                public void run() {
                    Failed.set(false);
                }
            });
        }

        await().untilTrue(Done);
        await().untilFalse(Failed);
    }
}