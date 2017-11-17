package com.dspot.declex.test.thread;

import com.dspot.declex.actions.BackgroundThreadActionHolder_;
import static com.jayway.awaitility.Awaitility.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*"})
@PrepareForTest({Background_.class, BackgroundThreadActionHolder_.class})
public class TestBackgroundThread {
    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testBackgroundAction() {
        final AtomicBoolean Done = new AtomicBoolean(false);

        {
            BackgroundThreadActionHolder_ action = BackgroundThreadActionHolder_.getInstance_(RuntimeEnvironment.application);
            action.init();
            action.build(new Runnable() {
                @Override
                public void run() {
                    Done.set(true);
                }
            });
            action.execute();
        }

        await().untilTrue(Done);
    }

    @Test
    public void testBackgroundBean() {
        final AtomicBoolean Done = new AtomicBoolean(false);
        final AtomicBoolean Failed = new AtomicBoolean(false);

        {
            Background_ bean = Background_.getInstance_(RuntimeEnvironment.application);
            bean.$detailsPosts(new Runnable() {
                @Override
                public void run() {
                    Done.set(true);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    Failed.set(true);
                }
            });
        }

        await().untilTrue(Done);
        await().untilFalse(Failed);
    }

    @Test
    public void testTwoBackgroundBean() {
        final AtomicBoolean One = new AtomicBoolean(false);
        final AtomicBoolean Two = new AtomicBoolean(false);

        {
            BackgroundThreadActionHolder_ actionOne = BackgroundThreadActionHolder_.getInstance_(RuntimeEnvironment.application);
            actionOne.init();
            actionOne.build(new Runnable() {
                @Override
                public void run() {
                    One.set(true);
                }
            });
            actionOne.execute();
        }
        {
            BackgroundThreadActionHolder_ actionTwo = BackgroundThreadActionHolder_.getInstance_(RuntimeEnvironment.application);
            actionTwo.init();
            actionTwo.build(new Runnable() {
                @Override
                public void run() {
                    Two.set(true);
                }
            });
            actionTwo.execute();
        }

        assertTrue(One.get());
        assertTrue(Two.get());
    }

    @Test
    public void testBackgroundTwoActions() {
        final AtomicBoolean One = new AtomicBoolean(false);
        final AtomicBoolean Two = new AtomicBoolean(false);

        {
            BackgroundThreadActionHolder_ action = BackgroundThreadActionHolder_.getInstance_(RuntimeEnvironment.application);
            action.init();
            action.build(new Runnable() {
                @Override
                public void run() {
                    {
                        One.set(true);
                        Runnable twoAction = new Runnable() {
                            @Override
                            public void run() {
                                Two.set(true);
                            }
                        };
                        twoAction.run();
                    }
                }
            });
            action.execute();
        }

        await().untilTrue(One);
        await().untilTrue(Two);
    }
}