package com.kmarlow.multidirfilesearch;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements MainScreen {

    private RecyclerView rvSearchResults;
    private EditText tvSearchInput;

    private MainPresenter presenter;
    private SearchResultsAdapter searchResultsAdapter;
    private TextView tvFound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViews();
        setupPresenter();
        setupViewObservables();
        setupRecyclerView();
    }

    private void setupViews() {
        tvSearchInput = findViewById(R.id.search_input);
        tvFound = findViewById(R.id.tvFound);
        rvSearchResults = findViewById(android.R.id.list);
    }

    private void setupPresenter() {
        ThreadSchedulers threadSchedulers = new ThreadSchedulers();
        DirectoryRepo directoryRepo = new DirectoryRepo(this);
        SearchEngine searchEngine = new SearchEngine(directoryRepo, threadSchedulers);
        PermissionManager permissionManager = new PermissionManager(this);
        presenter = new MainPresenter(this, searchEngine, threadSchedulers, permissionManager);
    }

    private void setupViewObservables() {
        presenter.onSearchTextChanges(RxTextView.afterTextChangeEvents(tvSearchInput));
    }

    private void setupRecyclerView() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rvSearchResults.setItemAnimator(new DefaultItemAnimator());
        rvSearchResults.setLayoutManager(layoutManager);
        rvSearchResults.setHasFixedSize(true);

        searchResultsAdapter = new SearchResultsAdapter(this);
        rvSearchResults.setAdapter(searchResultsAdapter);
    }

    @Override
    protected void onDestroy() {
        presenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public void updateFileList(List<FileItemViewModel> fileItemViewModels) {
        searchResultsAdapter.updateItems(fileItemViewModels);
        tvFound.setVisibility(fileItemViewModels.size() == 0 ? View.INVISIBLE : View.VISIBLE);
        tvFound.setText(getString(R.string.found_x, fileItemViewModels.size()));
    }
}
