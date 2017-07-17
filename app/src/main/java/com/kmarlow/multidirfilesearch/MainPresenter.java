package com.kmarlow.multidirfilesearch;

import com.jakewharton.rxbinding2.InitialValueObservable;
import com.jakewharton.rxbinding2.widget.TextViewAfterTextChangeEvent;

import io.reactivex.disposables.CompositeDisposable;

/**
 The presenter layer for our main view.
 This class contains all of our business logic
 and ideally should not contain any Android code.
 */
public class MainPresenter {

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final MainScreen screen;

    public MainPresenter(MainScreen screen) {
        this.screen = screen;
    }

    public void onSearchTextChanges(InitialValueObservable<TextViewAfterTextChangeEvent> textChangeObservable) {

    }

    public void onDestroy() {
        compositeDisposable.clear();
    }
}
