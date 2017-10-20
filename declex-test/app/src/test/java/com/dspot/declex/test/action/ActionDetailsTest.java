package com.dspot.declex.test.action;

import com.dspot.declex.event.GenerateResult;
import com.dspot.declex.event.GenerateResult_;
import com.dspot.declex.test.util.Calc;
import com.dspot.declex.test.util.CalcBasicActionHolder_;

import org.androidannotations.api.BackgroundExecutor;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;

import org.mockito.InOrder;
import org.mockito.internal.matchers.Not;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;

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
@PrepareForTest({CalcBasicActionHolder_.class, ActionMainFragment_.class})
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
        doNothing().when(holder);
        holder.init(result);
        doNothing().when(holder);
        holder.build(isNull(Runnable.class));
        doNothing().when(holder);
        holder.execute();

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
        {
            bean.calcSumValues(first, second);
            assertEquals(bean.getResult(), result);
        }
    }

    @Test
    public void testCalBasicActionBackground() {
        final CalcBasicActionHolder_ holder = mock(CalcBasicActionHolder_.class);

        final AtomicBoolean resultOperation = new AtomicBoolean(false);

        BackgroundExecutor.execute(new BackgroundExecutor.Task("", 0L, "") {
            @Override
            public void execute() {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        holder.init(result);
                        holder.build(new Runnable() {
                            @Override
                            public void run() {
                                resultOperation.set(true);
                            }
                        });
                        holder.execute();
                    }
                };

                runnable.run();
            }
        });

        assertFalse(resultOperation.get());
    }

    @Test
    public void testCalBasicActionInFragment() {
        ActionMainFragment_ fragment = mock(ActionMainFragment_.class);
        when(fragment.first()).thenReturn(first);
        when(fragment.second()).thenReturn(second);
        when(fragment.getResult()).thenReturn(result);

        {
            fragment.onResume();
        }

        assertEquals(fragment.getResult(), result);
    }

    @Test
    public void testResumeOverrideInFragment() {
        ActionMainFragment_ fragment = spy(new ActionMainFragment_());
        doNothing().when((ActionMainFragment) fragment).onResume();
        fragment.$onResume();
        verify(fragment).calcBasic();
    }

    @Test
    public void testActionEvent() {
        final GenerateResult_ event = GenerateResult_.getInstance_(RuntimeEnvironment.application);

        final AtomicBoolean executeEvent = new AtomicBoolean(false);

        {
            event.init();
            event.build(new GenerateResult.EventFinishedRunnable() {
                @Override
                public void run() {
                    executeEvent.set(true);
                }
            }, null);
            event.execute();
        }

        assertTrue(executeEvent.get());
    }

    @Test
    public void testActionInEvent() {
        {
            bean.callEvent();
            assertEquals(bean.getResult(), first * second);
        }
    }

    @Test
    public void testCallTwoActionsResume() {
        final GenerateResult_ event = GenerateResult_.getInstance_(RuntimeEnvironment.application);
        final CalcBasicActionHolder_ holder = CalcBasicActionHolder_.getInstance_(RuntimeEnvironment.application);
        final AtomicBoolean executeHolder = new AtomicBoolean(false);

        {
            event.init();
            event.build(new GenerateResult.EventFinishedRunnable() {
                @Override
                public void run() {
                    holder.init((result));
                    holder.operation((Calc.SUBT));
                    holder.numberFirst((first));
                    holder.numberSecond((second));
                    holder.build(new java.lang.Runnable() {

                                     @java.lang.Override
                                     public void run() {
                                         executeHolder.set(true);
                                     }
                                 }
                    );
                    holder.execute();
                }
            }, null);
            event.execute();
        }

        assertTrue(executeHolder.get());
    }

    @Test
    public void testCallTwoActions() {
        {
            bean.callTwoActions(first, second);
            assertEquals(bean.getResult(), 40);
        }
    }
}