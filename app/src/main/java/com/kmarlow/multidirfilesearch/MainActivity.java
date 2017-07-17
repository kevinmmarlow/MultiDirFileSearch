package com.kmarlow.multidirfilesearch;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

import com.jakewharton.rxbinding2.widget.RxTextView;
public class MainActivity extends AppCompatActivity implements MainScreen {

    private RecyclerView rvSearchResults;
    private EditText tvSearchInput;

    private MainPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViews();
        setupPresenter();
        setupViewObservables();
    }

    private void setupViews() {
        tvSearchInput = (EditText) findViewById(R.id.search_input);
        rvSearchResults = (RecyclerView) findViewById(android.R.id.list);
    }

    private void setupPresenter() {
        presenter = new MainPresenter(this);
    }

    private void setupViewObservables() {
        presenter.onSearchTextChanges(RxTextView.afterTextChangeEvents(tvSearchInput));
    }

    @Override
    protected void onDestroy() {
        presenter.onDestroy();
        super.onDestroy();
    }
}
