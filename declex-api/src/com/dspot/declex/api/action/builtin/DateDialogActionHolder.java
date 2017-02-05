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

import org.androidannotations.annotations.RootContext;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.widget.DatePicker;

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.annotation.Assignable;
import com.dspot.declex.api.action.annotation.FormattedExpression;
import com.dspot.declex.api.action.annotation.StopOn;

/**
 * An Action representing a simple dialog containing a {@link android.widget.DatePicker}.
 *
 * <p>See the <a href="{@docRoot}guide/topics/ui/controls/pickers.html">Pickers</a>
 * guide.</p>
 */

@ActionFor("DateDialog")
public class DateDialogActionHolder {

    DatePickerDialog dialog;

    @RootContext
    Context context;
        
    private DateSetRunnable DateSet;
    
    void init() {
    	init(null);
    }
        
    /**
     * @param calendar A Calendar object containing the initial day, month and year
     * for the date picker.
     */
    void init(Calendar calendar) {
    	
    	if (calendar == null) {
	        // Use the current time as the default values for the picker
	        calendar = Calendar.getInstance();
    	}
    	        
        final int year = calendar.get(Calendar.YEAR);
        final int month = calendar.get(Calendar.MONTH);
        final int day = calendar.get(Calendar.DATE);

        init(year, month, day);
    }
    
    /**
     * @param year The initial year of the dialog.
     * @param month The initial month of the dialog.
     * @param day The initial day of the dialog.
     */
    void init(int year, int month, int day) {
    	
    	// Create a new instance of DatePickerDialog
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

    
    /**
     * @param DateSet <i><b>(default)</b></i> This Action Selector will be executed when the Date is set.
     * <br><br>Parameters
     * <ul>
     * <li>datePicker - The DatePicker View associated with this listener.</li>
     * <li>year - The year that was set.</li>
     * <li>month - The month that was set (0-11) for compatibility with {@link java.util.Calendar}.</li>
     * <li>day - The day of the month that was set.</li>
     * </ul>  
     * 
     * @param Canceled This Action Selector will be invoked when the dialog is canceled.
     * 
     * @param Dismissed This Action Selector will be invoked when the dialog is dismissed.
     */
    void build(
    		final DateSetRunnable DateSet, 
    		final Runnable Canceled, 
    		final Runnable Dismissed) {
    	
    	this.DateSet = DateSet;
    	
    	if (Canceled != null) {
    		dialog.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface arg0) {
					Canceled.run();
				}
			});
    	}
    	
    	if (Dismissed != null) {
    		 dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				
				@Override
				public void onDismiss(DialogInterface arg0) {
					Dismissed.run();
				}
			});
    	}
       
    };
	
    void execute() {
		dialog.show();
	}

    /**
     * @return Internal Android Dialog instance to make further configurations.
     */
    @StopOn("show")
    public Dialog dialog() {
        return this.dialog;
    }
    
    /**
     * Assigns the Internal Android Dialog instance.
     * 
     * @param dialog The variable to which the dialog is going to be assigned
     */
    public DateDialogActionHolder dialog(@Assignable("dialog") Dialog dialog) {
    	return this;
    }

    public DateDialogActionHolder title(@FormattedExpression String title) {
        dialog.setTitle(title);
        return this;
    }

    public DateDialogActionHolder message(@FormattedExpression String message) {
        dialog.setMessage(message);
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
