package com.dspot.declex.test.model.putmodel;

import com.dspot.declex.actions.PutModelActionHolder_;

import static com.jayway.awaitility.Awaitility.*;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;

import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;


@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "javax.net.ssl.*", "org.powermock.*"})
@PrepareForTest({ModelBean_.class, PutModelActionHolder_.class})
public class TestPutModel {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testPutModelMockAction() {
        String title = "Testing";

        PutModelActionHolder_ model = mock(PutModelActionHolder_.class);
        when(model.getQuery()).thenReturn("title=" + title + "");
        when(model.getFields()).thenReturn("title, body");

        assertEquals("title=Testing", model.getQuery());
        assertEquals("title, body", model.getFields());
    }

    @Test
    public void testPutModelAction() {
        String title = "Testing";

        PutModelActionHolder_ model = PutModelActionHolder_.getInstance_(RuntimeEnvironment.application);
        model.query("title=" + title + "");
        model.fields("title, body");
        model.orderBy("create");

        assertEquals("title=Testing", model.getQuery());
        assertEquals("title, body", model.getFields());
        assertEquals("create", model.getOrderBy());
    }

    @Test
    public void testPutModelQuery() {
        final AtomicBoolean Done = new AtomicBoolean(false);
        final AtomicBoolean Failed = new AtomicBoolean(false);

        {
            ModelBean_ bean = ModelBean_.getInstance_(RuntimeEnvironment.application);
            bean.$updateModelQuery(new Runnable() {
                @Override
                public void run() {
                    Done.set(true);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    Failed.set(true);
                }
            });
        }

        await().untilTrue(Done);
        await().untilFalse(Failed);
    }
}