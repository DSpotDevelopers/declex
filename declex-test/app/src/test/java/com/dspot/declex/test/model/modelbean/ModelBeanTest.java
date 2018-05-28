package com.dspot.declex.test.model.modelbean;

import android.content.Context;

import com.dspot.declex.test.action.ActionActivityActionHolder_;
import com.dspot.declex.test.action.ActionsCallsBean_;
import com.dspot.declex.test.action.holder.SimpleActionHolder_;
import com.dspot.declex.test.model.ModelBean_;
import com.dspot.declex.test.model.usemodel.model.ModelUser;
import com.dspot.declex.test.model.usemodel.model.ModelUser_;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.OngoingStubbing;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*"})
@PrepareForTest({ModelBean_.class, ModelUser_.class})
public class ModelBeanTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void testBeanModelUser() {

        {
            ModelUser_ user = ModelUser_.getInstance_(RuntimeEnvironment.application);

            ModelBean_ modelBean = spy(ModelBean_.getInstance_(RuntimeEnvironment.application));
            when(modelBean.getUser()).thenReturn(user);

            mockStatic(ModelUser_.class);
            PowerMockito.when(ModelUser_.getInstance_(RuntimeEnvironment.application)).thenReturn(user);
            PowerMockito.when(ModelUser_.getModel_(RuntimeEnvironment.application, null, null)).thenReturn(user);

            assertNotNull(modelBean.getUser());
            assertEquals(modelBean.getUser(), user);
        }
    }

    @Test
    public void testBeanLoadingLazy() {

        ModelUser_ user = ModelUser_.getInstance_(RuntimeEnvironment.application);

        ModelBean_ modelBean = spy(ModelBean_.getInstance_(RuntimeEnvironment.application));
        when(modelBean.getLazyUser()).thenReturn(user);

        mockStatic(ModelUser_.class);
        PowerMockito.when(ModelUser_.getInstance_(RuntimeEnvironment.application)).thenReturn(user);
        PowerMockito.when(ModelUser_.getModel_(RuntimeEnvironment.application, null, null)).thenReturn(user);

        assertNotNull(modelBean.getLazyUser());
        assertEquals(modelBean.getLazyUser(), user);
    }

    @Test
    public void testBeanLoadingAsync() {

        ModelUser_ user = ModelUser_.getInstance_(RuntimeEnvironment.application);

        ModelBean_ modelBean = spy(ModelBean_.getInstance_(RuntimeEnvironment.application));
        when(modelBean.getAsyncUser()).thenReturn(user);

        mockStatic(ModelUser_.class);
        PowerMockito.when(ModelUser_.getInstance_(RuntimeEnvironment.application)).thenReturn(user);
        PowerMockito.when(ModelUser_.getModel_(RuntimeEnvironment.application, null, null)).thenReturn(user);

        assertNotNull(modelBean.getAsyncUser());
        assertEquals(modelBean.getAsyncUser(), user);
    }

    @Test
    public void testBeanAsyncPut() {

        ModelUser_ user = ModelUser_.getInstance_(RuntimeEnvironment.application);

        ModelBean_ modelBean = spy(ModelBean_.getInstance_(RuntimeEnvironment.application));
        when(modelBean.getAsyncPutUser()).thenReturn(user);

        mockStatic(ModelUser_.class);
        PowerMockito.when(ModelUser_.getInstance_(RuntimeEnvironment.application)).thenReturn(user);
        PowerMockito.when(ModelUser_.getModel_(RuntimeEnvironment.application, null, null)).thenReturn(user);

        assertNotNull(modelBean.getAsyncPutUser());
        assertEquals(modelBean.getAsyncPutUser(), user);
    }

    @Test
    public void testHandleExceptions() {

        ModelUser_ user = ModelUser_.getInstance_(RuntimeEnvironment.application);

        ModelBean_ modelBean = spy(ModelBean_.getInstance_(RuntimeEnvironment.application));
        when(modelBean.getWithoutExceptionUser()).thenReturn(user);

        mockStatic(ModelUser_.class);
        PowerMockito.when(ModelUser_.getInstance_(RuntimeEnvironment.application)).thenReturn(user);
        PowerMockito.when(ModelUser_.getModel_(RuntimeEnvironment.application, null, null)).thenReturn(user);

        assertNotNull(modelBean.getWithoutExceptionUser());
        assertEquals(modelBean.getWithoutExceptionUser(), user);
    }

    @Test
    public void testBeanEnhancedUser() {

        ModelUser_ user = ModelUser_.getInstance_(RuntimeEnvironment.application);

        ModelBean_ modelBean = mock(ModelBean_.class);
        when(modelBean.getEnhancedUser()).thenReturn(user);

        assertNotNull(modelBean.getEnhancedUser());
    }

    @Test
    public void testBeanModelList() {

        ModelBean_ modelBean = mock(ModelBean_.class);
        assertNotNull(modelBean.getUserList());
    }

    @Test
    public void testBeanModelEnhancedList() {

        ModelBean_ modelBean = mock(ModelBean_.class);
        assertNotNull(modelBean.getEnhancedUserList());
    }

    @Test
    public void testBeanModelListQuery() {

        ModelBean_ modelBean = mock(ModelBean_.class);
        assertNotNull(modelBean.getEnhancedQueryUserList());
    }

    @Test
    public void testBeanModelListQueryAsync() {

        ModelBean_ modelBean = mock(ModelBean_.class);
        assertNotNull(modelBean.getEnhancedQueryAsyncUserList());
    }
}