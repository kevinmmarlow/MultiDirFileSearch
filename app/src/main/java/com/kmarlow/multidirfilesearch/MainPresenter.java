package com.kmarlow.multidirfilesearch;

import android.support.annotation.NonNull;
import android.util.Log;

import com.jakewharton.rxbinding2.InitialValueObservable;
import com.jakewharton.rxbinding2.widget.TextViewAfterTextChangeEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

/**
 The presenter layer for our main view.
 This class contains all of our business logic
 and ideally should not contain any Android code.
 */
public class MainPresenter {

    private static final String TAG = MainPresenter.class.getSimpleName();

    private static final long ONEGB = 1073741824L;
    private static final long FIVEHUNDREDMB = 500000000L;
    private static final long ONEMB = 1048576L;
    private static final long FIVEHUNDREDKB = 500000L;
    private static final long ONEKB = 1024L;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final MainScreen screen;
    private final SearchEngine searchEngine;
    private final ThreadSchedulers threadSchedulers;

    public MainPresenter(MainScreen screen, SearchEngine searchEngine, ThreadSchedulers threadSchedulers) {
        this.screen = screen;
        this.searchEngine = searchEngine;
        this.threadSchedulers = threadSchedulers;
    }

    public void onSearchTextChanges(InitialValueObservable<TextViewAfterTextChangeEvent> textChangeObservable) {
        Flowable<List<FileItemViewModel>> searchResultFilesObservable = textChangeObservable
                .debounce(200, TimeUnit.MILLISECONDS)
                .compose(new AfterTextChangeEventTransformer())
                .distinctUntilChanged()
                .toFlowable(BackpressureStrategy.MISSING)
                .flatMap(new Function<String, Flowable<List<File>>>() {
                    @Override
                    public Flowable<List<File>> apply(@NonNull String trimmedQuery) throws Exception {
                        return searchEngine.startSearchDirectory(trimmedQuery);
                    }
                })
                .map(new Function<List<File>, List<FileItemViewModel>>() {
                    @Override
                    public List<FileItemViewModel> apply(@NonNull List<File> files) throws Exception {
                        return mapList(files);
                    }
                });

        compositeDisposable.add(searchResultFilesObservable
                .subscribeOn(threadSchedulers.workerThread())
                .observeOn(threadSchedulers.uiThread())
                .subscribe(new Consumer<List<FileItemViewModel>>() {
                    @Override
                    public void accept(@NonNull List<FileItemViewModel> fileItemViewModels) throws Exception {
                        screen.updateFileList(fileItemViewModels);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        Log.e(TAG, throwable.getLocalizedMessage(), throwable);
                    }
                }));
    }

    public void onDestroy() {
        compositeDisposable.clear();
    }

    private List<FileItemViewModel> mapList(List<File> files) {

        List<File> modifiable = new ArrayList<>(files);
        List<FileItemViewModel> newList = new ArrayList<>();

        for (File file : modifiable) {
            if (!file.isHidden()) {
                newList.add(map(file));
            }
        }

        return newList;
    }

    private FileItemViewModel map(File file) {
        return new FileItemViewModel(file.getName(), file, getDataSizeString(file.length()));
    }

    private String getDataSizeString(double fileSize) {
        String returnformat;
        if (fileSize > FIVEHUNDREDMB) { // if greater then 500mb
            returnformat = String.format("%1$.2f GB", fileSize / ONEGB);
        } else if (fileSize > FIVEHUNDREDKB) {
            returnformat = String.format("%1$.2f MB", fileSize / ONEMB);
        } else if (fileSize > ONEKB) {
            returnformat = String.format("%1$.2f KB", fileSize / ONEKB);
        } else {
            returnformat = (int) fileSize + " Bytes";
        }
        return returnformat;
    }
}
