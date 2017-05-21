package com.dspot.declex.test.action;

import android.support.v4.app.FragmentTransaction;

import com.dspot.declex.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
public class TestFragmentAction {

    private ActionMainActivity_ activity;

    @Before
    public void loadHolder() {
        //activity = Robolectric.setupActivity(ActionMainActivity_.class);
    }

    @Test
    public void testActionHolder_initMethods() throws Exception {
        ActionMainFragmentActionHolder_ holder = ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);

        holder.init();
        assertNotNull(getField(holder, "builder"));
        //assertNotNull(getField(holder, "transaction"));

        holder = ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);
        holder.init("FragmentTag");
        assertNotNull(getField(holder, "builder"));
        //assertNotNull(getField(holder, "transaction"));
    }

    @Test
    public void testActionHolder_directMethods() throws Exception {
        ActionMainFragmentActionHolder_ holder = ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);

        //Check default container
        assertEquals(R.id.container, getField(holder, "container"));

        holder.container(50);
        assertEquals(50, getField(holder, "container"));

        assertEquals(getField(holder, "transaction"), holder.transaction());
        assertEquals(getField(holder, "builder"), holder.builder());
    }

    @Test
    public void testActionHolder_defaultCallToExecute() throws Exception {
        FragmentTransaction transaction = mock(FragmentTransaction.class);
        when(transaction.commit()).thenReturn(0);
        when(transaction.replace(anyInt(), any(ActionMainFragment_.class), any(String.class))).thenReturn(transaction);

        ActionMainFragmentActionHolder_ holder = ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);
        setField(holder, "transaction", transaction);

        final AtomicBoolean calledStart = new AtomicBoolean(false);

        holder.init("SomeTag");
        holder.container(R.id.container);
        holder.build(new Runnable() {
            @Override
            public void run() {
                calledStart.set(true);
            }
        });

        holder.execute();

        verify(transaction, times(1)).replace(eq(R.id.container), any(ActionMainFragment_.class), eq("SomeTag"));
        verify(transaction, times(1)).commit();
        assertTrue(calledStart.get());
    }

    @Test
    public void testActionHolder_replaceIsCalled() throws Exception {
        FragmentTransaction transaction = mock(FragmentTransaction.class);
        when(transaction.replace(anyInt(), any(ActionMainFragment_.class), isNull(String.class))).thenReturn(transaction);

        ActionMainFragmentActionHolder_ holder = ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);
        setField(holder, "transaction", transaction);

        holder.init();
        holder.replace().execute();

        verify(transaction, times(1)).replace(eq(R.id.container), any(ActionMainFragment_.class), isNull(String.class));
        verify(transaction, times(1)).commit();
    }

    @Test
    public void testActionHolder_addIsCalled() throws Exception {
        FragmentTransaction transaction = mock(FragmentTransaction.class);
        when(transaction.add(anyInt(), any(ActionMainFragment_.class), isNull(String.class))).thenReturn(transaction);

        ActionMainFragmentActionHolder_ holder = ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);
        setField(holder, "transaction", transaction);

        holder.init();
        holder.add().execute();

        verify(transaction, times(1)).add(eq(R.id.container), any(ActionMainFragment_.class), isNull(String.class));
        verify(transaction, times(1)).commit();
    }

    @Test
    public void testActionHolder_addToBackStackIsCalled() throws Exception {
        FragmentTransaction transaction = mock(FragmentTransaction.class);
        when(transaction.addToBackStack(isNull(String.class))).thenReturn(transaction);

        ActionMainFragmentActionHolder_ holder = ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);
        setField(holder, "transaction", transaction);

        holder.init();
        holder.addToBackStack();

        verify(transaction, times(1)).addToBackStack(isNull(String.class));
    }

}