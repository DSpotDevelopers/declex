package com.dspot.declex.test.model.servermodel.model;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*"})
@PrepareForTest({ModelPlaceHolder_.class})
public class ModelPlaceHolderTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void testGettersAndSetters()
    {
        ModelPlaceHolder_ placeHolder = new ModelPlaceHolder_();
        placeHolder.setTitle("nesciunt quas odio");
        assertEquals("nesciunt quas odio", placeHolder.getTitle());
    }
}