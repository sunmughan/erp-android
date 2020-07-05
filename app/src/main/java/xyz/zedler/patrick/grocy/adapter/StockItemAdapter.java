package xyz.zedler.patrick.grocy.adapter;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.view.FilterChip;
import xyz.zedler.patrick.grocy.view.InputChip;

public class StockItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final static String TAG = StockItemAdapter.class.getSimpleName();

    private Context context;
    private ArrayList<GroupedListItem> groupedListItems;
    private ArrayList<String> shoppingListProductIds;
    private HashMap<Integer, QuantityUnit> quantityUnits;
    private StockItemAdapterListener listener;
    private int daysExpiringSoon;
    private String sortMode;
    private boolean showDateTracking;
    private ArrayList<FilterChip> filtersScrollFirst; // first row
    private ArrayList<InputChip> filtersScrollSecond; // second row

    public StockItemAdapter(
            Context context,
            ArrayList<StockItem> stockItems,
            HashMap<Integer, QuantityUnit> quantityUnits,
            ArrayList<String> shoppingListProductIds,
            int daysExpiringSoon,
            String sortMode,
            boolean showDateTracking,
            StockItemAdapterListener listener,
            ArrayList<FilterChip> filtersScrollFirst,
            ArrayList<InputChip> filtersScrollSecond
    ) {
        this.context = context;
        this.quantityUnits = quantityUnits;
        this.shoppingListProductIds = shoppingListProductIds;
        this.daysExpiringSoon = daysExpiringSoon;
        this.sortMode = sortMode;
        this.showDateTracking = showDateTracking;
        this.listener = listener;
        this.filtersScrollFirst = filtersScrollFirst;
        this.filtersScrollSecond = filtersScrollSecond;

        groupedListItems = new ArrayList<>();
        groupedListItems.add(new FilterRowFirstItem());
        groupedListItems.add(new FilterRowSecondItem());
        groupedListItems.addAll(stockItems);
    }

    public static class ViewHolderItem extends RecyclerView.ViewHolder {
        private LinearLayout linearLayoutItemContainer, linearLayoutDays;
        private TextView textViewName, textViewAmount, textViewDays;
        private View iconIsOnShoppingList;

        public ViewHolderItem(View view) {
            super(view);

            linearLayoutItemContainer = view.findViewById(R.id.linear_stock_item_container);
            linearLayoutDays = view.findViewById(R.id.linear_stock_item_days);
            textViewName = view.findViewById(R.id.text_stock_item_name);
            textViewAmount = view.findViewById(R.id.text_stock_item_amount);
            textViewDays = view.findViewById(R.id.text_stock_item_days);
            iconIsOnShoppingList = view.findViewById(R.id.view_stock_item_on_shopping_list);
        }
    }

    public static class ViewHolderFilterFirst extends RecyclerView.ViewHolder {
        private LinearLayout linearLayoutFilterContainer;

        public ViewHolderFilterFirst(View view) {
            super(view);

            linearLayoutFilterContainer = view.findViewById(R.id.linear_filter_container);
        }
    }

    public static class ViewHolderFilterSecond extends RecyclerView.ViewHolder {
        private LinearLayout linearLayoutFilterContainer;

        public ViewHolderFilterSecond(View view) {
            super(view);

            linearLayoutFilterContainer = view.findViewById(R.id.linear_filter_container);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == GroupedListItem.TYPE_FILTER_ROW_FIRST) {
            //inflate your layout and pass it to view holder
            return new ViewHolderFilterFirst(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.view_filter_scroll,
                            parent,
                            false
                    )
            );
        } else if (viewType == GroupedListItem.TYPE_FILTER_ROW_SECOND) {
            //inflate your layout and pass it to view holder
            return new ViewHolderFilterSecond(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.view_filter_scroll,
                            parent,
                            false
                    )
            );
        } else {
            return new ViewHolderItem(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.row_stock_item,
                            parent,
                            false
                    )
            );
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof ViewHolderFilterFirst) {
            ViewHolderFilterFirst holder = (ViewHolderFilterFirst) viewHolder;
            holder.linearLayoutFilterContainer.removeAllViews();
            for(FilterChip filterChip : filtersScrollFirst) {
                holder.linearLayoutFilterContainer.addView(filterChip);
            }
            return;
        } else if (viewHolder instanceof ViewHolderFilterSecond) {
            ViewHolderFilterSecond holder = (ViewHolderFilterSecond) viewHolder;
            holder.linearLayoutFilterContainer.removeAllViews();
            for(InputChip inputChip : filtersScrollSecond) {
                holder.linearLayoutFilterContainer.addView(inputChip);
            }
            return;
        }

        ViewHolderItem holder = (ViewHolderItem) viewHolder;
        StockItem stockItem = (StockItem) groupedListItems.get(viewHolder.getAdapterPosition());

        // NAME

        holder.textViewName.setText(stockItem.getProduct().getName());

        // IS ON SHOPPING LIST

        if(shoppingListProductIds.contains(String.valueOf(stockItem.getProduct().getId()))) {
            holder.iconIsOnShoppingList.setVisibility(View.VISIBLE);
        } else {
            holder.iconIsOnShoppingList.setVisibility(View.GONE);
        }

        // AMOUNT

        QuantityUnit quantityUnit = quantityUnits.get(stockItem.getProduct().getQuIdStock());

        String unit = null;
        if(quantityUnit != null && stockItem.getAmount() == 1) {
            unit = quantityUnit.getName();
        } else if (quantityUnit != null) {
            unit = quantityUnit.getNamePlural();
        }
        StringBuilder stringBuilderAmount = new StringBuilder(
                context.getString(
                        R.string.subtitle_amount,
                        NumUtil.trim(stockItem.getAmount()),
                        unit
                )
        );
        if(stockItem.getAmountOpened() > 0) {
            stringBuilderAmount.append(" ");
            stringBuilderAmount.append(
                    context.getString(
                            R.string.subtitle_amount_opened,
                            NumUtil.trim(stockItem.getAmountOpened())
                    )
            );
        }
        // aggregated amount
        if(stockItem.getIsAggregatedAmount() == 1) {
            if(quantityUnit != null && stockItem.getAmountAggregated() == 1) {
                unit = quantityUnit.getName();
            } else if (quantityUnit != null) {
                unit = quantityUnit.getNamePlural();
            }
            stringBuilderAmount.append("  ∑ ");
            stringBuilderAmount.append(
                    context.getString(
                            R.string.subtitle_amount,
                            NumUtil.trim(stockItem.getAmountAggregated()),
                            unit
                    )
            );
        }
        holder.textViewAmount.setText(stringBuilderAmount);
        if(stockItem.getAmount() < stockItem.getProduct().getMinStockAmount()) {
            holder.textViewAmount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_medium)
            );
            holder.textViewAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.retro_blue_fg)
            );
        } else {
            holder.textViewAmount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_regular)
            );
            holder.textViewAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.on_background_secondary)
            );
        }

        // BEST BEFORE

        String date = stockItem.getBestBeforeDate();
        String days = null;
        boolean colorDays = false;
        if(date != null) days = String.valueOf(DateUtil.getDaysFromNow(date));

        if(!showDateTracking) {
            holder.linearLayoutDays.setVisibility(View.GONE);
        } else if(days != null && (sortMode.equals(Constants.STOCK.SORT.BBD)
                || Integer.parseInt(days) <= daysExpiringSoon
                && !date.equals(Constants.DATE.NEVER_EXPIRES))
        ) {
            holder.linearLayoutDays.setVisibility(View.VISIBLE);
            holder.textViewDays.setText(new DateUtil(context).getHumanForDaysFromNow(date));
            if(Integer.parseInt(days) <= 5) colorDays = true;
        } else {
            holder.linearLayoutDays.setVisibility(View.GONE);
            holder.textViewDays.setText(null);
        }

        if(colorDays) {
            holder.textViewDays.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_medium)
            );
            holder.textViewDays.setTextColor(
                    ContextCompat.getColor(
                            context, Integer.parseInt(days) < 0
                                    ? R.color.retro_red_fg
                                    : R.color.retro_yellow_fg
                    )
            );
        } else {
            holder.textViewDays.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_regular)
            );
            holder.textViewDays.setTextColor(
                    ContextCompat.getColor(context, R.color.on_background_secondary)
            );
        }

        // CONTAINER

        holder.linearLayoutItemContainer.setOnClickListener(
                view -> listener.onItemRowClicked(holder.getAdapterPosition())
        );
    }

    public void setSortMode(String sortMode) {
        this.sortMode = sortMode;
    }

    @Override
    public long getItemId(int position) {
        GroupedListItem groupedListItem = groupedListItems.get(position);
        if(groupedListItem instanceof StockItem) {
            return ((StockItem) groupedListItems.get(position)).getProductId();
        } else if(groupedListItem instanceof FilterRowFirstItem) {
            return -1;
        } else {
            return -2;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return groupedListItems.get(position).getType();
    }

    @Override
    public int getItemCount() {
        return groupedListItems != null ? groupedListItems.size() : 0;
    }

    public interface StockItemAdapterListener {
        void onItemRowClicked(int position);
    }

    private static class FilterRowFirstItem extends GroupedListItem {
        @Override
        public int getType() {
            return GroupedListItem.TYPE_FILTER_ROW_FIRST;
        }
    }

    private static class FilterRowSecondItem extends GroupedListItem {
        @Override
        public int getType() {
            return GroupedListItem.TYPE_FILTER_ROW_SECOND;
        }
    }

    public void updateList(ArrayList<StockItem> newList, ArrayList<String> shoppingListProductIds) {
        this.shoppingListProductIds = shoppingListProductIds;

        ArrayList<GroupedListItem> newGroupedListItems = new ArrayList<>();
        newGroupedListItems.add(new FilterRowFirstItem());
        newGroupedListItems.add(new FilterRowSecondItem());
        newGroupedListItems.addAll(newList);

        DiffCallback diffCallback = new DiffCallback(
                newGroupedListItems,
                this.groupedListItems
        );
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        groupedListItems = newGroupedListItems;
        diffResult.dispatchUpdatesTo(this);
    }

    static class DiffCallback extends DiffUtil.Callback {

        ArrayList<GroupedListItem> oldItems;
        ArrayList<GroupedListItem> newItems;

        public DiffCallback(
                ArrayList<GroupedListItem> newItems,
                ArrayList<GroupedListItem> oldItems
        ) {
            this.newItems = newItems;
            this.oldItems = oldItems;
        }

        @Override
        public int getOldListSize() {
            return oldItems.size();
        }

        @Override
        public int getNewListSize() {
            return newItems.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return compare(oldItemPosition, newItemPosition, false);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return compare(oldItemPosition, newItemPosition, true);
        }

        private boolean compare(int oldItemPos, int newItemPos, boolean compareContent) {
            int oldItemType = oldItems.get(oldItemPos).getType();
            int newItemType = newItems.get(newItemPos).getType();
            if(oldItemType != newItemType) return false;
            if(oldItemType == GroupedListItem.TYPE_ENTRY) {
                StockItem newItem = (StockItem) newItems.get(newItemPos);
                StockItem oldItem = (StockItem) oldItems.get(oldItemPos);
                return compareContent
                        ? newItem.equals(oldItem)
                        : newItem.getProductId() == oldItem.getProductId();
            } else {
                return true; // Filter row items are always on top
            }
        }
    }
}
