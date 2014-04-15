package com.circlegate.liban.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.circlegate.liban.R;
import com.circlegate.liban.view.LoadingView;
import com.google.common.collect.ImmutableList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class BaseAdapterWithPaging<Item> extends BaseAdapter {
    public static final int VIEW_TYPE_ITEM = 0;
    public static final int VIEW_TYPE_LOAD_MORE = 1;
    public static final int VIEW_TYPE_COUNT = 2;

    private final Context context;
    private final LayoutInflater inflater;

    private final List<Item> items = new ArrayList<Item>();
    private int lastPageInd = -1;
    private boolean hasMorePages = false;
    private boolean wasLoadMoreItemsHandled = false;

    private OnLoadMoreItemsListener onLoadMoreItemsListener;

    public BaseAdapterWithPaging(Context context) {
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    //
    // GETTERS
    //

    public Context getContext() {
        return this.context;
    }

    public LayoutInflater getInflater() {
        return this.inflater;
    }

    public int getLastPageInd() {
        return this.lastPageInd;
    }

    public boolean getHasMorePages() {
        return this.hasMorePages;
    }

    public int getItemsCount() {
        return this.items.size();
    }

    public ImmutableList<Item> generateItems() {
        return ImmutableList.copyOf(this.items);
    }


    //
    // SETTERS
    //

    public void setItems(Collection<? extends Item> items, int pageInd, boolean hasMorePages) {
        this.items.clear();
        addItems(items, pageInd, hasMorePages);
    }

    public void addItems(Collection<? extends Item> items, int pageInd, boolean hasMorePages) {
        this.lastPageInd = pageInd;
        this.hasMorePages = hasMorePages;
        this.wasLoadMoreItemsHandled = false;
        this.items.addAll(items);
        this.notifyDataSetChanged();
    }

    public void replaceItemDontNotify(int index, Item item) {
        this.items.set(index, item);
    }

    public void replaceItemsDontNotify(List<? extends Item> items) {
        if (this.items.size() != items.size())
            throw new RuntimeException("Wrong items count!");
        this.items.clear();
        this.items.addAll(items);
    }

    public boolean clear() {
        if (this.lastPageInd != -1 ||
                this.hasMorePages != false ||
                this.wasLoadMoreItemsHandled != false ||
                this.items.size() > 0)
        {
            this.lastPageInd = -1;
            this.hasMorePages = false;
            this.wasLoadMoreItemsHandled = false;
            this.items.clear();
            notifyDataSetChanged();
            return true;
        }
        else
            return false;
    }


    //
    // OVERRIDES
    //

    @Override
    public int getCount() {
        return this.items.size() + (hasMorePages ? 1 : 0);
    }

    @Override
    public Item getItem(int position) {
        return position < this.items.size() ? this.items.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return hasMorePages && position == this.items.size() ? VIEW_TYPE_LOAD_MORE : VIEW_TYPE_ITEM;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public boolean isEnabled(int position) {
        return position < this.items.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (hasMorePages && position == this.items.size()) {
            if (!this.wasLoadMoreItemsHandled) {
                this.wasLoadMoreItemsHandled = onLoadMoreItems(this.lastPageInd + 1);
            }

            if (convertView == null) {
                convertView = newViewLoading(position, parent);
            }
            return convertView;
        }
        else {
            return getViewItem(position, convertView, parent);
        }
    }


    //
    // PROTECTED
    //

    public View newViewLoading(int position, ViewGroup parent) {
        View convertView = (LoadingView)inflater.inflate(R.layout.adapter_loading_view, parent, false);
        setupLoadingView((LoadingView)convertView);
        return convertView;
    }

    protected abstract View getViewItem(int position, View convertView, ViewGroup parent);

    protected void setupLoadingView(LoadingView loadingView) {
    }


    //
    // LISTENERS
    //

    protected boolean onLoadMoreItems(int newPageInd) {
        if (this.onLoadMoreItemsListener != null) {
            return this.onLoadMoreItemsListener.onLoadMoreItems(newPageInd);
        }
        return false;
    }

    public void setOnLoadMoreItemsListener(OnLoadMoreItemsListener l) {
        this.onLoadMoreItemsListener = l;
    }

    public interface OnLoadMoreItemsListener {
        boolean onLoadMoreItems(int newPageInd);
    }
}
