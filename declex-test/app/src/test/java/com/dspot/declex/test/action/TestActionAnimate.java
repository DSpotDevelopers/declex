package com.dspot.declex.test.action;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentController;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;

import com.dspot.declex.actions.AnimateActionHolder_;
import com.dspot.declex.test.R;

import static junit.framework.Assert.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*" })
@PrepareForTest({ ActionAnimate_.class, AnimateActionHolder_.class, Animation.class })
public class TestActionAnimate {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {}

    @Test
    public void testAnimationProperties() {
        long startTime = 600;

        Animation animation = mock(Animation.class);
        when(animation.getStartTime()).thenReturn(startTime);
        EditText editText = mock(EditText.class);

        AnimateActionHolder_ holder = mock(AnimateActionHolder_.class);
        doNothing().when(holder).init(editText, android.R.anim.fade_in);
        when(holder.animation()).thenReturn(animation);
        holder.init(editText, android.R.anim.fade_in);

        verify(holder).init(editText, android.R.anim.fade_in);
        assertEquals(startTime, holder.animation().getStartTime());
    }

    @Test
    public void testAnimation() {
        Animation animation = AnimationUtils.loadAnimation(RuntimeEnvironment.application, android.R.anim.fade_in);
        assertNotNull(animation);
    }

    @Test
    public void testAnimationClass() {
        long startTime = 300;
        long duration = 600;

        Animation mockAnimation = mock(Animation.class);
        doNothing().when(mockAnimation).setStartTime(startTime);
        when(mockAnimation.getStartTime()).thenReturn(startTime);

        doNothing().when(mockAnimation).setRepeatCount(23);
        when(mockAnimation.getRepeatCount()).thenReturn(23);

        doNothing().when(mockAnimation).setRepeatMode(Animation.RESTART);
        when(mockAnimation.getRepeatMode()).thenReturn(Animation.RESTART);

        doNothing().when(mockAnimation).setDuration(duration);
        when(mockAnimation.getDuration()).thenReturn(duration);

        mockAnimation.setStartTime(startTime);
        mockAnimation.setRepeatCount(23);
        mockAnimation.setRepeatMode(Animation.RESTART);
        mockAnimation.setDuration(duration);

        assertEquals(startTime, mockAnimation.getStartTime());
        assertEquals(duration, mockAnimation.getDuration());
        assertEquals(23, mockAnimation.getRepeatCount());
        assertEquals(Animation.RESTART, mockAnimation.getRepeatMode());
    }
}