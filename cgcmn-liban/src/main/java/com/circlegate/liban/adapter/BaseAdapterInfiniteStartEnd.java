package com.circlegate.liban.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.circlegate.liban.R;
import com.circlegate.liban.view.CustomListView;
import com.circlegate.liban.view.LoadingView;
import com.google.common.collect.ImmutableList;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class BaseAdapterInfiniteStartEnd<Item> extends BaseAdapter {
    public static final int SCROLL_NONE = 0;
    public static final int SCROLL_TO_START = 1;
    public static final int SCROLL_TO_END = 2;

    public static final int VIEW_TYPE_ITEM = 0;
    public static final int VIEW_TYPE_LOAD_MORE = 1;
    public static final int VIEW_TYPE_COUNT = 2;

    private final CustomListView listView; // potrebujeme jenom kvuli setSelectionFromTop u pridavani polozek pred start
    private final boolean hasStartMoreBtn;
    private final boolean showNoMoreStartEndButtons; // pokud je false, a soucasne je hasMoreItemsStart resp. hasMoreItemsEnd, tak se proste na dane strane adapteru nezobrazuje zadna polozka navic
    private final Context context;
    private final LayoutInflater inflater;
    private final String textLoadMoreItemsStart;    // jenom pokud hasStartMoreBtn == true
    private final String textLoadingMoreItemsStart; // jenom pokud hasStartMoreBtn == true
    private final String textLoadingMoreItemsEnd;
    private final String textNoMoreItemsStart;      // jenom pokud je showNoMoreStartEndButtons == true
    private final String textNoMoreItemsEnd;        // jenom pokud je showNoMoreStartEndButtons == true

    private final List<Item> items = new ArrayList<Item>();
    private boolean hasMoreItemsStart;
    private boolean isLoadingMoreItemsStart;
    private boolean hasMoreItemsEnd;
    private boolean wasLoadMoreItemsEndHandled;

    private OnLoadMoreItemsListener onLoadMoreItemsListener;

    public BaseAdapterInfiniteStartEnd(CustomListView listView,
                                       String textLoadMoreItemsStart,
                                       String textLoadingMoreItemsStart, String textLoadingMoreItemsEnd,
                                       String textNoMoreItemsStart, String textNoMoreItemsEnd)
    {
        this(listView, true, true, textLoadMoreItemsStart, textLoadingMoreItemsStart, textLoadingMoreItemsEnd, textNoMoreItemsStart, textNoMoreItemsEnd);
    }

    /**
     * Verze konstruktoru bez tlacitka pro nacteni predchozich
     * @param listView
     * @param textLoadingMoreItemsEnd
     * @param textNoMoreItemsStart
     * @param textNoMoreItemsEnd
     */
    public BaseAdapterInfiniteStartEnd(CustomListView listView,
                                       String textLoadingMoreItemsEnd,
                                       String textNoMoreItemsStart, String textNoMoreItemsEnd)
    {
        this(listView, false, true, null, null, textLoadingMoreItemsEnd, textNoMoreItemsStart, textNoMoreItemsEnd);
    }

    public BaseAdapterInfiniteStartEnd(CustomListView listView,
                                        boolean hasStartMoreBtn,
                                        boolean showNoMoreStartEndButtons,
                                        String textLoadMoreItemsStart,
                                        String textLoadingMoreItemsStart, String textLoadingMoreItemsEnd,
                                        String textNoMoreItemsStart, String textNoMoreItemsEnd)
    {
        this.listView = listView;
        this.hasStartMoreBtn = hasStartMoreBtn;
        this.showNoMoreStartEndButtons = showNoMoreStartEndButtons;
        this.context = listView.getContext();
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.textLoadMoreItemsStart = textLoadMoreItemsStart;
        this.textLoadingMoreItemsStart = textLoadingMoreItemsStart;
        this.textLoadingMoreItemsEnd = textLoadingMoreItemsEnd;
        this.textNoMoreItemsStart = textNoMoreItemsStart;
        this.textNoMoreItemsEnd = textNoMoreItemsEnd;
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

    public boolean getHasMoreItemsStart() {
        return hasMoreItemsStart;
    }

    public boolean getHasMoreItemsEnd() {
        return hasMoreItemsEnd;
    }

    public int getItemsCount() {
        return this.items.size();
    }

    public Item getItemAt(int index) {
        return this.items.get(index);
    }

    public int getItemIndByPosition(int position) {
        return position - getStartMoreBtnsCount();
    }

    public int getItemPositionByInd(int index) {
        return index + getStartMoreBtnsCount();
    }

    public ImmutableList<Item> generateItems() {
        return ImmutableList.copyOf(items);
    }

    public boolean isStartItem(int position) {
        return position < getStartMoreBtnsCount();
    }

    public boolean isEndItem(int position) {
        return position >= items.size() + getStartMoreBtnsCount();
    }

    public boolean isStartEndItem(int position) {
        return isStartItem(position) || isEndItem(position);
    }

    public int getStartMoreBtnsCount() {
        return (items.size() > 0 && hasStartMoreBtn && (hasMoreItemsStart || showNoMoreStartEndButtons)) ? 1 : 0;
    }

    public int getEndMoreBtnsCount() {
        return (items.size() > 0 && (hasMoreItemsEnd || showNoMoreStartEndButtons)) ? 1 : 0;
    }


    //
    // SETTERS
    //

    public void setItems(Collection<? extends Item> items, int scrollType) {
        setItems(items, scrollType, true, true);
    }

    public void setItems(Collection<? extends Item> items, int scrollType, boolean hasMoreItemsStart, boolean hasMoreItemsEnd) {
        if (items.size() == 0) {
            clear();
        }
        else {
            this.items.clear();
            this.hasMoreItemsStart = hasMoreItemsStart;
            this.isLoadingMoreItemsStart = false;
            this.hasMoreItemsEnd = hasMoreItemsEnd;
            this.wasLoadMoreItemsEndHandled = false;
            this.items.addAll(0, items);

            this.notifyDataSetChanged();

            if (scrollType == SCROLL_TO_START) {
                this.listView.setSelectionFromTop(listView.getHeaderViewsCount() + getStartMoreBtnsCount(), this.listView.getDividerHeight());
            }
            else if (scrollType == SCROLL_TO_END)
                this.listView.setSelection(listView.getHeaderViewsCount() + items.size() - getStartMoreBtnsCount());
        }
    }

    public void addItemsStart(final Collection<? extends Item> items) {
        addItemsStart(items, false);
    }

    public void addItemsStart(final Collection<? extends Item> items, boolean removeFirstOldItem) {
        int position;
        final int headerViewsCount = listView.getHeaderViewsCount();
        for (position = listView.getFirstVisiblePosition(); position < listView.getLastVisiblePosition(); position++) {
            if (position > headerViewsCount + getStartMoreBtnsCount() + (removeFirstOldItem ? 1 : 0))
                break;
        }
        final int offset = listView.getVisiblePositionOffset(position - listView.getFirstVisiblePosition());

        this.hasMoreItemsStart = true;
        this.isLoadingMoreItemsStart = false;
        if (removeFirstOldItem)
            this.items.remove(0);
        this.items.addAll(0, items);

        this.notifyDataSetChanged();

        listView.setSelectionFromTop(position + items.size() + (removeFirstOldItem ? -1 : 0), offset - listView.getPaddingTop());// + listView.getPaddingTop() - listView.getDividerHeight());

        if (hasStartMoreBtn || position != headerViewsCount + 1 || items.size() == 0) {
            // nic
        }
        else if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
            listView.smoothScrollToPositionFromTop(items.size() - 1 + headerViewsCount + getStartMoreBtnsCount(), listView.getPaddingTop() - listView.getDividerHeight(), 500);
        }
        else {
            // Radsi nic...
            //listView.scrollToPositionFromTop(items.size() - 1 + headerViewsCount + getStartMoreBtnsCount(), listView.getPaddingTop() - listView.getDividerHeight(), 500);
        }
    }

    public void addItemsEnd(Collection<? extends Item> items) {
        this.hasMoreItemsEnd = true;
        this.wasLoadMoreItemsEndHandled = false;
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

    public void clear() {
        if (items.size() != 0 ||
                hasMoreItemsStart ||
                isLoadingMoreItemsStart ||
                hasMoreItemsEnd ||
                wasLoadMoreItemsEndHandled)
        {
            this.items.clear();
            this.hasMoreItemsStart = false;
            this.isLoadingMoreItemsStart = false;
            this.hasMoreItemsEnd = false;
            this.wasLoadMoreItemsEndHandled = false;

            notifyDataSetChanged();
        }
    }

    public void setNoMoreItemsStart(boolean notify) {
        if (items.size() > 0 ) {
            this.hasMoreItemsStart = false;
            this.isLoadingMoreItemsStart = false;

            if (notify)
                notifyDataSetChanged();
        }
    }

    public void setNoMoreItemsEnd(boolean notify) {
        if (items.size() > 0 ) {
            this.wasLoadMoreItemsEndHandled = true;
            this.hasMoreItemsEnd = false;

            if (notify)
                notifyDataSetChanged();
        }
    }

    public boolean loadMoreItemsStart() {
        if (!isLoadingMoreItemsStart && onLoadMoreItemsStart()) {
            this.isLoadingMoreItemsStart = true;
            this.hasMoreItemsStart = true;
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
        return getStartMoreBtnsCount() + this.items.size() + getEndMoreBtnsCount();
    }

    @Override
    public Item getItem(int position) {
        return isStartEndItem(position) ? null : this.items.get(getItemIndByPosition(position));
    }

    @Override
    public long getItemId(int position) {
        if (isStartItem(position))
            return Long.MIN_VALUE;
        else if (isEndItem(position))
            return Long.MAX_VALUE;
        else
            return getItemIdAtIndex(getItemIndByPosition(position));
    }

    @Override
    public int getItemViewType(int position) {
        return isStartEndItem(position) ? VIEW_TYPE_LOAD_MORE : VIEW_TYPE_ITEM;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public boolean isEnabled(int position) {
        return !isStartEndItem(position) || (isStartItem(position) && hasMoreItemsStart);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (isStartEndItem(position)) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.adapter_loading_view, parent, false);
            }

            LoadingView v = (LoadingView)convertView;

            if (position == 0) {
                if (this.isLoadingMoreItemsStart) {
                    v.setProgresBarVisible(true);
                    v.setText(textLoadingMoreItemsStart);
                }
                else if (this.hasMoreItemsStart) {
                    v.setProgresBarVisible(false);
                    v.setText(textLoadMoreItemsStart);
                }
                else {
                    v.setProgresBarVisible(false);
                    v.setText(textNoMoreItemsStart);
                }
                setupLoadingView((LoadingView)convertView, true);
                return convertView;
            }
            else {
                if (!this.wasLoadMoreItemsEndHandled) {
                    this.wasLoadMoreItemsEndHandled = true;
                    this.hasMoreItemsEnd = onLoadMoreItemsEnd();
                }

                if (this.hasMoreItemsEnd) {
                    v.setProgresBarVisible(true);
                    v.setText(textLoadingMoreItemsEnd);
                }
                else {
                    v.setProgresBarVisible(false);
                    v.setText(textNoMoreItemsEnd);
                }
                setupLoadingView((LoadingView)convertView, false);
                return convertView;
            }
        }
        else {
            return getViewItem(position, convertView, parent);
        }
    }


    //
    // PROTECTED
    //

    protected abstract View getViewItem(int position, View convertView, ViewGroup parent);

    protected long getItemIdAtIndex(int index) {
        return 0;
    }

    protected void setupLoadingView(LoadingView loadingView, boolean isStart) {
    }


    //
    // LISTENERS
    //

    protected boolean onLoadMoreItemsStart() {
        return this.onLoadMoreItemsListener != null && this.onLoadMoreItemsListener.onLoadMoreItemsStart();
    }

    protected boolean onLoadMoreItemsEnd() {
        return this.onLoadMoreItemsListener != null && this.onLoadMoreItemsListener.onLoadMoreItemsEnd();
    }

    public void setOnLoadMoreItemsListener(OnLoadMoreItemsListener l) {
        this.onLoadMoreItemsListener = l;
    }

    public interface OnLoadMoreItemsListener {
        boolean onLoadMoreItemsStart();
        boolean onLoadMoreItemsEnd();
    }
}
