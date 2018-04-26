/**
 * Copyright (C) 2016-2018 DSpot Sp. z o.o
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
package com.dspot.declex.test.injection.property;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.dspot.declex.api.injection.Property;
import com.dspot.declex.test.R;

import org.androidannotations.api.view.HasViews;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PropertyInjectionTest {

    private static final String LABEL_TEXT_TO_SET = "Label Text To Set";
    private static final String LABEL_TEXT = "Label Text";
    private static final String LABEL2_TEXT_TO_SET = "Label Text To Set";
    private static final String LABEL2_TEXT = "Label2 Text";

    private static final int LABEL_VISIBILITY = 10;
    private static final int EDIT_VISIBILITY = 20;
    private static final Integer IMAGE_VISIBILITY = 50;

    private static final Integer BUTTON_VISIBILITY_TO_SET = 80;
    private static final Integer BUTTON_VISIBILITY = 90;

    private static final ColorStateList LABEL_COLOR_STATE_LIST = mock(ColorStateList.class);

    private static final Integer IMAGE_BACKGROUND_RESOURCE = 10;

    private PropertyInjectionActivity_ activity;
    private HasViews viewsContainer;

    private TextView label;
    private TextView label2;
    private EditText edit;
    private ImageView image;
    private Button button;

    @Before
    public void setUp() {
        activity = Robolectric.setupActivity(PropertyInjectionActivity_.class);
    }

    public void prepareMockedViews() {

        label = mock(TextView.class);
        when(label.getText()).thenReturn(LABEL_TEXT);
        when(label.getVisibility()).thenReturn(LABEL_VISIBILITY);
        when(label.getTextColors()).thenReturn(LABEL_COLOR_STATE_LIST);

        label2 = mock(TextView.class);
        when(label2.getText()).thenReturn(LABEL2_TEXT);

        edit = mock(EditText.class);
        when(edit.getVisibility()).thenReturn(EDIT_VISIBILITY);

        image = mock(ImageView.class);
        when(image.getVisibility()).thenReturn(IMAGE_VISIBILITY);
        doNothing().when(image).setBackgroundResource(any(Integer.class));

        button = mock(Button.class);
        when(button.getVisibility()).thenReturn(BUTTON_VISIBILITY);

        viewsContainer = mock(HasViews.class);
        when(viewsContainer.internalFindViewById(R.id.label)).thenReturn(label);
        when(viewsContainer.internalFindViewById(R.id.label2)).thenReturn(label2);
        when(viewsContainer.internalFindViewById(R.id.edit)).thenReturn(edit);
        when(viewsContainer.internalFindViewById(R.id.image)).thenReturn(image);
        when(viewsContainer.internalFindViewById(R.id.button)).thenReturn(button);

    }

    @Test
    public void testPropertiesOnAfterView() {

        prepareMockedViews();

        {
            activity.onViewChanged(viewsContainer);
        }

        assertEquals(activity.getLabelTextSimpleArg(), LABEL_TEXT);
        assertEquals(activity.getEditVisibilityArg(), EDIT_VISIBILITY);
        assertEquals(activity.getImageVisibilityWithWrapperSimpleArg(), IMAGE_VISIBILITY);

        Property<CharSequence> charSequenceProperty = activity.getLabelTextPropertyArg();
        assertEquals(charSequenceProperty.get(), LABEL_TEXT);
        charSequenceProperty.set(LABEL_TEXT_TO_SET);
        verify(label).setText(LABEL_TEXT_TO_SET);

        Property<String> stringProperty = activity.getLabel2TextPropertyAsString();
        assertEquals(stringProperty.get(), LABEL2_TEXT);
        stringProperty.set(LABEL2_TEXT_TO_SET);
        verify(label2).setText(LABEL2_TEXT_TO_SET);

        stringProperty = activity.getLabelVisibilityPropertyAsStringArg();
        assertEquals(stringProperty.get(), String.valueOf(LABEL_VISIBILITY));
        stringProperty.set(LABEL_TEXT_TO_SET);
        verify(label, never()).setVisibility(any(Integer.class));

        Property<Integer> integerProperty = activity.getButtonVisibilityPrimitivePropertyArg();
        assertEquals(integerProperty.get(), BUTTON_VISIBILITY);
        integerProperty.set(BUTTON_VISIBILITY_TO_SET);
        verify(button).setVisibility(BUTTON_VISIBILITY_TO_SET);

        Property<ColorStateList> colorStateListProperty = activity.getReadOnlyPropertyArg();
        assertEquals(colorStateListProperty.get(), LABEL_COLOR_STATE_LIST);
        colorStateListProperty.set(LABEL_COLOR_STATE_LIST);

        integerProperty = activity.getWriteOnlyPropertyArg();
        assertEquals(integerProperty.get(), null);
        integerProperty.set(IMAGE_BACKGROUND_RESOURCE);
        verify(image).setBackgroundResource(IMAGE_BACKGROUND_RESOURCE);

    }

}