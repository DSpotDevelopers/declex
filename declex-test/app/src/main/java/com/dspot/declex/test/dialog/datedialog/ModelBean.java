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
package com.dspot.declex.test.dialog.datedialog;

import static com.dspot.declex.actions.Action.*;

import org.androidannotations.annotations.EBean;

import java.util.Locale;

@EBean
public class ModelBean {
    public void selectDate() {
        $DateDialog()
                .title("Select Date")
                .message("Select Date of the Job");

        int $year = 0, $month = 0, $day = 0;
        String selectedDate = String.format(Locale.US, "Selected date is %02d-%02d-%02d", $year , $month+1, $day);
    }
}
