package com.dspot.declex.test.model.servermodel;

import com.dspot.declex.test.model.servermodel.model.ModelPlaceHolder_;

import static org.junit.Assert.*;

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
@PrepareForTest({ModelPlaceHolder_.class})
public class ModelBeanTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private ModelBean_ bean;

    @Before
    public void loadObject() {
        bean = ModelBean_.getInstance_(RuntimeEnvironment.application);
    }

    @Test
    public void downloadListPostsAction() throws IllegalArgumentException {
        {
            bean.downloadListPosts();
        }
    }
}