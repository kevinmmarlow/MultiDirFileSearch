package com.kmarlow.multidirfilesearch;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.regex.PatternSyntaxException;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.PublishSubject;

import static android.content.pm.PackageManager.MATCH_ALL;

/**
 TODO: Add Class Header
 */
public class SearchEngine {
    private static final String TAG = SearchEngine.class.getCanonicalName();
    private final PublishSubject<String> keywords = PublishSubject.create();
    private final PublishSubject<Object> cancelEvents = PublishSubject.create();
    private final DirectoryRepo directoryRepo;
    private final ThreadSchedulers threadSchedulers;

    public SearchEngine(DirectoryRepo directoryRepo, ThreadSchedulers threadSchedulers) {
        this.directoryRepo = directoryRepo;
        this.threadSchedulers = threadSchedulers;
    }

    /**
     Given a keyword, pull every searchable directory, then map each one into a search of the keyword.

     Merge these searches back into one stream. Accumulate and sort the list.

     Whenever a new keyword is entered, cancel previous searches via takeUntil.
     */
    public Flowable<List<File>> startSearchDirectory(final String keyword) {
        keywords.onNext(keyword);
        return Flowable.merge(
                Flowable.fromIterable(directoryRepo.directoriesToSearch())
                        .map(new Function<File, Flowable<List<File>>>() {
                            @Override
                            public Flowable<List<File>> apply(@NonNull File directory) throws Exception {
                                return searchDirectory(directory, keyword);
                            }
                        }))
                .scan(Collections.<File>emptyList(), accumulateSortList(comparator()))
                .takeUntil(cancelEvents().toFlowable(BackpressureStrategy.LATEST))
                .takeUntil(keywords.toFlowable(BackpressureStrategy.LATEST));
    }

    public void cancelSearch() {
        cancelEvents.onNext(new Object());
    }

    private Observable<Object> cancelEvents() {
        return cancelEvents.hide();
    }

    private Comparator<File> comparator() {
        return new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                return file1.getName().compareTo(file2.getName());
            }
        };
    }

    private Flowable<List<File>> searchDirectory(final File folderDirectory, final String keyword) {
        return Flowable.create(new FlowableOnSubscribe<List<File>>() {
            @Override
            public void subscribe(@NonNull FlowableEmitter<List<File>> emitter) throws Exception {
                searchDirectory(folderDirectory, keyword, emitter);
                emitter.onComplete();
            }
        }, BackpressureStrategy.BUFFER)
                .takeUntil(cancelEvents().toFlowable(BackpressureStrategy.LATEST))
                .takeUntil(keywords.toFlowable(BackpressureStrategy.LATEST))
                .subscribeOn(threadSchedulers.subscribeOn());
    }

    private File[] getFilesFromDirectory(File directory, final String keyword) {
        File[] files;
        if (TextUtils.isEmpty(keyword)) {
            files = directory.listFiles();
        } else {
            try {
                FileFilter filter = new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory() || file.getName().toLowerCase()
                                .matches(MATCH_ALL + keyword.toLowerCase() + MATCH_ALL);
                    }
                };
                files = directory.listFiles(filter);
            } catch (PatternSyntaxException e) {
                files = null;
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        }
        return files;
    }

    private void searchDirectory(File directory, String keyword,
                                 final FlowableEmitter<List<File>> emitter) {
        if (!directory.canRead()) {
            return;
        }

        final Queue<File> directories = new ArrayDeque<>(100);
        directories.add(directory);

        while (!directories.isEmpty()) {
            File dir = directories.poll();
            File[] files = getFilesFromDirectory(dir, keyword);
            if (files != null && files.length > 0) {
                Observable.fromArray(files)
                        .filter(new Predicate<File>() {
                            @Override
                            public boolean test(@NonNull File file) throws Exception {
                                return isFile(file);
                            }
                        })
                        .toList()
                        .subscribe(new Consumer<List<File>>() {
                            @Override
                            public void accept(@NonNull List<File> files) throws Exception {
                                emitter.onNext(files);
                            }
                        }, logError());
                Observable.fromArray(files)
                        .filter(new Predicate<File>() {
                            @Override
                            public boolean test(@NonNull File file) throws Exception {
                                return isDirectoryAndCanRead(file);
                            }
                        })
                        .subscribe(new Consumer<File>() {
                            @Override
                            public void accept(@NonNull File file) throws Exception {
                                directories.add(file);
                            }
                        }, logError());
            }
        }
    }

    @NonNull
    private Consumer<Throwable> logError() {
        return new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable throwable) throws Exception {
                Log.e(TAG, throwable.getLocalizedMessage(), throwable);
            }
        };
    }

    private boolean isFile(File file) {
        return !file.isHidden() && !file.isDirectory();
    }

    private boolean isDirectoryAndCanRead(File folderDirectory) {
        return folderDirectory.isDirectory() && folderDirectory.canRead();
    }

    private BiFunction<List<File>, List<File>, List<File>> accumulateSortList(final Comparator<File> comparator) {
        return new BiFunction<List<File>, List<File>, List<File>>() {
            @Override
            public List<File> apply(@NonNull List<File> left, @NonNull List<File> right) throws Exception {
                List<File> list = new ArrayList<>(left.size() + right.size());
                list.addAll(left);
                list.addAll(right);
                Collections.sort(list, comparator);
                return list;
            }
        };
    }
}
