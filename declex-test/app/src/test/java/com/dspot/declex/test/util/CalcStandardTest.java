package com.dspot.declex.test.util;

import static org.junit.Assert.*;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*"})
public class CalcStandardTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private int first;

    private int second;

    private int result;

    @Before
    public void setUp() throws Exception {
        first = 4;
        second = 5;
        result = 9;
    }

    @Test
    public void testSum() throws Exception {
        CalcStandard calcStandard = spy(new CalcStandard());
        doNothing().when((Calc) calcStandard).createOperation();
        calcStandard.setOperation(Calc.SUM);
        calcStandard.setNumberFirst(first);
        calcStandard.setNumberSecond(second);
        calcStandard.$createOperation();
        verify(calcStandard).$createOperation();
        assertEquals(calcStandard.getResultOperation(), 9);
    }

    @Test
    public void testSubt() throws Exception {
        CalcStandard calcStandard = spy(new CalcStandard());
        doNothing().when((Calc) calcStandard).createOperation();
        calcStandard.setOperation(Calc.SUBT);
        calcStandard.setNumberFirst(first);
        calcStandard.setNumberSecond(second);
        calcStandard.$createOperation();
        verify(calcStandard).$createOperation();
        assertEquals(calcStandard.getResultOperation(), 1);
    }
}