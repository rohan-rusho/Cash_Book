package com.moneytrackultra.cashbook;

import android.app.Application;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PrefsManager.init(this);

        // Do NOT call any seeder here.
        // Optional: restore saved currency into runtime formatter if you use one.
        String c = PrefsManager.get().getCurrency();
        if (c != null) {
            CurrencyUtil.setCode(c);
        }
    }
}