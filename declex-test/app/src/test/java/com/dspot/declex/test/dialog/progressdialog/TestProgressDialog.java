package com.dspot.declex.test.dialog.progressdialog;

import com.dspot.declex.actions.ProgressDialogActionHolder_;
import com.dspot.declex.test.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*"})
@PrepareForTest({ModelBean_.class, ProgressDialogActionHolder_.class})
public class TestProgressDialog {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void testProgressDialogAction() {
        ProgressDialogActionHolder_ actionProgress = ProgressDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionProgress.init();
        assertNotNull(actionProgress);
        assertNotNull(actionProgress.dialog());
    }

    @Test
    public void testProgressDialogActionProperties() {
        ProgressDialogActionHolder_ actionProgress = ProgressDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionProgress.init();
        assertNotNull(actionProgress);
        assertNotNull(actionProgress.title("Testing"));
        assertNotNull(actionProgress.message("DecleX Test Now"));
    }

    @Test
    public void testProgressDialogActionResources() {
        ProgressDialogActionHolder_ actionProgress = ProgressDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionProgress.init();
        assertNotNull(actionProgress);
        assertNotNull(actionProgress.title(R.string.title));
        assertNotNull(actionProgress.message(R.string.message));

        String information = "Data User Thomas";
        assertNotNull(actionProgress.message(((("Upload information: " + ((information) + "")) + " to the server"))));
    }

    @Test
    public void testProgressDialogActionEvents() {
        Runnable shownAction = new Runnable() {
            @Override
            public void run() {

            }
        };

        Runnable dismissedAction = new Runnable() {
            @Override
            public void run() {

            }
        };

        ProgressDialogActionHolder_ actionProgress = ProgressDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionProgress.init();
        actionProgress.build(shownAction, null, dismissedAction);
        actionProgress.execute();

        assertNotNull(actionProgress);
        assertNotNull(actionProgress.dialog());
        assertNotNull(actionProgress.title("Testing"));
        assertNotNull(actionProgress.message("DecleX Test Now"));
    }
}