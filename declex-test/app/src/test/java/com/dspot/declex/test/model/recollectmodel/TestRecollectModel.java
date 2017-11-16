package com.dspot.declex.test.model.recollectmodel;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.dspot.declex.actions.RecollectActionHolder_;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.Recollect;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;
import com.dspot.declex.test.model.service.model.ServerModelEntity_;

import com.mobsandgeeks.saripaar.QuickRule;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.Validator.ValidationListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*" })
@PrepareForTest({ModelBean_.class, RecollectActionHolder_.class, Validator.class, ValidationListener.class})
public class TestRecollectModel {
    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testRecollectAction() {
        Map<String, Object> args = new HashMap();
        ServerModelEntity_ model = ServerModelEntity_.getModel_(RuntimeEnvironment.application, args, Arrays.asList(Annotation.class, Model.class, Recollect.class));

        final AtomicBoolean Done = new AtomicBoolean(false);
        final AtomicBoolean Failed = new AtomicBoolean(false);

        {
            RecollectActionHolder_ recollectAction = RecollectActionHolder_.getInstance_(RuntimeEnvironment.application);
            recollectAction.init(model);
            recollectAction.build(new Runnable() {
                @Override
                public void run() {
                    Done.set(true);
                }
            }, new OnFailedRunnable() {
                @Override
                public void run() {
                    Failed.set(true);
                }
            });

            recollectAction.getDone().run();
            recollectAction.getFailed().run();
        }

        assertTrue(Done.get());
        assertTrue(Failed.get());
    }

    @Test
    public void testRecollectNested() {
        final RecollectActionHolder_ populateAction = RecollectActionHolder_.getInstance_(RuntimeEnvironment.application);
        final AtomicBoolean Done = new AtomicBoolean(false);

        {
            Runnable recollect = new Runnable() {
                @Override
                public void run() {
                    populateAction.build(new Runnable() {
                        @Override
                        public void run() {
                            Done.set(true);
                        }
                    });
                    populateAction.getDone().run();
                }
            };

            recollect.run();
        }

        assertTrue(Done.get());
    }

    @Test
    public void testRecollectValidator() {
        final AtomicBoolean Done = new AtomicBoolean(false);
        final AtomicBoolean Failed = new AtomicBoolean(false);
        final TextView view = mock(TextView.class);

        ValidationListener validatorListener = new ValidationListener() {
            @Override
            public void onValidationSucceeded() {
                Done.set(true);
            }

            @Override
            public void onValidationFailed(List<ValidationError> list) {
                Failed.set(true);
            }
        };
        Validator validator = new Validator(validatorListener);
        validator.setValidationListener(validatorListener);
        validator.put(view, new QuickRule<TextView>() {
            @Override
            public boolean isValid(TextView view) {
                return true;
            }

            @Override
            public String getMessage(Context context) {
                return null;
            }
        });
        validator.validate();

        assertTrue(Done.get());
        assertFalse(Failed.get());
    }
}