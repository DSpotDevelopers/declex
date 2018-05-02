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

import org.androidannotations.annotations.EBean;

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

    public int actionReturnInBlock() {
        {$SimpleAction();}
        return 1;
    }

}
