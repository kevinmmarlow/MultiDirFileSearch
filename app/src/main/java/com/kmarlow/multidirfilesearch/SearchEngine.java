package com.kmarlow.multidirfilesearch;

import android.text.TextUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.regex.PatternSyntaxException;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import timber.log.Timber;

/**
 The main search engine. This class is responsible for taking a keyword and
 finding matches against it in all readable directories. It emits the results in
 in batches based on {@link #SEARCH_RESULT_BATCH_TIME} milliseconds.

 Note: We batch because we do not want to update the UI too frequently.
 */
public class SearchEngine {
    private static final String MATCH_ALL = ".*";

    private static final int SEARCH_RESULT_BATCH_TIME = 150;
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
        return Flowable.fromIterable(directoryRepo.directoriesToSearch())
                .flatMap(directory -> searchDirectory(directory, keyword))
                .buffer(SEARCH_RESULT_BATCH_TIME, TimeUnit.MILLISECONDS);
    }

    private Flowable<File> searchDirectory(final File folderDirectory, final String keyword) {
        return Flowable.create((FlowableOnSubscribe<File>) emitter -> {
            searchDirectory(folderDirectory, keyword, emitter);
            emitter.onComplete();
        }, BackpressureStrategy.BUFFER)
                .subscribeOn(threadSchedulers.ioThread());
    }

    private void searchDirectory(File directory, String keyword,
                                 final FlowableEmitter<File> emitter) {
        if (!directory.canRead()) {
            return;
        }

        final Queue<File> directories = new ArrayDeque<>(100);
        directories.add(directory);

        while (!directories.isEmpty()) {
            File dir = directories.poll();
            File[] files = getFilesFromDirectory(dir, keyword);
            if (files != null && files.length > 0) {

                for (File file : files) {
                    if (isVisibleFile(file)) {
                        emitter.onNext(file);
                    } else if (isDirectoryAndCanRead(file)) {
                        directories.add(file);
                    }
                }
            }
        }
    }

    private boolean isVisibleFile(File file) {
        return !file.isHidden() && !file.isDirectory();
    }

    private boolean isDirectoryAndCanRead(File folderDirectory) {
        return folderDirectory.isDirectory() && folderDirectory.canRead();
    }

    private File[] getFilesFromDirectory(File directory, final String keyword) {
        File[] files;
        if (TextUtils.isEmpty(keyword)) {
            files = directory.listFiles();
        } else {
            try {
                FileFilter filter = file -> file.isDirectory() || file.getName().toLowerCase(Locale.getDefault())
                        .matches(MATCH_ALL + keyword.toLowerCase(Locale.getDefault()) + MATCH_ALL);

                files = directory.listFiles(filter);
            } catch (PatternSyntaxException e) {
                files = new File[0];
                Timber.e(e);
            }
        }
        return files;
    }
}
