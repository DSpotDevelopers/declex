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

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.TimePicker;

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.annotation.Assignable;
import com.dspot.declex.api.action.annotation.FormattedExpression;

@EBean
@ActionFor("TimeDialog")
public class TimeDialogActionHolder {

    TimePickerDialog dialog;

    @RootContext
    Context context;
        
    private TimeSetRunnable TimeSet;
    
    void init() {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int minute = c.get(Calendar.MINUTE);

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
    
	//Here you can infer any parameter, the first parameter is the next listener 
    void build(TimeSetRunnable TimeSet, final Runnable Dismissed) {
    	this.TimeSet = TimeSet;
    	
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

    public TimeDialogActionHolder title(@FormattedExpression String title) {
        dialog.setTitle(title);
        return this;
    }

    public TimeDialogActionHolder message(@FormattedExpression String message) {
        dialog.setMessage(message);
        return this;
    }

    public TimeDialogActionHolder into(@Assignable("dialog") Dialog dialog) {
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
