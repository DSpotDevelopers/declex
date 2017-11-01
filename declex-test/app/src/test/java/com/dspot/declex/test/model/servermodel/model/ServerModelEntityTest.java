package com.dspot.declex.test.model.servermodel.model;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

import org.robolectric.annotation.Config;


@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*", "javax.net.ssl.*"})
@PrepareForTest({ServerModelEntity_.class})
public class ServerModelEntityTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void testGettersAndSetters() {
        ServerModelEntity_ model = new ServerModelEntity_();

        model.setTitle("Test Title");
        assertEquals("Test Title", model.getTitle());

        model.setBody("Test Body");
        assertEquals("Test Body", model.getBody());
    }
}