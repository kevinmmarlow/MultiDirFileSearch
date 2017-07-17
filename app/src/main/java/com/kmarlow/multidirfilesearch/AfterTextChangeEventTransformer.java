package com.kmarlow.multidirfilesearch;

import android.text.Editable;

import com.jakewharton.rxbinding2.widget.TextViewAfterTextChangeEvent;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;

/**
 Simple transformer to convert text view change events to strings
 */
public class AfterTextChangeEventTransformer implements ObservableTransformer<TextViewAfterTextChangeEvent, String> {

    @Override
    public ObservableSource<String> apply(@NonNull Observable<TextViewAfterTextChangeEvent> upstream) {
        return upstream
                .map(new Function<TextViewAfterTextChangeEvent, Editable>() {
                    @Override
                    public Editable apply(@NonNull TextViewAfterTextChangeEvent textViewAfterTextChangeEvent) throws Exception {
                        return textViewAfterTextChangeEvent.editable();
                    }
                })
                .map(new Function<Editable, String>() {
                    @Override
                    public String apply(@NonNull Editable editable) throws Exception {
                        return editable.toString();
                    }
                });
    }
}
