package com.dspot.declex.test.thread;

import com.dspot.declex.actions.BackgroundThreadActionHolder_;
import com.dspot.declex.actions.UIThreadActionHolder_;
import static com.jayway.awaitility.Awaitility.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "javax.net.ssl.*", "org.powermock.*" })
@PrepareForTest({UIThread_.class, UIThreadActionHolder_.class})
public class TestUIThread {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void testUIThreadAction() {
        final AtomicBoolean Done = new AtomicBoolean(false);

        {
            UIThreadActionHolder_ actionThread = UIThreadActionHolder_.getInstance_(RuntimeEnvironment.application);
            actionThread.init();
            actionThread.build(new Runnable() {
                @Override
                public void run() {
                    Done.set(true);
                }
            });
            actionThread.execute();
        }

        await().untilTrue(Done);
    }

    @Test
    public void testUIThreadWithBackgroundAction() {
        final AtomicBoolean BackgroundAction = new AtomicBoolean(false);
        final AtomicBoolean UIThreadAction = new AtomicBoolean(false);

        Runnable actionBackground = new Runnable() {
            @Override
            public void run() {
                BackgroundAction.set(true);

                UIThreadActionHolder_ actionThread = UIThreadActionHolder_.getInstance_(RuntimeEnvironment.application);
                actionThread.init();
                actionThread.build(new Runnable() {
                    @Override
                    public void run() {
                        UIThreadAction.set(true);
                    }
                });
                actionThread.execute();
            }
        };
        actionBackground.run();

        await().untilTrue(BackgroundAction);
        await().untilTrue(UIThreadAction);
    }

    @Test
    public void testUIThreadBeanAction() {
        final AtomicBoolean Failed = new AtomicBoolean(false);

        UIThread_ bean = UIThread_.getInstance_(RuntimeEnvironment.application);
        bean.$sortListModel(new Runnable() {
            @Override
            public void run() {
                Failed.set(true);
            }
        });

        await().untilFalse(Failed);
        assertNotNull(bean.getDetailsModel());
    }
}