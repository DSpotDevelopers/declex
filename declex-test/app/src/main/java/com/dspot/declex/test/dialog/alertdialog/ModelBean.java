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
package com.dspot.declex.test.dialog.alertdialog;

import com.dspot.declex.test.R;

import static com.dspot.declex.actions.Action.*;

import org.androidannotations.annotations.EBean;

import java.util.ArrayList;
import java.util.List;


@EBean
public class ModelBean {
    public void showAlertWithMessageAndTitle() {
        $AlertDialog().title("Testing").message("Declex Test Now").positiveButton("Yes");
    }

    public void showAlertUsingStringResources() {
        $AlertDialog().title(R.string.title)
                .message(R.string.message)
                .positiveButton(R.string.yes)
                .negativeButton(R.string.no)
                .neutralButton(R.string.ask_later);
    }

    public void showAlertItemSelection() {
        String[] countries = new String[]{"United State", "France", "England", "China", "Japan"};
        $AlertDialog().title("Select Countries").items(countries);

        if ($AlertDialog.ItemSelected) {
            int $position = 0;
            String country = countries[$position];
        }
    }

    public void showAlertMultiChoice() {
        String[] countries = new String[]{"United State", "France", "England", "China", "Japan"};
        $AlertDialog().title("Select Countries").multiChoice(countries);

        if($AlertDialog.MultiChoiceSelected) {
            int $position = 0;
            String country = countries[$position];
        }
    }

    public void showAlertMultiChoiceBooleanChecked() {
        List<String> countries = new ArrayList<>();
        boolean[] checkedItems = new boolean[]{ true, false, false, true, false };

        $AlertDialog().title("Select Countries").multiChoice(countries, checkedItems);

        if($AlertDialog.MultiChoiceSelected) {
            int $position = 0;
            String country = countries.get($position);
        }
    }

    public void showAlertActionsButtons(Runnable Positive, Runnable Negative) {
        $AlertDialog().title("Testing")
                .message("Declex Test Now")
                .positiveButton("Yes")
                .negativeButton("No");

        if($AlertDialog.PositiveButtonPressed) {
            if(Positive != null) Positive.run();
        }

        if($AlertDialog.NegativeButtonPressed) {
            if(Negative != null) Negative.run();
        }
    }
}