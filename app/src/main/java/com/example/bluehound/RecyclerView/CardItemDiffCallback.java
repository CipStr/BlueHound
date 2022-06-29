package com.example.bluehound.RecyclerView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.example.bluehound.CardItem;

import java.util.List;

/**
 * Utility class to compare two List<CardItem>
 *     Android suggests the use of DiffUtil.Callback to improve the performance of the RecyclerView
 *     Each time the list needs to be updated, only the changed items will be reloaded.
 */
public class CardItemDiffCallback extends DiffUtil.Callback {

    private final List<CardItem> oldCardList;
    private final List<CardItem> newCardList;

    /**
     * Constructor that takes the two lists
     * @param oldCardList the old list already displayed
     * @param newCardList the new list to display
     */
    public CardItemDiffCallback(List<CardItem> oldCardList, List<CardItem> newCardList) {
        this.oldCardList = oldCardList;
        this.newCardList = newCardList;
    }

    @Override
    public int getOldListSize() {
        return oldCardList.size();
    }

    @Override
    public int getNewListSize() {
        return newCardList.size();
    }

    /**
     *
     * @param oldItemPosition position of the old item
     * @param newItemPosition position of the new item
     * @return true if the two items are the same
     */
    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldCardList.get(oldItemPosition) == newCardList.get(newItemPosition);
    }

    /**
     *
     * @param oldItemPosition position of the old item
     * @param newItemPosition position of the new item
     * @return true if the two item have the same content
     */
    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        final CardItem oldItem = oldCardList.get(oldItemPosition);
        final CardItem newItem = newCardList.get(newItemPosition);
        return oldItem.getPlaceName().equals(newItem.getPlaceName()) &&
                oldItem.getPlaceDescription().equals(newItem.getPlaceDescription()) &&
        oldItem.getDate().equals(newItem.getDate());
    }

    /**
     * When areItemsTheSame(int, int) returns true for two items and
     * areContentsTheSame(int, int) returns false for the two items,
     * this method is called to get a payload about the change.
     * @param oldItemPosition position of the old item
     * @param newItemPosition position of the new item
     * @return an Object (it could be a bundle) that contains the changes (the only change in this case
     * id the content of the item to handle in the Adapter in the overridden method
     * onBindViewHolder(CryptoViewHolder holder, int position, List<Object> payloads)).
     *
     * It returns null by default.
     */
    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}