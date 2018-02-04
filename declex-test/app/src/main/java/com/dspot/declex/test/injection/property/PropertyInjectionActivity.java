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
package com.dspot.declex.test.injection.property;

import android.app.Activity;
import android.content.res.ColorStateList;

import com.dspot.declex.api.injection.Property;
import com.dspot.declex.test.R;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;

@EActivity(R.layout.view_basic)
public class PropertyInjectionActivity extends Activity {

    CharSequence labelTextSimpleArg;
    int editVisibilityArg;
    Integer imageVisibilityWithWrapperSimpleArg;
    Property<CharSequence> labelTextPropertyArg;
    Property<String> label2TextPropertyAsString;
    private Property<String> labelVisibilityPropertyAsStringArg;
    private Property<Integer> buttonVisibilityPrimitivePropertyArg;
    private Property<ColorStateList> readOnlyPropertyArg;
    private Property<Integer> writeOnlyPropertyArg;

    public CharSequence getLabelTextSimpleArg() {
        return labelTextSimpleArg;
    }

    public int getEditVisibilityArg() {
        return editVisibilityArg;
    }

    public Integer getImageVisibilityWithWrapperSimpleArg() {
        return imageVisibilityWithWrapperSimpleArg;
    }

    public Property<CharSequence> getLabelTextPropertyArg() {
        return labelTextPropertyArg;
    }

    public Property<String> getLabel2TextPropertyAsString() {
        return label2TextPropertyAsString;
    }

    public Property<String> getLabelVisibilityPropertyAsStringArg() {
        return labelVisibilityPropertyAsStringArg;
    }

    public Property<Integer> getButtonVisibilityPrimitivePropertyArg() {
        return buttonVisibilityPrimitivePropertyArg;
    }

    public Property<ColorStateList> getReadOnlyPropertyArg() {
        return readOnlyPropertyArg;
    }

    public Property<Integer> getWriteOnlyPropertyArg() {
        return writeOnlyPropertyArg;
    }

    @AfterViews
    void labelTextSimple(CharSequence labelText) {
        this.labelTextSimpleArg = labelText;
    }

    @AfterViews
    void editVisibilitySimple(int editVisibility) {
        this.editVisibilityArg = editVisibility;
    }

    @AfterViews
    void imageVisibilityWithWrapperSimple(Integer imageVisibility) {
        this.imageVisibilityWithWrapperSimpleArg = imageVisibility;
    }

    @AfterViews
    void labelTextProperty(Property<CharSequence> labelText) {
        this.labelTextPropertyArg = labelText;
    }

    @AfterViews
    void label2TextPropertyAsString(Property<String> label2Text) {
        this.label2TextPropertyAsString = label2Text;
    }

    @AfterViews
    void labelVisibilityPropertyAsString(Property<String> labelVisibility) {
        this.labelVisibilityPropertyAsStringArg = labelVisibility;
    }

    @AfterViews
    void buttonVisibilityPrimitiveProperty(Property<Integer> buttonVisibility) {
        this.buttonVisibilityPrimitivePropertyArg = buttonVisibility;
    }

    @AfterViews
    void readOnlyProperty(Property<ColorStateList> labelTextColors) {
        this.readOnlyPropertyArg = labelTextColors;
    }

    @AfterViews
    void writeOnlyProperty(Property<Integer> imageBackgroundResource) {
        this.writeOnlyPropertyArg = imageBackgroundResource;
    }

}
