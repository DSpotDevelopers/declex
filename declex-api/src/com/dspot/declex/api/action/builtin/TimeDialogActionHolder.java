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

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.widget.TimePicker;

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.annotation.Assignable;
import com.dspot.declex.api.action.annotation.FormattedExpression;
import com.dspot.declex.api.action.annotation.StopOn;

/**
 * An Action representing a dialog that prompts the user for the time of day using a
 * {@link TimePicker}.
 *
 * <p>
 * See the <a href="{@docRoot}guide/topics/ui/controls/pickers.html">Pickers</a>
 * guide.
 */

@ActionFor("TimeDialog")
public class TimeDialogActionHolder {

    TimePickerDialog dialog;

    @RootContext
    Context context;
        
    private TimeSetRunnable TimeSet;
    
    void init() {
    	init(null);
    }
    
    /**
     * @param calendar A Calendar object containing the initial hour and minute
     * for the time picker.
     */
    void init(Calendar calendar) {
    	if (calendar == null) {
            // Use the current time as the default values for the picker
            calendar = Calendar.getInstance();    		
    	}
        
        final int hour = calendar.get(Calendar.HOUR);
        final int minute = calendar.get(Calendar.MINUTE);

        init(hour, minute);
    }
    
    /**
     * @param hour The initial hour of the dialog.
     * @param minute The initial minute of the dialog.
     */
    void init(int hour, int minute) {
    	// Create a new instance of TimePickerDialog and return it
        dialog = new TimePickerDialog(
            context,
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    if (TimeSet != null)
                        TimeSet.onTimeSet(view, hourOfDay, minute);
                }
            },
            hour, minute, true
        );
    }
    
    /**
     * @param TimeSet <i><b>(default)</b></i> This Action Selector will be executed when the Time is set.
     * <br><br>Parameters
     * <ul>
     * <li>datePicker - The DatePicker View associated with this listener.</li>
     * <li>hour - The hour that was set.</li>
     * <li>minute - The minute that was set.</li>
     * </ul>  
     * 
     * @param Canceled This Action Selector will be invoked when the dialog is canceled.
     * 
     * @param Dismissed This Action Selector will be invoked when the dialog is dismissed.
     */
    void build(
    		final TimeSetRunnable TimeSet, 
    		final Runnable Canceled, 
    		final Runnable Dismissed) {
    	
    	this.TimeSet = TimeSet;
    	
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
					if (Dismissed != null) Dismissed.run();
				}
			});
    	}
       
    };
	
    void execute() {
		dialog.show();
	}

    /**
     * @return Internal Android Dialog instance.
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
    public TimeDialogActionHolder dialog(@Assignable("dialog") Dialog dialog) {
    	return this;
    }

    public TimeDialogActionHolder title(@FormattedExpression String title) {
        dialog.setTitle(title);
        return this;
    }

    public TimeDialogActionHolder message(@FormattedExpression String message) {
        dialog.setMessage(message);
        return this;
    }

    public static abstract class TimeSetRunnable implements Runnable, TimePickerDialog.OnTimeSetListener {

        public TimePicker timePicker;
        public int hour;
        public int minute;

        @Override
        public void onTimeSet(TimePicker timePicker, int hour, int minute) {
            this.timePicker = timePicker;
            this.hour = hour;
            this.minute = minute;
            run();
        }
    }
}
