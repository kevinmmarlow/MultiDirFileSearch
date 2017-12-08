package com.kmarlow.multidirfilesearch;

import com.jakewharton.rxbinding2.widget.TextViewAfterTextChangeEvent;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;

/**
 Simple transformer to convert text view change events to strings
 */
public class AfterTextChangeEventTransformer implements ObservableTransformer<TextViewAfterTextChangeEvent, String> {

    @Override
    public ObservableSource<String> apply(@NonNull Observable<TextViewAfterTextChangeEvent> upstream) {
        return upstream
                .map(TextViewAfterTextChangeEvent::editable)
                .map(CharSequence::toString)
                .map(String::trim);
    }
}
