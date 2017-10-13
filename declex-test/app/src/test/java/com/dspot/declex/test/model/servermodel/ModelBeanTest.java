package com.dspot.declex.test.model.servermodel;

import com.dspot.declex.test.model.servermodel.model.ModelPlaceHolder_;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*", "javax.net.ssl.*"})
@PrepareForTest({ModelBean_.class})
public class ModelBeanTest {
    private ModelBean_ bean;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() {
        bean = mock(ModelBean_.class);
    }

    @Test
    public void testDownloadListPosts() {
        when(bean.getSizeListPosts()).thenReturn(0);
    }
}