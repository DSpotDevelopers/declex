/**
 * Copyright (C) 2016-2018 DSpot Sp. z o.o
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dspot.declex.test.action;

import android.content.Context;

import com.dspot.declex.api.action.runnable.OnActivityResultRunnable;
import com.dspot.declex.test.action.holder.SimpleActionHolder_;

import org.androidannotations.annotations.Background;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.mockito.Mockito.inOrder;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*" })
@PrepareOnlyThisForTest({
        ActionActivityActionHolder_.class,
        ActionFragmentActionHolder_.class,
        SimpleActionHolder_.class
})
public class ActionsCallsTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private ActionsCallsBean_ bean;

    private ActionFragmentActionHolder_ fragmentHolder;

    private ActionActivityActionHolder_ activityHolder;

    private SimpleActionHolder_ simpleHolder;

    @Before
    public void loadHolder() {
        bean = ActionsCallsBean_.getInstance_(RuntimeEnvironment.application);
    }

    private void mockFragmentHolder() {
        fragmentHolder = mock(ActionFragmentActionHolder_.class);
        doNothing().when(fragmentHolder).init();
        doNothing().when(fragmentHolder).build(isNull(Runnable.class));
        doNothing().when(fragmentHolder).execute();

        mockStatic(ActionFragmentActionHolder_.class);
        when(ActionFragmentActionHolder_.getInstance_(RuntimeEnvironment.application)).thenReturn(fragmentHolder);
    }

    private void mockActivityHolder() {
        activityHolder = mock(ActionActivityActionHolder_.class);
        doNothing().when(activityHolder).init();
        doNothing().when(activityHolder).build(isNull(Runnable.class));
        doReturn(null).when(activityHolder).setBuilder(any(Context.class));
        doNothing().when(activityHolder).execute();

        mockStatic(ActionActivityActionHolder_.class);
        when(ActionActivityActionHolder_.getInstance_(any(Context.class))).thenReturn(activityHolder);
    }

    private void mockSimpleHolder() {

        simpleHolder = spy(SimpleActionHolder_.getInstance_(RuntimeEnvironment.application));

        mockStatic(SimpleActionHolder_.class);
        when(SimpleActionHolder_.getInstance_(any(Context.class))).thenReturn(simpleHolder);
    }

    @Test
    public void testFragmentAction() {

        mockFragmentHolder();

        //Function under test
        bean.callMainFragment();

        verifyStatic(ActionFragmentActionHolder_.class); ActionFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);

        InOrder inOrder = inOrder(fragmentHolder);
        inOrder.verify(fragmentHolder).init();
        inOrder.verify(fragmentHolder).build(isNull(Runnable.class));
        inOrder.verify(fragmentHolder).execute();
    }

    @Test
    public void testActivityAction() {

        mockActivityHolder();

        //Function under test
        bean.callMainActivity();

        verifyStatic(ActionActivityActionHolder_.class); ActionActivityActionHolder_.getInstance_(RuntimeEnvironment.application);

        InOrder inOrder = inOrder(activityHolder);
        inOrder.verify(activityHolder).init();
        inOrder.verify(activityHolder).setBuilder(RuntimeEnvironment.application);
        inOrder.verify(activityHolder).build(isNull(Runnable.class), isNull(OnActivityResultRunnable.class));
        inOrder.verify(activityHolder).execute();
    }

    @Test
    public void testSimpleActionCalls() {

        {
            mockSimpleHolder();

            bean.callSimpleAction();

            verifyStatic(SimpleActionHolder_.class); SimpleActionHolder_.getInstance_(RuntimeEnvironment.application);

            InOrder inOrder = inOrder(simpleHolder);
            inOrder.verify(simpleHolder).init();
            inOrder.verify(simpleHolder).build(isNull(Runnable.class), isNull(Runnable.class), isNull(Runnable.class));
            inOrder.verify(simpleHolder).execute();
        }

        {
            mockSimpleHolder();

            bean.callSimpleActionWithInitParam("any");

            verifyStatic(SimpleActionHolder_.class);
            SimpleActionHolder_.getInstance_(RuntimeEnvironment.application);

            InOrder inOrder = inOrder(simpleHolder);
            inOrder.verify(simpleHolder).init("any");
            inOrder.verify(simpleHolder).build(isNull(Runnable.class), isNull(Runnable.class), isNull(Runnable.class));
            inOrder.verify(simpleHolder).execute();

            assertEquals("any", simpleHolder.getInitParam());
        }

        {
            mockSimpleHolder();

            bean.callSimpleActionWithInitParamAndMethod("any");

            verifyStatic(); SimpleActionHolder_.getInstance_(RuntimeEnvironment.application);

            InOrder inOrder = inOrder(simpleHolder);
            inOrder.verify(simpleHolder).init("any");
            inOrder.verify(simpleHolder).method1();
            inOrder.verify(simpleHolder).build(isNull(Runnable.class), isNull(Runnable.class), isNull(Runnable.class));
            inOrder.verify(simpleHolder).execute();

            assertEquals("any", simpleHolder.getInitParam());
        }

        {
            mockSimpleHolder();

            bean.callSimpleActionWithInitParamAndParam("any", "any param");

            verifyStatic(); SimpleActionHolder_.getInstance_(RuntimeEnvironment.application);

            InOrder inOrder = inOrder(simpleHolder);
            inOrder.verify(simpleHolder).init("any");
            inOrder.verify(simpleHolder).param1("any param");
            inOrder.verify(simpleHolder).build(isNull(Runnable.class), isNull(Runnable.class), isNull(Runnable.class));
            inOrder.verify(simpleHolder).execute();

            assertEquals("any", simpleHolder.getInitParam());
            assertEquals("any param", simpleHolder.getParam1());
        }

        {
            mockSimpleHolder();

            bean.callSimpleActionWithParamAndMethod("any param");

            verifyStatic(); SimpleActionHolder_.getInstance_(RuntimeEnvironment.application);

            InOrder inOrder = inOrder(simpleHolder);
            inOrder.verify(simpleHolder).init();
            inOrder.verify(simpleHolder).param1("any param");
            inOrder.verify(simpleHolder).method1();
            inOrder.verify(simpleHolder).build(isNull(Runnable.class), isNull(Runnable.class), isNull(Runnable.class));
            inOrder.verify(simpleHolder).execute();

            assertEquals("any param", simpleHolder.getParam1());
        }

        {
            mockSimpleHolder();

            bean.callSimpleActionWithMethodAndParam("any param");

            verifyStatic(); SimpleActionHolder_.getInstance_(RuntimeEnvironment.application);

            InOrder inOrder = inOrder(simpleHolder);
            inOrder.verify(simpleHolder).init();
            inOrder.verify(simpleHolder).method1();
            inOrder.verify(simpleHolder).param1("any param");
            inOrder.verify(simpleHolder).build(isNull(Runnable.class), isNull(Runnable.class), isNull(Runnable.class));
            inOrder.verify(simpleHolder).execute();

            assertEquals("any param", simpleHolder.getParam1());
        }


    }

}