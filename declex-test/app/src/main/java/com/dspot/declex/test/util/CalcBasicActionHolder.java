package com.dspot.declex.test.util;

import android.os.Handler;
import android.os.Looper;

import com.dspot.declex.annotation.action.ActionFor;
import com.dspot.declex.annotation.action.FormattedExpression;
import com.dspot.declex.annotation.action.StopOn;

@ActionFor(value = "CalcBasic")
public class CalcBasicActionHolder {
    protected Calc calc;

    protected int variable;

    protected Runnable Done;

    void init(int field) {
        this.calc = new Calc();
        this.variable = field;
    }

    void build(Runnable Done) {
        this.Done = Done;
    }

    void execute() {
        final Runnable execute  = new Runnable() {
            @Override
            public void run() {
                calc.createOperation();
                if (Done != null) Done.run();
            }
        };

        execute.run();
    }

    @StopOn("create")
    public Calc calc() {
        return this.calc;
    }

    public CalcBasicActionHolder operation(@FormattedExpression String operation) {
        this.calc.setOperation(operation);
        return this;
    }

    public CalcBasicActionHolder numberFirst(int numberFirst) {
        this.calc.setNumberFirst(numberFirst);
        return this;
    }

    public CalcBasicActionHolder numberSecond(int numberSecond) {
        this.calc.setNumberSecond(numberSecond);
        return this;
    }
}