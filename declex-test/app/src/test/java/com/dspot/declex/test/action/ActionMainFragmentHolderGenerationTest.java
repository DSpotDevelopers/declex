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

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.dspot.declex.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.util.ReflectionHelpers.getField;
import static org.robolectric.util.ReflectionHelpers.setField;

@RunWith(RobolectricTestRunner.class)
@Config(
    manifest = "app/src/main/AndroidManifest.xml",
    sdk = 25
)
public class ActionMainFragmentHolderGenerationTest {

    @Test
    public void testInitMethods() throws Exception {
        FragmentTransaction transaction = mock(FragmentTransaction.class);

        FragmentManager fragmentManager = mock(FragmentManager.class);
        when(fragmentManager.beginTransaction()).thenReturn(transaction);

        AppCompatActivity activity = mock(AppCompatActivity.class);
        when(activity.getSupportFragmentManager()).thenReturn(fragmentManager);

        {
            ActionMainFragmentActionHolder_ holder = ActionMainFragmentActionHolder_.getInstance_(activity);
            {
                holder.init();
            }

            assertNotNull(getField(holder, "builder"));
            assertEquals(transaction, getField(holder, "transaction"));
        }

        {
            ActionMainFragmentActionHolder_ holder = ActionMainFragmentActionHolder_.getInstance_(activity);
            {
                holder.init("FragmentTag");
            }

            assertNotNull(getField(holder, "builder"));
            assertEquals(transaction, getField(holder, "transaction"));
        }
    }

    @Test
    public void testDirectMethods() throws Exception {
        ActionMainFragmentActionHolder_ holder = ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);

        //Check default container
        assertEquals(R.id.container, getField(holder, "container"));

        holder.container(50);
        assertEquals(50, getField(holder, "container"));

        assertEquals(getField(holder, "transaction"), holder.transaction());
        assertEquals(getField(holder, "builder"), holder.builder());
    }

    @Test
    public void testDefaultCallToExecute() throws Exception {
        FragmentTransaction transaction = mock(FragmentTransaction.class);
        when(transaction.commit()).thenReturn(0);
        when(transaction.replace(anyInt(), any(ActionMainFragment_.class), any(String.class))).thenReturn(transaction);

        ActionMainFragmentActionHolder_ holder = ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);
        setField(holder, "transaction", transaction);

        final AtomicBoolean calledStart = new AtomicBoolean(false);

        {
            holder.init("SomeTag");
            holder.container(R.id.container);
            holder.build(new Runnable() {
                @Override
                public void run() {
                    calledStart.set(true);
                }
            });
            holder.execute();
        }

        verify(transaction, times(1)).replace(eq(R.id.container), any(ActionMainFragment_.class), eq("SomeTag"));
        verify(transaction, times(1)).commit();
        assertTrue(calledStart.get());
    }

    @Test
    public void testReplaceIsCalled() throws Exception {
        FragmentTransaction transaction = mock(FragmentTransaction.class);
        when(transaction.replace(anyInt(), any(ActionMainFragment_.class), isNull(String.class))).thenReturn(transaction);

        ActionMainFragmentActionHolder_ holder = ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);
        setField(holder, "transaction", transaction);

        {
            holder.init();
            holder.replace().execute();
        }

        verify(transaction, times(1)).replace(eq(R.id.container), any(ActionMainFragment_.class), isNull(String.class));
        verify(transaction, times(1)).commit();
    }

    @Test
    public void testAddIsCalled() throws Exception {
        FragmentTransaction transaction = mock(FragmentTransaction.class);
        when(transaction.add(anyInt(), any(ActionMainFragment_.class), isNull(String.class))).thenReturn(transaction);

        ActionMainFragmentActionHolder_ holder = ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);
        setField(holder, "transaction", transaction);

        {
            holder.init();
            holder.add().execute();
        }

        verify(transaction, times(1)).add(eq(R.id.container), any(ActionMainFragment_.class), isNull(String.class));
        verify(transaction, times(1)).commit();
    }

    @Test
    public void testAddToBackStackIsCalled() throws Exception {
        FragmentTransaction transaction = mock(FragmentTransaction.class);
        when(transaction.addToBackStack(isNull(String.class))).thenReturn(transaction);

        ActionMainFragmentActionHolder_ holder = ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);
        setField(holder, "transaction", transaction);

        {
            holder.init();
            holder.addToBackStack();
        }

        verify(transaction, times(1)).addToBackStack(isNull(String.class));
    }

}