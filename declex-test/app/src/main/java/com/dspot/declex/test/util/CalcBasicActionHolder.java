package com.dspot.declex.test.util;

import com.dspot.declex.annotation.action.ActionFor;

@ActionFor(value = "CalcBasic")
public class CalcBasicActionHolder {
    protected Calc calc;

    protected Runnable Done;

    private String operation;

    void init(String operation, int numberFirst, int numberSecond) {
        this.operation = operation;
        this.calc = new Calc(numberFirst, numberSecond);
    }

    void build(Runnable Done) {
        this.Done = Done;
    }

    void execute() {
        new Runnable() {
            @Override
            public void run() {
                if (operation.equals("Sum")) {
                    calc.Sum();
                } else if (operation.equals("Rest")) {
                    calc.Rest();
                }
            }
        };
    }

    public CalcBasicActionHolder setNumberFirst(int numberFirst) {
        calc.setNumberFirst(numberFirst);
        return this;
    }

    public CalcBasicActionHolder setNumberSecond(int numberSecond) {
        calc.setNumberSecond(numberSecond);
        return this;
    }
}
