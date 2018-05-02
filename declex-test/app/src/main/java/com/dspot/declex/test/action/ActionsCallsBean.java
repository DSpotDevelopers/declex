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
package com.dspot.declex.test.action;

import android.support.annotation.NonNull;

import org.androidannotations.annotations.EBean;

import static com.dspot.declex.Action.$ActionActivity;
import static com.dspot.declex.Action.$ActionFragment;
import static com.dspot.declex.Action.$SimpleAction;

@EBean
public class ActionsCallsBean {

    public void callMainFragment() {
        $ActionFragment();
    }

    public void callMainActivity() { $ActionActivity(); }

    public void callSimpleAction() {
        $SimpleAction();
    }

    public void callSimpleActionWithInitParam(String initParam) {
        $SimpleAction(initParam);
    }

    public void callSimpleActionWithInitParamAndMethod(String initParam) {
        $SimpleAction(initParam).method1();
    }

    public void callSimpleActionWithInitParamAndParam(String initParam, String param) {
        $SimpleAction(initParam).param1(param);
    }

    public void callSimpleActionWithParamAndMethod(String param) {
        $SimpleAction().param1(param).method1();
    }

    public void callSimpleActionWithMethodAndParam(String param) {
        $SimpleAction().method1().param1(param);
    }

    public void callSimpleActionAndCheckDefaultSelector(@NonNull Runnable afterDefaultSelector) {
        $SimpleAction();
        afterDefaultSelector.run();
    }

    public void callSimpleActionAndCheckIfElseWithDefaultSelector(String initParam,
            @NonNull Runnable afterDefaultSelector, @NonNull Runnable afterSecondSelector) {

        $SimpleAction(initParam);
        if ($SimpleAction.Selector1) {
            afterDefaultSelector.run();
        } else {
            afterSecondSelector.run();
        }

    }

    public void callSimpleActionAndCheckIfElseWithSecondSelector(String initParam,
            @NonNull Runnable afterDefaultSelector, @NonNull Runnable afterSecondSelector) {

        $SimpleAction(initParam);
        if ($SimpleAction.Selector2) {
            afterSecondSelector.run();
        } else {
            afterDefaultSelector.run();
        }

    }

    public void callSimpleActionAndCheckIfElseWithThirdSelector(String initParam,
            @NonNull Runnable afterDefaultSelector, @NonNull Runnable afterThirdSelector) {

        $SimpleAction(initParam);
        if ($SimpleAction.Selector3) {
            afterThirdSelector.run();
        } else {
            afterDefaultSelector.run();
        }

    }

    public void callSimpleActionAndCheckSelectors(String initParam,
            @NonNull Runnable afterDefaultSelector, @NonNull Runnable afterSecondSelector,
            @NonNull Runnable afterThirdSelector) {

        $SimpleAction(initParam);

        if ($SimpleAction.Selector1) {
            afterDefaultSelector.run();
        }

        if ($SimpleAction.Selector2) {
            afterSecondSelector.run();
        }

        if ($SimpleAction.Selector3) {
            afterThirdSelector.run();
        }

    }


}
