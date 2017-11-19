/**
 * Copyright (C) 2016-2017 DSpot Sp. z o.o
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dspot.declex.test.dialog.progressdialog;

import android.app.Dialog;

import com.dspot.declex.test.R;

import static com.dspot.declex.actions.Action.*;

import org.androidannotations.annotations.EBean;

@EBean
public class ModelBean {
    public void downloadUsers() {
        Dialog dialog = $ProgressDialog()
                .title("Testing")
                .message("DecleX Test Now")
                .dialog();
        dialog.dismiss();
    }

    public void downloadUsersUsingResources() {
        Dialog dialog = $ProgressDialog()
                .title(R.string.title)
                .message(R.string.message)
                .dialog();
        dialog.dismiss();
    }

    public void downloadUsersWithActions(Runnable dismissed, Runnable shown) {
        Dialog dialog = $ProgressDialog()
                .title("Testing")
                .message("DecleX Test Now")
                .dialog();

        if($ProgressDialog.Shown) {
            if(shown != null) shown.run();
        }

        if($ProgressDialog.Dismissed) {
            if(dismissed != null) dismissed.run();
        }

        dialog.dismiss();
    }

    public void showInformation(String information) {
        $ProgressDialog().message("Upload information: {information} to the server");
    }
}
