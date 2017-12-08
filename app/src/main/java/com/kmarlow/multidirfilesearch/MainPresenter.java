package com.kmarlow.multidirfilesearch;

import android.Manifest;

import com.jakewharton.rxbinding2.InitialValueObservable;
import com.jakewharton.rxbinding2.widget.TextViewAfterTextChangeEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import timber.log.Timber;

/**
 The presenter layer for our main view.
 This class contains all of our business logic
 and ideally should not contain any Android code.
 */
public class MainPresenter {
    private static final int READ_PERMISSION_REQUEST_CODE = 1001;

    private static final long ONEGB = 1024L * 1024L * 1024L;
    private static final long FIVEHUNDREDMB = ONEGB / 2L;
    private static final long ONEMB = 1024L * 1024L;
    private static final long FIVEHUNDREDKB = ONEMB / 2L;
    private static final long ONEKB = 1024L;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final MainScreen screen;
    private final SearchEngine searchEngine;
    private final ThreadSchedulers threadSchedulers;
    private final PermissionManager permissionManager;
    private Disposable searchDisposable;

    public MainPresenter(MainScreen screen, SearchEngine searchEngine, ThreadSchedulers threadSchedulers, PermissionManager permissionManager) {
        this.screen = screen;
        this.searchEngine = searchEngine;
        this.threadSchedulers = threadSchedulers;
        this.permissionManager = permissionManager;
    }

    public void onSearchTextChanges(InitialValueObservable<TextViewAfterTextChangeEvent> textChangeObservable) {
        if (!permissionManager.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            screen.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSION_REQUEST_CODE);
            return;
        }
        compositeDisposable.add(textChangeObservable
                .debounce(200, TimeUnit.MILLISECONDS)
                .compose(new AfterTextChangeEventTransformer())
                .observeOn(threadSchedulers.uiThread())
                .subscribe(keyword -> {
                    if (keyword.isEmpty()) {
                        Timber.d("Search field is empty. Clearing screen");
                        disposeSearch();
                        screen.updateFileList(Collections.emptyList());
                    } else {
                        Timber.d("Beginning search for '%s'", keyword);
                        search(keyword);
                    }
                }, Timber::e));
    }

    private void search(String keyword) {
        disposeSearch();
        searchDisposable = searchEngine.startSearchDirectory(keyword)
                .map(this::mapList)
                .scan(new ArrayList<>(), accumulateSortList(comparator()))
                .map(Collections::unmodifiableList)
                .subscribeOn(threadSchedulers.ioThread())
                .observeOn(threadSchedulers.uiThread())
                .subscribe(screen::updateFileList, Timber::e);
    }

    private void disposeSearch() {
        if (searchDisposable != null && !searchDisposable.isDisposed()) {
            searchDisposable.dispose();
        }
    }

    private <T> BiFunction<List<T>, List<T>, List<T>> accumulateSortList(final Comparator<T> comparator) {
        return (left, right) -> {
            left.addAll(right);
            Collections.sort(left, comparator);
            return left;
        };
    }

    private Comparator<FileItemViewModel> comparator() {
        return (file1, file2) -> file1.getName().compareTo(file2.getName());
    }

    public void onDestroy() {
        disposeSearch();
        compositeDisposable.clear();
    }

    private List<FileItemViewModel> mapList(List<File> files) {
        List<File> modifiable = new ArrayList<>(files);
        List<FileItemViewModel> newList = new ArrayList<>();

        for (File file : modifiable) {
            newList.add(map(file));
        }

        return newList;
    }

    private FileItemViewModel map(File file) {
        return new FileItemViewModel(file.getName(), file, getDataSizeString(file.length()));
    }

    private String getDataSizeString(double fileSize) {
        String returnFormat;
        if (fileSize > FIVEHUNDREDMB) { // if greater then 500mb
            returnFormat = String.format("%1$.2f GB", fileSize / ONEGB);
        } else if (fileSize > FIVEHUNDREDKB) {
            returnFormat = String.format("%1$.2f MB", fileSize / ONEMB);
        } else if (fileSize > ONEKB) {
            returnFormat = String.format("%1$.2f KB", fileSize / ONEKB);
        } else {
            returnFormat = (int) fileSize + " Bytes";
        }
        return returnFormat;
    }
}
