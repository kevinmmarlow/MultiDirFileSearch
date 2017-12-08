package com.kmarlow.multidirfilesearch;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 Helper class for abstracting RxJava thread schedulers.
 */
public class ThreadSchedulers {

    public Scheduler uiThread() {
        return AndroidSchedulers.mainThread();
    }

    public Scheduler ioThread() {
        return Schedulers.io();
    }

    public Scheduler computationThread() {
        return Schedulers.computation();
    }
}
