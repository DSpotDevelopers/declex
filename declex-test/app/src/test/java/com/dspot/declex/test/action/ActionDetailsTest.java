package com.dspot.declex.test.action;

import com.dspot.declex.test.util.Calc;
import com.dspot.declex.test.util.CalcBasicActionHolder_;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;

import org.mockito.InOrder;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*"})
@PrepareForTest({CalcBasicActionHolder_.class})
public class ActionDetailsTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private ActionDetails_ bean;

    private int first;

    private int second;

    private int result;

    @Before
    public void setUp() throws Exception {
        bean = ActionDetails_.getInstance_(RuntimeEnvironment.application);
        first = 4;
        second = 5;
        result = 9;
    }

    @Test
    public void testCalBasicActionIsAction() {
        CalcBasicActionHolder_ holder = mock(CalcBasicActionHolder_.class);
        doNothing().when(holder);holder.init(result);
        doNothing().when(holder);holder.build(isNull(Runnable.class));
        doNothing().when(holder);holder.execute();

        mockStatic(CalcBasicActionHolder_.class);
        when(CalcBasicActionHolder_.getInstance_(RuntimeEnvironment.application)).thenReturn(holder);

        {
            //Function under test
            bean.calcSumValues(first, second);
        }

        verifyStatic();
        CalcBasicActionHolder_.getInstance_(RuntimeEnvironment.application);

        InOrder inOrder = inOrder(holder);
        inOrder.verify(holder).init(result);
        inOrder.verify(holder).build(isNull(Runnable.class));
        inOrder.verify(holder).execute();
    }

    @Test
    public void testCalBasicActionInBeanClass() {

    }
}