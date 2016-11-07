/**
 * Copyright (C) 2016 DSpot Sp. z o.o
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
package com.dspot.declex.api.action.builtin;

import java.util.Calendar;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.DatePicker;

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.annotation.Assignable;
import com.dspot.declex.api.action.annotation.FormattedExpression;

@EBean
@ActionFor("DateDialog")
public class DateDialogActionHolder {

    DatePickerDialog dialog;

    @RootContext
    Context context;
        
    private DateSetRunnable DateSet;
    
    void init() {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        final int day = c.get(Calendar.DATE);
        int month = c.get(Calendar.MONTH);
        int year = c.get(Calendar.YEAR);

        // Create a new instance of TimePickerDialog
        dialog = new DatePickerDialog(
            context,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    if (DateSet != null)
                        DateSet.onDateSet(view, year, monthOfYear, dayOfMonth);
                }
            },
            year, month, day
        );
    }
    
	//Here you can infer any parameter, the first parameter is the next listener 
    void build(DateSetRunnable DateSet, final Runnable Dismissed) {
    	this.DateSet = DateSet;
    	
    	if (Dismissed != null) {
    		 dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				
				@Override
				public void onDismiss(DialogInterface arg0) {
					if (Dismissed != null) Dismissed.run();
				}
			});
    	}
       
    };
	
    void execute() {
		dialog.show();
	}

    public Dialog dialog() {
        return this.dialog;
    }

    public DateDialogActionHolder title(@FormattedExpression String title) {
        dialog.setTitle(title);
        return this;
    }

    public DateDialogActionHolder message(@FormattedExpression String message) {
        dialog.setMessage(message);
        return this;
    }

    public DateDialogActionHolder into(@Assignable("dialog") Dialog dialog) {
    	return this;
    }



    public static abstract class DateSetRunnable implements Runnable, DatePickerDialog.OnDateSetListener {

        public DatePicker datePicker;
        public int year;
        public int month;
        public int day;

        @Override
        public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
            this.datePicker = datePicker;
            this.year = year;
            this.month = monthOfYear;
            this.day = dayOfMonth;
            run();
        }
    }
}
