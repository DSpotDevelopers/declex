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
package com.dspot.declex.api.action.builtin;

import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.ListView;

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.annotation.Assignable;
import com.dspot.declex.api.action.annotation.FormattedExpression;
import com.dspot.declex.api.action.annotation.StopOn;
import com.dspot.declex.api.action.runnable.dialog.DialogClickRunnable;
import com.dspot.declex.api.action.runnable.dialog.DialogMultiChoiceClickRunnable;

@EBean
@ActionFor("AlertDialog")
public class AlertDialogActionHolder {
    AlertDialog.Builder builder;

    String positiveButtonText;
    int positiveButtonRes;

    String negativeButtonText;
    int negativeButtonRes;

    String neutralButtonText;
    int neutralButtonRes;

	String[] multiChoiceItems;
	int multiChoiceRes;
	
	boolean[] checkedItems;
	
	String[] items;
	int itemsRes;
	
    AlertDialog dialog;

    @RootContext
    Context context;
        
    void init() {
        builder = new AlertDialog.Builder(context);
    }
    
	//Here you can infer any parameter, the first parameter is the next listener 
    /**
     * @param PositiveButtonPressed This Action Selector will be executed when the positive button is pressed in
     * the AlertDialog
     * @param NegativeButtonPressed This Action Selector will be executed when the negative button is pressed in
     * the AlertDialog
     * @param NeutralButtonPressed This Action Selector will be executed when the neutral button is pressed in
     * the AlertDialog 
     * @param Canceled This Action Selector will be executed when the AlertDialog is canceled
     * @param Canceled This Action Selector will be executed when the AlertDialog is dismissed  
     */
    void build(
    		final DialogClickRunnable PositiveButtonPressed, 
    		final DialogClickRunnable NegativeButtonPressed,
    		final DialogClickRunnable NeutralButtonPressed,
    		final DialogClickRunnable ItemSelected, 
    		final DialogMultiChoiceClickRunnable MultiChoiceSelected,
    		final Runnable Canceled, 
    		final Runnable Dismissed) { 

		if (negativeButtonText != null) {
			builder.setNegativeButton(negativeButtonText, NegativeButtonPressed);
		} else if (negativeButtonRes != 0) {
			builder.setNegativeButton(negativeButtonRes, NegativeButtonPressed);
		}
		
		if (positiveButtonText != null) {
			builder.setPositiveButton(positiveButtonText, PositiveButtonPressed);
		} else if (positiveButtonRes != 0) {
			builder.setPositiveButton(positiveButtonRes, PositiveButtonPressed);
		}
		
		if (neutralButtonText != null) {
			builder.setNeutralButton(neutralButtonText, NeutralButtonPressed);
		} else if (neutralButtonRes != 0) {
			builder.setNeutralButton(neutralButtonRes, NeutralButtonPressed);
		}
		
		if (items != null) {
			builder.setItems(items, ItemSelected);
		} else if (itemsRes != 0) {
			builder.setItems(itemsRes, ItemSelected);
		}
		
		if (multiChoiceItems != null) {
			builder.setMultiChoiceItems(multiChoiceItems, checkedItems, MultiChoiceSelected);
		} else if (multiChoiceRes != 0) {
			builder.setMultiChoiceItems(multiChoiceRes, checkedItems, MultiChoiceSelected);
		}
		

		
        dialog = builder.create();
        
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
    public AlertDialogActionHolder dialog(@Assignable("dialog") Dialog dialog) {
    	return this;
    }
    
    /**
     * @return Android Builder layer to access the underlying Notifications Builder object
     * 
     * @see android.support.v4.app.NotificationCompat.Builder Notifications Builder
     */
    @StopOn("create")
    public AlertDialog.Builder builder() {
    	return this.builder;
    }
    
    /**
     * Set the title using the given resource id.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public AlertDialogActionHolder title(@StringRes int titleId) {
        builder.setTitle(context.getString(titleId));
        return this;
    }
    
    /**
     * Set the title displayed in the {@link Dialog}.
     */
    public AlertDialogActionHolder title(@FormattedExpression String title) {
        builder.setTitle(title);
        return this;
    }
    
    /**
     * Set the title using the custom view {@code customTitleView}.
     * <p>
     * The methods {@link #setTitle(int)} and {@link #setIcon(int)} should
     * be sufficient for most titles, but this is provided if the title
     * needs more customization. Using this will replace the title and icon
     * set via the other methods.
     * <p>
     * <strong>Note:</strong> To ensure consistent styling, the custom view
     * should be inflated or constructed using the alert dialog's themed
     * context obtained via {@link #getContext()}.
     *
     * @param customTitleView the custom view to use as the title
     */
    public AlertDialogActionHolder customTitle(@Nullable View customTitleView){
    	builder.setCustomTitle(customTitleView)   ;
    	return this;
    }

    /**
     * Set the message to display using the given resource id.
     */
    public AlertDialogActionHolder message(@StringRes int messageId) {
        builder.setMessage(context.getString(messageId));
        return this;
    }

    /**
     * Set the message to display.
     */
    public AlertDialogActionHolder message(@FormattedExpression String message) {
        builder.setMessage(message);
        return this;
    }
    
    /**
     * Set the resource id of the {@link Drawable} to be used in the title.
     * <p>
     * Takes precedence over values set using {@link #setIcon(Drawable)}.
     */
    public AlertDialogActionHolder icon(@DrawableRes int iconId){
    	builder.setIcon(iconId);
    	return this;
    }

    /**
     * Set the {@link Drawable} to be used in the title.
     * <p>
     * <strong>Note:</strong> To ensure consistent styling, the drawable
     * should be inflated or constructed using the alert dialog's themed
     * context obtained via {@link #getContext()}.
     */
    public AlertDialogActionHolder icon(@Nullable Drawable icon){
    	builder.setIcon(icon);
    	return this;
    }

    /**
     * Set an icon as supplied by a theme attribute. e.g.
     * {@link android.R.attr#alertDialogIcon}.
     * <p>
     * Takes precedence over values set using {@link #setIcon(int)} or
     * {@link #setIcon(Drawable)}.
     *
     * @param attrId ID of a theme attribute that points to a drawable resource.
     */
    public AlertDialogActionHolder setIconAttribute(@AttrRes int attrId){
    	builder.setIconAttribute(attrId);
    	return this;
    }
    
    /**
     * Set a listener to be invoked when the positive button of the dialog is pressed.
     * @param textId The resource id of the text to display in the positive button
     */
    public AlertDialogActionHolder positiveButton(int textId) {
        positiveButtonRes = textId;
        return this;
    }
    
    /**
     * Set a listener to be invoked when the positive button of the dialog is pressed.
     * @param text The text to display in the positive button
     */
    public AlertDialogActionHolder positiveButton(@FormattedExpression String text) {
        positiveButtonText = text;
        return this;
    }

    /**
     * Set a listener to be invoked when the negative button of the dialog is pressed.
     * @param textId The resource id of the text to display in the negative button
     */
    public AlertDialogActionHolder negativeButton(int textId) {
        negativeButtonRes = textId;
        return this;
    }

    /**
     * Set a listener to be invoked when the negative button of the dialog is pressed.
     * @param text The text to display in the negative button
     */
    public AlertDialogActionHolder negativeButton(@FormattedExpression String text) {
        negativeButtonText = text;
        return this;
    }

    /**
     * Set a listener to be invoked when the neutral button of the dialog is pressed.
     * @param textId The resource id of the text to display in the neutral button
     */
    public AlertDialogActionHolder neutralButton(int textId) {
    	neutralButtonRes = textId;
        return this;
    }
    
    /**
     * Set a listener to be invoked when the negative button of the dialog is pressed.
     * @param text The text to display in the negative button
     */
    public AlertDialogActionHolder neutralButton(@FormattedExpression String text) {
        neutralButtonText = text;
        return this;
    }
    
    /**
     * Sets whether the dialog is cancelable or not.  Default is true.
     */
    public AlertDialogActionHolder cancelable(boolean cancelable){
    	builder.setCancelable(cancelable);
    	return this;
    }

    /**
     * Set a list of items to be displayed in the dialog as the content, you will be notified of the
     * selected item via the supplied listener. This should be an array type i.e. R.array.foo
     */
	public AlertDialogActionHolder items(int res) {
		this.itemsRes = res;
        return this;
    }
	
    /**
     * Set a list of items to be displayed in the dialog as the content, you will be notified of the
     * selected item via the supplied listener.
     */
	public AlertDialogActionHolder items(String ... items) {
		this.items = items;
        return this;
    }

    /**
     * Set a list of items to be displayed in the dialog as the content, you will be notified of the
     * selected item via the supplied listener.
     */
	public AlertDialogActionHolder items(List<?> items) {
		this.items = new String[items.size()];
		for (int i = 0; i < items.size(); i++) {
			this.items[i] = items.get(i).toString();
		}
        return this;
    }
	
    /**
     * Set a list of items to be displayed in the dialog as the content,
     * you will be notified of the selected item via the supplied listener.
     * This should be an array type, e.g. R.array.foo. The list will have
     * a check mark displayed to the right of the text for each checked
     * item. Clicking on an item in the list will not dismiss the dialog.
     * Clicking on a button will dismiss the dialog.
     *
     * @param itemsId the resource id of an array i.e. R.array.foo
     * @param checkedItems specifies which items are checked. It should be null in which case no
     *        items are checked. If non null it must be exactly the same length as the array of
     *        items.
     */
	public AlertDialogActionHolder multiChoice(int res, boolean[] checkedItemsArray) {
		multiChoiceRes = res;
		checkedItems = checkedItemsArray;
        return this;
    }

    /**
     * Set a list of items to be displayed in the dialog as the content,
     * you will be notified of the selected item via the supplied listener.
     * This should be an array type, e.g. R.array.foo. The list will have
     * a check mark displayed to the right of the text for each checked
     * item. Clicking on an item in the list will not dismiss the dialog.
     * Clicking on a button will dismiss the dialog.
     *
     * @param itemsId the resource id of an array i.e. R.array.foo
     */
	public AlertDialogActionHolder multiChoice(int res) {
		multiChoiceRes = res;
        return this;
    }
	
    /**
     * Set a list of items to be displayed in the dialog as the content,
     * you will be notified of the selected item via the supplied listener.
     * The list will have a check mark displayed to the right of the text
     * for each checked item. Clicking on an item in the list will not
     * dismiss the dialog. Clicking on a button will dismiss the dialog.
     *
     * @param items the text of the items to be displayed in the list.
     */
	public AlertDialogActionHolder multiChoice(String ... items) {
		multiChoiceItems = items;
        return this;
    }
	
    /**
     * Set a list of items to be displayed in the dialog as the content,
     * you will be notified of the selected item via the supplied listener.
     * The list will have a check mark displayed to the right of the text
     * for each checked item. Clicking on an item in the list will not
     * dismiss the dialog. Clicking on a button will dismiss the dialog.
     *
     * @param items the text of the items to be displayed in the list.
     */
	public AlertDialogActionHolder multiChoice(List<?> items) {
		this.multiChoiceItems = new String[items.size()];
		for (int i = 0; i < items.size(); i++) {
			this.multiChoiceItems[i] = items.get(i).toString();
		}
        return this;
    }
	
    /**
     * Set a list of items to be displayed in the dialog as the content,
     * you will be notified of the selected item via the supplied listener.
     * The list will have a check mark displayed to the right of the text
     * for each checked item. Clicking on an item in the list will not
     * dismiss the dialog. Clicking on a button will dismiss the dialog.
     *
     * @param items the text of the items to be displayed in the list.
     * @param checkedItems specifies which items are checked. It should be null in which case no
     *        items are checked. If non null it must be exactly the same length as the array of
     *        items.
     */
	public AlertDialogActionHolder multiChoice(List<String> items, boolean[] checkedItemsArray) {
		multiChoiceItems = new String[items.size()];
		multiChoiceItems = items.toArray(multiChoiceItems);
		checkedItems = checkedItemsArray;
        return this;
    }
	
    /**
     * Set a custom view resource to be the contents of the Dialog. The
     * resource will be inflated, adding all top-level views to the screen.
     *
     * @param layoutResId View Resource ID to be inflated.
     */
    public AlertDialogActionHolder view(int layoutResId) {
        builder.setView(layoutResId);
        return this;
    }

    /**
     * Sets a custom view to be the contents of the alert dialog.
     * <p>
     * When using a pre-Holo theme, if the supplied view is an instance of
     * a {@link ListView} then the light background will be used.
     * <p>
     * <strong>Note:</strong> To ensure consistent styling, the custom view
     * should be inflated or constructed using the alert dialog's themed
     * context obtained via {@link #getContext()}.
     *
     * @param view the view to use as the contents of the alert dialog
     */
    public AlertDialogActionHolder view(View view) {
        builder.setView(view);
        return this;
    }	
	
}
