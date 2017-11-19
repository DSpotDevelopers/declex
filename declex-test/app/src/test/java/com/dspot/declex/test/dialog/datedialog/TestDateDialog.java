package com.dspot.declex.test.dialog.datedialog;

import com.dspot.declex.actions.DateDialogActionHolder;
import com.dspot.declex.actions.DateDialogActionHolder_;

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
@PrepareForTest({ModelBean_.class, DateDialogActionHolder_.class})
public class TestDateDialog {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testDateDialogAction() {
        DateDialogActionHolder_  actionDateDialog = DateDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionDateDialog.init();
        assertNotNull(actionDateDialog);
        assertNotNull(actionDateDialog.dialog());

        actionDateDialog.init(2017, 11, 18);
        assertNotNull(actionDateDialog.dialog());

        actionDateDialog.init(Calendar.getInstance());
        assertNotNull(actionDateDialog.dialog());
    }

    @Test
    public void testDateDialogActionProperties() {
        DateDialogActionHolder_  actionDateDialog = DateDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionDateDialog.init();
        assertNotNull(actionDateDialog);
        assertNotNull(actionDateDialog.title("Select Date"));
        assertNotNull(actionDateDialog.message("Select Date of the Job"));
    }

    @Test
    public void testDateDialogSelectAction() {
        DateDialogActionHolder.DateSetRunnable dateSelected = new DateDialogActionHolder.DateSetRunnable() {
            @Override
            public void run() {

            }
        };

        DateDialogActionHolder_ actionDateDialog = DateDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionDateDialog.init();
        actionDateDialog.build(dateSelected, null, null);
        actionDateDialog.execute();

        assertNotNull(actionDateDialog);
        assertNotNull(actionDateDialog.dialog());
    }
}