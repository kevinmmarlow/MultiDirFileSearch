package com.kmarlow.multidirfilesearch;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 The results adapter used with the RecyclerView. Responsible for showing the search results.
 */
public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    private final Context context;
    private final List<FileItemViewModel> fileItemViewModels = new ArrayList<>();

    public SearchResultsAdapter(Context context) {
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.row_item_search_result, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindTo(fileItemViewModels.get(position));
    }

    @Override
    public int getItemCount() {
        return fileItemViewModels.size();
    }

    public synchronized void updateItems(List<FileItemViewModel> viewModels) {
        fileItemViewModels.clear();
        fileItemViewModels.addAll(viewModels);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvFileName;
        private final TextView tvFileSize;

        public ViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(android.R.id.text1);
            tvFileSize = itemView.findViewById(android.R.id.text2);
        }

        public void bindTo(final FileItemViewModel viewModel) {
            tvFileName.setText(viewModel.getName());
            tvFileSize.setText(viewModel.getSize());

            itemView.setOnClickListener(v -> {
                String path = viewModel.getFile().getAbsolutePath();
                Toast.makeText(v.getContext(), path, Toast.LENGTH_SHORT).show();
            });
        }
    }
}
