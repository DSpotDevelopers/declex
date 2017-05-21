package com.dspot.declex.test.action;

import android.content.Context;
import android.support.v4.app.FragmentTransaction;

import com.dspot.declex.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.robolectric.util.ReflectionHelpers.getField;
import static org.robolectric.util.ReflectionHelpers.setField;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ActionMainActivityActionHolder_.class, ActionMainFragmentActionHolder_.class})
public class TestActionBean {

    private ActionBean_ bean;

    @Before
    public void loadHolder() {
        bean = ActionBean_.getInstance_(RuntimeEnvironment.application);
    }

    @Test
    public void testActionBean_fragmentAction() {
        ActionMainFragmentActionHolder_ holder = mock(ActionMainFragmentActionHolder_.class);

        doNothing().when(holder);
        holder.init();

        doNothing().when(holder);
        holder.build(isNull(Runnable.class));

        doNothing().when(holder);
        holder.execute();

        mockStatic(ActionMainFragmentActionHolder_.class);
        when(ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application)).thenReturn(holder);

        bean.callMainFragment();

        verifyStatic();
        ActionMainFragmentActionHolder_.getInstance_(RuntimeEnvironment.application);

        InOrder inOrder = inOrder(holder);

        inOrder.verify(holder).init();
        inOrder.verify(holder).build(isNull(Runnable.class));
        inOrder.verify(holder).execute();
    }

}