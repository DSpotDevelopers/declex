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
package com.dspot.declex.action.util;

import com.dspot.declex.action.Actions;
import org.androidannotations.AndroidAnnotationsEnvironment;

public class ActionsLogger {

    private String debugIndex = "";

    private boolean isValidating;

    private AndroidAnnotationsEnvironment environment;

    public ActionsLogger(boolean isValidating, AndroidAnnotationsEnvironment environment) {
        this.isValidating = isValidating;
        this.environment = environment;
    }

    public void info(String message) {
        if (showDebugInfo()) System.out.println(debugPrefix() + message);
    }

    public void increaseIndex() {
        debugIndex = debugIndex + "    ";
    }

    public void decreaseIndex() {
        debugIndex = debugIndex.substring(0, debugIndex.length()-4);

    }

    private boolean showDebugInfo() {
        if (isValidating) return false;

        return environment.getOptionBooleanValue(Actions.OPTION_DEBUG_ACTIONS);
    }

    private String debugPrefix() {
        String prefix = "";
        return prefix + debugIndex;
    }

}
