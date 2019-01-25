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
package com.dspot.declex.test.action.holder;

import com.dspot.declex.annotation.action.ActionFor;

import java.util.Arrays;
import java.util.List;

@ActionFor(value="SimpleAction", timeConsuming = false)
public class SimpleActionHolder {

	Runnable Selector1, Selector2, Selector3;

	String initParam;
	String param1;

	void init() {
        this.initParam = "0";
	}

    void init(String initParam) {
        this.initParam = initParam;
    }

    void build(Runnable Selector1, Runnable Selector2, Runnable Selector3) {
        this.Selector1 = Selector1;
        this.Selector2 = Selector2;
        this.Selector3 = Selector3;
    }

    void execute() {

	    List<String> selectorsTriggers = Arrays.asList("2", "3");

        if (this.Selector1 != null && !selectorsTriggers.contains(initParam)) {
            this.Selector1.run();
        } else if (this.Selector2 != null && initParam.equals("2")) {
            this.Selector2.run();
        } else if (this.Selector3 != null && initParam.equals("3")) {
            this.Selector3.run();
        }

    }

    public SimpleActionHolder method1() {
        return this;
    }

    public SimpleActionHolder param1(String param1) {
	    this.param1 = param1;
	    return this;
    }

    public String getInitParam() {
	    return initParam;
    }

    public String getParam1() {
	    return param1;
    }

}