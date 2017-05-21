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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ActionMainActivityActionHolder_.class, ActionMainFragmentActionHolder_.class})
public class TestActionBean {

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

        //Function under test
        bean.callMainFragment();

        verifyStatic();
        ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);

        InOrder inOrder = inOrder(holder);
        inOrder.verify(holder).init();
        inOrder.verify(holder).build(isNull(Runnable.class));
        inOrder.verify(holder).execute();
    }

}