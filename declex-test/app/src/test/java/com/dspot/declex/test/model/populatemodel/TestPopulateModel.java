package com.dspot.declex.test.model.populatemodel;

import com.dspot.declex.actions.PopulateActionHolder_;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.Populate;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;
import com.dspot.declex.test.model.service.model.ServerModelEntity_;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;


@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*" })
@PrepareForTest({ModelBean_.class, PopulateActionHolder_.class})
public class TestPopulateModel {
    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testPopulateAction() {
        Map<String, Object> args = new HashMap();
        ServerModelEntity_ model = ServerModelEntity_.getModel_(RuntimeEnvironment.application, args, Arrays.asList(Annotation.class, Model.class, Populate.class));

        final AtomicBoolean Done = new AtomicBoolean(false);
        final AtomicBoolean Failed = new AtomicBoolean(false);

        {
            PopulateActionHolder_ populateAction = PopulateActionHolder_.getInstance_(RuntimeEnvironment.application);
            populateAction.init(model);
            populateAction.build(new Runnable() {
                @Override
                public void run() {
                    Done.set(true);
                }
            }, new OnFailedRunnable() {
                @Override
                public void run() {
                    Failed.set(true);
                }
            });

            populateAction.getDone().run();
            populateAction.getFailed().run();
        }

        assertTrue(Done.get());
        assertTrue(Failed.get());
    }

    @Test
    public void testPopulateNested() {
        final PopulateActionHolder_ populateAction = PopulateActionHolder_.getInstance_(RuntimeEnvironment.application);
        final AtomicBoolean Done = new AtomicBoolean(false);

        {
            Runnable recollect = new Runnable() {
                @Override
                public void run() {
                    populateAction.build(new Runnable() {
                        @Override
                        public void run() {
                            Done.set(true);
                        }
                    });
                    populateAction.getDone().run();
                }
            };

            recollect.run();
        }

        assertTrue(Done.get());
    }

    @Test
    public void testBeanPopulate() {
        ModelBean_ bean = ModelBean_.getInstance_(RuntimeEnvironment.application);
        bean.$click_saveModel();

        assertNotNull(bean.getPopulateModel());
        assertEquals("Testing", bean.getPopulateModel().getTitle());
        assertEquals("Testing Request", bean.getPopulateModel().getBody());
    }

    @Test
    public void testBeanNested() {
        ModelBean_ bean = ModelBean_.getInstance_(RuntimeEnvironment.application);
        bean.$click_deleteModel();

        assertNotNull(bean.getBothModel());
        assertEquals("Testing both", bean.getBothModel().getTitle());
    }
}