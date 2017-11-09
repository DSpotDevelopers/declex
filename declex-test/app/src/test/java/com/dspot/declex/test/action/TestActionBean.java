/**
 * Copyright (C) 2016-2017 DSpot Sp. z o.o
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.mockito.ArgumentMatchers.isNull;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.mockito.Mockito.inOrder;

@RunWith(RobolectricTestRunner.class)
@Config(
    manifest = "app/src/main/AndroidManifest.xml",
    sdk = 25
)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*" })
@PrepareForTest({ActionMainActivityActionHolder_.class, ActionMainFragmentActionHolder_.class})
public class TestActionBean {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private ActionBean_ bean;

    @Before
    public void loadHolder() {
        bean = ActionBean_.getInstance_(RuntimeEnvironment.application);
    }

    @Test
    public void testFragmentAction() {

        ActionMainFragmentActionHolder_ holder = mock(ActionMainFragmentActionHolder_.class);
        doNothing().when(holder); holder.init();
        doNothing().when(holder); holder.build(isNull(Runnable.class));
        doNothing().when(holder); holder.execute();

        mockStatic(ActionMainFragmentActionHolder_.class);
        when(ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application)).thenReturn(holder);

        {
            //Function under test
            bean.callMainFragment();
        }

        verifyStatic();
        ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);

        InOrder inOrder = inOrder(holder);
        inOrder.verify(holder).init();
        inOrder.verify(holder).build(isNull(Runnable.class));
        inOrder.verify(holder).execute();
    }

}