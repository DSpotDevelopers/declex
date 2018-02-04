package com.dspot.declex.test.model.localdbmodels.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.dspot.declex.annotation.LocalDBModel;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;

import java.util.Locale;

@LocalDBModel
public class Expense extends Model {
    @NotEmpty
    @Column
    String description = "";

    @Column
    String comment = "";

    @NotEmpty
    @Column
    float amount;

    @NotEmpty
    @Column
    String date = "";

    @NotEmpty
    @Column
    String time = "";

    @Column
    long user_id;

    public String getAmount() {
        if (amount == 0) return "";

        return String.format(Locale.US, "%.2f", amount);
    }

    public String datetime() {
        return date + " " + time;
    }
}