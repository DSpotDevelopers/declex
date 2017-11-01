package com.dspot.declex.test.manager;

import com.dspot.declex.api.action.runnable.OnFailedRunnable;

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

import java.util.concurrent.atomic.AtomicBoolean;

import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.junit.Assert.*;


@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*"})
@PrepareForTest({CalcService_.class})
public class CalcServiceTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private int first;

    private int second;

    CalcService_ mockCalcService;

    @Before
    public void setUp() throws Exception {
        mockCalcService = mock(CalcService_.class);
        first = 4;
        second = 5;
    }

    @Test
    public void testCalculateSumFromService() throws Exception {
        doThrow(new RuntimeException()).when(mockCalcService, "calculateSumFromService",  first, second);

        final CalcManager_ calcManager = CalcManager_.getInstance_(mockCalcService.getBaseContext());
        final AtomicBoolean executeSum = new AtomicBoolean(false);
        final AtomicBoolean executeFailSum = new AtomicBoolean(false);

        {
            calcManager.$calculateSumVirtual(first, second);
            calcManager._populate_result(new Runnable() {
                @Override
                public void run() {
                    executeSum.set(true);
                }
            }, new OnFailedRunnable() {
                @Override
                public void run() {
                    executeFailSum.set(false);
                }
            });
        }

        assertTrue(executeSum.get());
        assertFalse(executeFailSum.get());
    }

    @Test
    public void testCalculateSubtFromService() throws Exception {
        doThrow(new RuntimeException()).when(mockCalcService, "calculateSubtFromService",  first, second);

        final CalcManager_ calcManager = CalcManager_.getInstance_(mockCalcService.getBaseContext());
        final AtomicBoolean executeSubt = new AtomicBoolean(false);
        final AtomicBoolean executeFailSubt = new AtomicBoolean(false);

        {
            calcManager.$calculateSubtVirtual(first, second);
            calcManager._populate_result(new Runnable() {
                @Override
                public void run() {
                    executeSubt.set(true);
                }
            }, new OnFailedRunnable() {
                @Override
                public void run() {
                    executeFailSubt.set(true);
                }
            });
        }

        assertTrue(executeSubt.get());
        assertFalse(executeFailSubt.get());
    }

    @Test
    public void testIntentSumFromSevice() {
        CalcService_.IntentBuilder_ intentBuilder = mock(CalcService_.IntentBuilder_.class);
        CalcService_.IntentBuilder_ intentBuilderResume = CalcService_.intent(RuntimeEnvironment.application);

        {
            final CalcService_.IntentBuilder_ intentBuilder_ = intentBuilder.calculateSumFromService(first, second);
            assertNotNull(intentBuilder_);
        }

        {
            final CalcService_.IntentBuilder_ intentBuilderResume_ = intentBuilderResume.calculateSubtFromService(first, second);
            assertNotNull(intentBuilderResume_);
        }
    }

    @Test
    public void testIntentSubtFromSevice() {
        CalcService_.IntentBuilder_ intentBuilder = mock(CalcService_.IntentBuilder_.class);
        CalcService_.IntentBuilder_ intentBuilderResume = CalcService_.intent(RuntimeEnvironment.application);

        {
            final CalcService_.IntentBuilder_ intentBuilder_ = intentBuilder.calculateSubtFromService(first, second);
            assertNotNull(intentBuilder_);
        }

        {
            final CalcService_.IntentBuilder_ intentBuilderResume_ = intentBuilderResume.calculateSubtFromService(first, second);
            assertNotNull(intentBuilderResume_);
        }
    }
}