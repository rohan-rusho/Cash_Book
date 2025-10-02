package com.moneytrackultra.cashbook;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a wallet / account.
 * 'expanded' is a transient UI state (not required to persist).
 */
public class Wallet {
    public long id;
    public String name;
    public long balance;                  // Stored in MAJOR units (whole), adjust if you move to minor units.
    public List<WalletTransaction> transactions = new ArrayList<>();

    // Transient UI state for adapter
    public boolean expanded = false;

    public Wallet() {}

    public Wallet(long id, String name, long balance) {
        this.id = id;
        this.name = name;
        this.balance = balance;
    }
}