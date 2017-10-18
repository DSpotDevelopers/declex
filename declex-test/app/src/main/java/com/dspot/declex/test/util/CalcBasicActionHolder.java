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
                if (operation.equals(Calc.SUM)) {
                    calc.Sum();
                } else if (operation.equals(Calc.REST)) {
                    calc.Rest();
                }
            }
        };
    }

    public int getNumberFirst() {
        return calc.getNumberFirst();
    }

    public int getNumberSecond() {
        return calc.getNumberSecond();
    }
}
