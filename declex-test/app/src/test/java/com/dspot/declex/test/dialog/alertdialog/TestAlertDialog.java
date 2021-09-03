package com.dspot.declex.test.dialog.alertdialog;

import com.dspot.declex.actions.AlertDialogActionHolder_;
import com.dspot.declex.actions.runnable.DialogClickRunnable;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*"})
@PrepareForTest({ModelBean_.class, AlertDialogActionHolder_.class})
public class TestAlertDialog {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testAlertDialogAction() {
        AlertDialogActionHolder_ actionAlertDialog = AlertDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionAlertDialog.init();

        assertNotNull(actionAlertDialog);
        assertNotNull(actionAlertDialog.builder());
    }

    @Test
    public void testAlertDialogActionProperties() {
        AlertDialogActionHolder_ actionAlertDialog = AlertDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionAlertDialog.init();

        assertNotNull(actionAlertDialog.builder());
        assertNotNull(actionAlertDialog.title("Testing"));
        assertNotNull(actionAlertDialog.message("Declex Test Now"));
        assertNotNull(actionAlertDialog.positiveButton("Yes"));
    }

    @Test
    public void testAlertDialogActionResources() {
        AlertDialogActionHolder_ actionAlertDialog = AlertDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionAlertDialog.init();

        assertNotNull(actionAlertDialog.builder());
        assertNotNull(actionAlertDialog.title(R.string.title));
        assertNotNull(actionAlertDialog.message(R.string.message));
        assertNotNull(actionAlertDialog.positiveButton(R.string.yes));
    }

    @Test
    public void testAlertDialogActionSelectItems() {
        String[] countries = (new String[]{"United State", "France", "England", "China", "Japan"});

        AlertDialogActionHolder_ actionAlertDialog = AlertDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionAlertDialog.init();

        assertNotNull(actionAlertDialog.builder());
        assertNotNull(actionAlertDialog.title("Select Countries"));
        assertNotNull(actionAlertDialog.items(countries));
    }

    @Test
    public void testAlertDialogActionMultiChoice() {
        List<String> countries = new ArrayList<>();
        boolean[] checkedItems = (new boolean[]{true, false, false, true, false});

        AlertDialogActionHolder_ actionAlertDialog = AlertDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionAlertDialog.init();

        assertNotNull(actionAlertDialog.builder());
        assertNotNull(actionAlertDialog.multiChoice(countries, checkedItems));
    }

    @Test
    public void testAlertDialogButtonsAction() {
        DialogClickRunnable PositiveButtonPressed = new DialogClickRunnable() {
            @java.lang.Override
            public void run() {

            }
        };
        DialogClickRunnable NegativeButtonPressed = new DialogClickRunnable() {
            @java.lang.Override
            public void run() {

            }
        };

        AlertDialogActionHolder_ actionAlertDialog = AlertDialogActionHolder_.getInstance_(RuntimeEnvironment.application);
        actionAlertDialog.init();
        actionAlertDialog.title("Testing");
        actionAlertDialog.message("Declex Test Now");
        actionAlertDialog.positiveButton("Yes");
        actionAlertDialog.negativeButton("No");
        actionAlertDialog.build(PositiveButtonPressed, NegativeButtonPressed, null, null, null, null, null);
        actionAlertDialog.execute();

        assertNotNull(actionAlertDialog.builder());
        assertNotNull(actionAlertDialog.dialog());
    }
}