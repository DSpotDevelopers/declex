/**
 * Copyright (C) 2016-2019 DSpot Sp. z o.o
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

import org.androidannotations.annotations.EBean;

import java.util.Random;

import static com.dspot.declex.Action.$SimpleAction;

@EBean
public class ActionsReturnBean {

    public int actionReturnInt() {
        $SimpleAction();
        return 1;
    }

    public short actionReturnShort() {
        $SimpleAction();
        return 1;
    }

    public long actionReturnLong() {
        $SimpleAction();
        return 1;
    }

    public float actionReturnFloat() {
        $SimpleAction();
        return 1;
    }

    public double actionReturnDouble() {
        $SimpleAction();
        return 1;
    }

    public char actionReturnChar() {
        $SimpleAction();
        return 1;
    }

    public byte actionReturnByte() {
        $SimpleAction();
        return 1;
    }

    public boolean actionReturnBoolean() {
        $SimpleAction();
        return true;
    }

    public ActionsReturnBean actionReturnObject() {
        $SimpleAction();
        return this;
    }

    public int actionReturnOutsideBlock() {
        {$SimpleAction();}
        return 1;
    }

    protected int actionReturnOutsideMultipleBlock() {

        boolean firstCondition = new Random().nextBoolean();
        boolean secondCondition = new Random().nextBoolean();

        if (firstCondition) {
            {$SimpleAction();}
            return 0;
        } else if (!secondCondition) {
            {$SimpleAction();}
            return 1;
        }

        return 2;

    }

    public int actionReturnWithSelectors(String initParam) {

        {
            $SimpleAction(initParam);

            if ($SimpleAction.Selector1) {
                return 1;
            }

            if ($SimpleAction.Selector2) {
                return 2;
            }

            if ($SimpleAction.Selector3) {
                return 3;
            }
        }

        return 0;

    }

    public void actionReturnVoidWithSelectors(String initParam) {

        $SimpleAction(initParam);

        if ($SimpleAction.Selector1) {
            return;
        }

        if ($SimpleAction.Selector2) {
            return;
        }

        if ($SimpleAction.Selector3) {
            return;
        }

    }

    public String actionReturnWithVariables(String param) {
        final String someVariable = "";
        $SimpleAction(someVariable).param1(param);
        return param;
    }

    public void methodReturnVoidBeforeAction(String param) {

        final String someVariable = "";
        if (!someVariable.equals(param)) {
            return;
        }

        $SimpleAction(someVariable).param1(param);

    }

    public String methodReturnBeforeAction(String param) {

        final String someVariable = "";
        if (!someVariable.equals(param)) {
            return someVariable;
        }

        $SimpleAction(someVariable).param1(param);
        return someVariable;

    }

}
