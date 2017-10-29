package com.dspot.declex.test.manager;

import com.dspot.declex.api.action.runnable.OnFailedRunnable;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.verify;

import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*"})
@PrepareForTest({CalcManager_.class})
public class CalcManagerTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private int first;

    private int second;

    private int result;

    CalcManager_ mockCalcManager;

    @Before
    public void setUp() throws Exception {
        mockCalcManager = mock(CalcManager_.class);
        first = 4;
        second = 5;
        result = 9;
    }

    @Test
    public void calculateSumVirtual() throws Exception {
        doThrow(new RuntimeException()).when(mockCalcManager, "calculateSubtVirtual",  first, second);
        final AtomicBoolean executeSum = new AtomicBoolean(false);
        final AtomicBoolean executeFailSum = new AtomicBoolean(false);

        {
            mockCalcManager.$calculateSumVirtual(first, second);
            mockCalcManager._populate_result(new Runnable() {
                @Override
                public void run() {
                    executeSum.set(true);
                }
            }, new OnFailedRunnable() {
                @Override
                public void run() {
                    executeFailSum.set(true);
                }
            });
        }

        assertTrue(executeSum.get());
        assertFalse(executeFailSum.get());
    }

    @Test
    public void calculateSubtVirtual() throws Exception {
        doThrow(new RuntimeException()).when(mockCalcManager, "calculateSubtVirtual",  first, second);
        final AtomicBoolean executeSubt = new AtomicBoolean(false);
        final AtomicBoolean executeFailSubt = new AtomicBoolean(false);

        {
            mockCalcManager.$calculateSubtVirtual(first, second);
            mockCalcManager._populate_result(new Runnable() {
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
}