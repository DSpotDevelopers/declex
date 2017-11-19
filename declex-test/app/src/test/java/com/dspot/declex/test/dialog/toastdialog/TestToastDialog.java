package com.dspot.declex.test.dialog.toastdialog;

import com.dspot.declex.actions.ToastActionHolder_;
import com.dspot.declex.test.R;

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

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*" })
@PrepareForTest({ToastActionHolder_.class})
public class TestToastDialog {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testToastDialogAction() {
        ToastActionHolder_ actionToastDialog = ToastActionHolder_.getInstance_(RuntimeEnvironment.application);
        assertNotNull(actionToastDialog);
    }

    @Test
    public void testToastDialogActionProperties() {
        ToastActionHolder_ actionToastDialog = ToastActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionToastDialog.init("DecleX Test Now");
        assertNotNull(actionToastDialog);
    }

    @Test
    public void testToastDialogActionResources() {
        ToastActionHolder_ actionToastDialog = ToastActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionToastDialog.init(R.string.message);
        assertNotNull(actionToastDialog);
    }
}