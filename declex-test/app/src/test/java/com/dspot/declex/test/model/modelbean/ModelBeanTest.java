package com.dspot.declex.test.model.modelbean;

import com.dspot.declex.annotation.Model;
import com.dspot.declex.test.model.ModelBean_;
import com.dspot.declex.test.model.usemodel.model.ModelUser;
import com.dspot.declex.test.model.usemodel.model.ModelUser_;

import org.androidannotations.api.BackgroundExecutor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*"})
@PrepareForTest({ModelBean_.class})
public class ModelBeanTest {

    private ModelBean_ bean;
    private AtomicBoolean executeAsyncDone;
    private AtomicBoolean executeAsyncFailed;
    private Map<String, Object> args;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {
        this.bean = ModelBean_.getInstance_(RuntimeEnvironment.application);
        this.executeAsyncDone = new AtomicBoolean(false);
        this.executeAsyncFailed = new AtomicBoolean(false);
        this.args = new HashMap<>();
    }

    @Test
    public void testBeanModelUser() {

        ModelUser_ user = new ModelUser_();
        bean.setUser(user);
        assertEquals(bean.getUser(), user);

    }

    @Test
    public void testBeanLoadingLazy() {
        bean.setLazyUser(ModelUser_.getModel_(RuntimeEnvironment.application, args, Arrays.asList(Annotation.class, Model.class)));
        assertNotNull(bean.getLazyUser());
        assertTrue(bean.getLazyUser() instanceof ModelUser_);
    }

    @Test
    public void testBeanLoadingAsync() {

        final ModelBean_ bean = ModelBean_.getInstance_(RuntimeEnvironment.application);
        final AtomicBoolean executeAsyncDone = new AtomicBoolean(false);
        final AtomicBoolean executeAsyncFailed = new AtomicBoolean(false);
        final Map<String, Object> args = new HashMap<>();

        {
            BackgroundExecutor.execute(new BackgroundExecutor.Task("", 0, "") {
                @Override
                public void execute() {
                    try {
                        bean.setAsyncUser(ModelUser_.getModel_(RuntimeEnvironment.application, args, Arrays.asList(Annotation.class, Model.class)));
                        executeAsyncDone.set(true);
                    } catch (final Throwable e) {
                        executeAsyncFailed.set(true);
                    }
                }
            });

            assertTrue(executeAsyncDone.get());
            assertFalse(executeAsyncFailed.get());
            assertNotNull(bean.getAsyncUser());
            assertTrue(bean.getAsyncUser() instanceof ModelUser_);
        }

    }

    @Test
    public void testBeanAsyncPut() {
        try {
            bean.setAsyncPutUser(ModelUser_.getModel_(RuntimeEnvironment.application, args, Arrays.asList(Annotation.class, Model.class)));
            Runnable Done = new Runnable() {
                @Override
                public void run() {
                    executeAsyncDone.set(true);
                }
            };
            Done.run();
        } catch (final Throwable e) {
            Runnable Failed = new Runnable() {
                @Override
                public void run() {
                    executeAsyncFailed.set(true);
                }
            };
            Failed.run();
        }

        assertTrue(executeAsyncDone.get());
        assertFalse(executeAsyncFailed.get());
        assertNotNull(bean.getAsyncUser());
        assertTrue(bean.getAsyncPutUser() instanceof ModelUser_);
    }

    @Test
    public void testHandleExceptions() {
        try {
            bean.setWihtoutExceptionUser(ModelUser_.getModel_(RuntimeEnvironment.application, args, Arrays.asList(Annotation.class, Model.class)));
            Runnable Done = new Runnable() {
                @Override
                public void run() {
                    executeAsyncDone.set(true);
                }
            };
            Done.run();
        } catch (final Throwable e) {
            Runnable Failed = new Runnable() {
                @Override
                public void run() {
                    executeAsyncFailed.set(true);
                }
            };
            Failed.run();
        }

        assertTrue(executeAsyncDone.get());
        assertFalse(executeAsyncFailed.get());
        assertNotNull(bean.getWihtoutExceptionUser());
        assertTrue(bean.getWihtoutExceptionUser() instanceof ModelUser_);
    }

    @Test
    public void testBeanEnhancedUser() {
        try {
            bean.setEnhancedUser(ModelUser_.getModel_(RuntimeEnvironment.application, args, Arrays.asList(Annotation.class, Model.class)));
            Runnable Done = new Runnable() {
                @Override
                public void run() {
                    executeAsyncDone.set(true);
                }
            };
            Done.run();
        } catch (final Throwable e) {
            Runnable Failed = new Runnable() {
                @Override
                public void run() {
                    executeAsyncFailed.set(true);
                }
            };
            Failed.run();
        }

        assertTrue(executeAsyncDone.get());
        assertFalse(executeAsyncFailed.get());
        assertNotNull(bean.getEnhancedUser());
        assertTrue(bean.getEnhancedUser() instanceof ModelUser_);
    }

    @Test
    public void testBeanModelList() {
        try {
            List<ModelUser_> userListLocal = ModelUser_.getModelList_(RuntimeEnvironment.application, args, Arrays.asList(Annotation.class, Model.class));

            if (bean.getUserList() == null) {
                bean.setUserList(new LinkedList());
            }

            synchronized (bean.getUserList()) {
                bean.getUserList().clear();
                bean.getUserList().addAll(((List<ModelUser>) ((List) userListLocal)));
            }

            Runnable Done = new Runnable() {
                @Override
                public void run() {
                    executeAsyncDone.set(true);
                }
            };
            Done.run();
        } catch (final Throwable e) {
            Runnable Failed = new Runnable() {
                @Override
                public void run() {
                    executeAsyncFailed.set(true);
                }
            };
            Failed.run();
        }

        assertNotNull(bean.getUserList());
        assertTrue(executeAsyncDone.get());
        assertFalse(executeAsyncFailed.get());
    }

    @Test
    public void testBeanModelEnhancedList() {
        try {
            List<ModelUser_> enhancedUserListLocal = ModelUser_.getModelList_(RuntimeEnvironment.application, args, Arrays.asList(Annotation.class, Model.class));

            if (bean.getEnhancedUserList() == null) {
                bean.setEnhancedUserList(new LinkedList());
            }

            synchronized (bean.getEnhancedUserList()) {
                bean.getEnhancedUserList().clear();
                bean.getEnhancedUserList().addAll(enhancedUserListLocal);
            }

            Runnable Done = new Runnable() {
                @Override
                public void run() {
                    executeAsyncDone.set(true);
                }
            };
            Done.run();
        } catch (final Throwable e) {
            Runnable Failed = new Runnable() {
                @Override
                public void run() {
                    executeAsyncFailed.set(true);
                }
            };
            Failed.run();
        }

        assertNotNull(bean.getEnhancedUserList());
        assertTrue(executeAsyncDone.get());
        assertFalse(executeAsyncFailed.get());
    }

    @Test(expected = NoSuchFieldException.class)
    public void testForcedUseModelFields() throws NoSuchFieldException {
        ModelBean_.class.getField("forcedUseModelUser");
        ModelBean_.class.getField("forcedUseModelUserList");
    }

    @Test(expected = NoSuchMethodException.class)
    public void testForcedUseModelMethod() throws NoSuchMethodException {
        ModelBean_.class.getMethod("getForcedUseModelUser");
        ModelBean_.class.getMethod("getForcedUseModelUserList");
    }

    @Test
    public void testBeanModelListQuery() {
        args.put("query", "age=35");

        try {
            List<ModelUser_> enhancedQueryUserListLocal = ModelUser_.getModelList_(RuntimeEnvironment.application, args, Arrays.asList(Annotation.class, Model.class));

            if(enhancedQueryUserListLocal.size() == 0) {
                ModelUser_ user  = new ModelUser_();
                user.setAge(35);
                enhancedQueryUserListLocal.add(user);
            }

            if (bean.getEnhancedQueryUserList() == null) {
                bean.setEnhancedQueryUserList(new LinkedList());
            }

            synchronized (bean.getEnhancedQueryUserList()) {
                bean.getEnhancedQueryUserList().clear();
                bean.getEnhancedQueryUserList().addAll(enhancedQueryUserListLocal);
            }

            Runnable Done = new Runnable() {
                @Override
                public void run() {
                    executeAsyncDone.set(true);
                }
            };
            Done.run();
        } catch (final Throwable e) {
            Runnable Failed = new Runnable() {
                @Override
                public void run() {
                    executeAsyncFailed.set(true);
                }
            };
            Failed.run();
        }

        assertNotNull(bean.getEnhancedQueryUserList());
        assertEquals(35, bean.getEnhancedQueryUserList().get(0).getAge());
        assertTrue(executeAsyncDone.get());
        assertFalse(executeAsyncFailed.get());
    }

    @Test
    public void testBeanModelListQueryAsync() {
        final ModelBean_ bean = ModelBean_.getInstance_(RuntimeEnvironment.application);
        final AtomicBoolean executeAsyncDone = new AtomicBoolean(false);
        final AtomicBoolean executeAsyncFailed = new AtomicBoolean(false);
        final Map<String, Object> args = new HashMap();

        String yourName = "Thomas";
        args.put("query", ("name="+((yourName)+"")));

        {
            BackgroundExecutor.execute(new BackgroundExecutor.Task("", 0, "") {
                @Override
                public void execute() {
                    try {
                        List<ModelUser_> enhancedQueryAsyncUserListLocal = ModelUser_.getModelList_(RuntimeEnvironment.application, args, Arrays.asList(Annotation.class, Model.class));

                        if(enhancedQueryAsyncUserListLocal.size() == 0) {
                            ModelUser_ user  = new ModelUser_();
                            user.setName("Thomas");
                            enhancedQueryAsyncUserListLocal.add(user);
                        }

                        if (bean.getEnhancedQueryAsyncUserList() == null) {
                            bean.setEnhancedQueryAsyncUserList(new LinkedList());
                        }

                        synchronized (bean.getEnhancedQueryAsyncUserList()) {
                            bean.getEnhancedQueryAsyncUserList().clear();
                            bean.getEnhancedQueryAsyncUserList().addAll(enhancedQueryAsyncUserListLocal);
                        }

                        executeAsyncDone.set(true);
                    } catch (final Throwable e) {
                        executeAsyncFailed.set(true);
                    }
                }
            });

            assertNotNull(bean.getEnhancedQueryAsyncUserList());
            assertEquals("Thomas", bean.getEnhancedQueryAsyncUserList().get(0).getName());
            assertTrue(executeAsyncDone.get());
            assertFalse(executeAsyncFailed.get());
        }
    }
}