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
import android.support.v4.app.Fragment;
import android.widget.ImageView;

import com.dspot.declex.api.injection.Property;
import com.dspot.declex.test.R;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EFragment;

@EActivity(R.layout.view_basic)
public class PropertyInjectionActivity extends Activity {

    @AfterViews
    void labelTextSimple(CharSequence labelText) {

    }

    @AfterViews
    void editVisibilitySimple(int editVisibility) {

    }

    @AfterViews
    void imageVisibilitySimple(Integer imageVisibility) {

    }

    @AfterViews
    void labelText(Property<CharSequence> labelText) {

    }

    @AfterViews
    void labelTextString(Property<String> labelText) {

    }

    @AfterViews
    void labelVisibilityString(Property<String> labelVisibility) {

    }

    @AfterViews
    void buttonVisibility(Property<Integer> buttonVisibility) {

    }

    @AfterViews
    void readOnlyProperty(Property<Integer> labelTextColor) {

    }

    @AfterViews
    void writeOnlyProperty(Property<Integer> imageBackgroundResource) {

    }

}
