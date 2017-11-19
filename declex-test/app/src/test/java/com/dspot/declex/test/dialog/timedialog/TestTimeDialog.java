package com.dspot.declex.test.dialog.timedialog;

import com.dspot.declex.actions.TimeDialogActionHolder;
import com.dspot.declex.actions.TimeDialogActionHolder_;

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

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*" })
@PrepareForTest({ModelBean_.class, TimeDialogActionHolder_.class})
public class TestTimeDialog {
    @Rule
    public PowerMockRule rule = new PowerMockRule();
    
    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testTimeDialogAction() {
        TimeDialogActionHolder_  actionTimeDialog = TimeDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionTimeDialog.init();
        assertNotNull(actionTimeDialog);
        assertNotNull(actionTimeDialog.dialog());

        actionTimeDialog.init(6, 35);
        assertNotNull(actionTimeDialog.dialog());

        actionTimeDialog.init(Calendar.getInstance());
        assertNotNull(actionTimeDialog.dialog());
    }

    @Test
    public void testTimeDialogActionProperties() {
        TimeDialogActionHolder_  actionTimeDialog = TimeDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionTimeDialog.init();
        assertNotNull(actionTimeDialog);
        assertNotNull(actionTimeDialog.title("Select Time"));
        assertNotNull(actionTimeDialog.message("Select Time of the Job"));
    }

    @Test
    public void testTimeDialogSelectAction() {
        TimeDialogActionHolder.TimeSetRunnable timeSelected = new TimeDialogActionHolder.TimeSetRunnable() {
            @Override
            public void run() {

            }
        };

        TimeDialogActionHolder_ actionTimeDialog = TimeDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionTimeDialog.init();
        actionTimeDialog.build(timeSelected, null, null);
        actionTimeDialog.execute();

        assertNotNull(actionTimeDialog);
        assertNotNull(actionTimeDialog.dialog());
    }
}