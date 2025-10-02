package com.moneytrackultra.cashbook;

import java.util.ArrayList;
import java.util.List;

public class BalanceAggregate {
    public String key;
    public long incomeMinor;
    public long expenseMinor;
    public boolean expanded = false;
    public List<Transaction> transactions = new ArrayList<>();

    public long netMinor() {
        return incomeMinor - expenseMinor;
    }
}