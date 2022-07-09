package com.example.bluehound.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluehound.CardItem;
import com.example.bluehound.R;
import com.example.bluehound.Utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter linked to the RecyclerView of the homePage
 */
public class CardAdapter extends RecyclerView.Adapter<CardViewHolder> implements Filterable {

    private List<CardItem> cardItemList = new ArrayList<>();

    private final Activity activity;

    private final OnItemListener listener;

    private List<CardItem> cardItemListNotFiltered = new ArrayList<>();

    public CardAdapter(OnItemListener listener, Activity activity) {
        this.listener = listener;
        this.activity = activity;
    }

    /**
     *
     * Called when RecyclerView needs a new RecyclerView.ViewHolder of the given type to represent an item.
     *
     * @param parent ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_layout,
                parent,false);
        return new CardViewHolder(layoutView, listener);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method should update the contents of the RecyclerView.ViewHolder.itemView to reflect
     * the item at the given position.
     *
     * @param holder ViewHolder which should be updated to represent the contents of the item at
     *               the given position in the data set.
     * @param position position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardItem currentCardItem = cardItemList.get(position);
        String imagePath = currentCardItem.getImageResource();
        if (imagePath.contains("ic_")){
            Drawable drawable = AppCompatResources.getDrawable(activity, activity.getResources()
                    .getIdentifier(imagePath, "drawable", activity.getPackageName()));
            holder.placeImageView.setImageDrawable(drawable);
        } else {
            Bitmap bitmap = Utilities.getImageBitmap(activity, Uri.parse(imagePath));
            if (bitmap != null){
                holder.placeImageView.setImageBitmap(bitmap);
            }
        }

        holder.placeTextView.setText(currentCardItem.getPlaceName());
        holder.dateTextView.setText(currentCardItem.getDate());
        holder.locationTextView.setText(currentCardItem.getShortAddress());
        holder.statusTextView.setText(currentCardItem.getStatus());
        if(currentCardItem.getStatus().equals("Not Connected")) {
            holder.statusImageView.setImageResource(R.drawable.ic_disconnected_24dp);
        }
    }

    @Override
    public int getItemCount() {
        return cardItemList.size();
    }

    /**
     *
     * @return the Filter that should be applied to the search
     */
    @Override
    public Filter getFilter() {
        return cardFilter;
    }

    private final Filter cardFilter = new Filter() {
        /**
         * Called to filter the data according to the constraint
         * @param constraint constraint used to filtered the data
         * @return the result of the filtering
         */
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<CardItem> filteredList = new ArrayList<>();

            //if you have no constraint --> return the full list
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(cardItemListNotFiltered);
            } else {
                //else apply the filter and return a filtered list
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (CardItem item : cardItemListNotFiltered) {
                    if (item.getPlaceDescription().toLowerCase().contains(filterPattern) ||
                            item.getPlaceName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        /**
         * Called to publish the filtering results in the user interface
         * @param constraint constraint used to filter the data
         * @param results the result of the filtering
         */
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            List<CardItem> filteredList = new ArrayList<>();
            List<?> result = (List<?>) results.values;
            for (Object object : result) {
                if (object instanceof CardItem) {
                    filteredList.add((CardItem) object);
                }
            }

            //warn the adapter that the data are changed after the filtering
            updateCardListItems(filteredList);
        }
    };

    public void updateCardListItems(List<CardItem> filteredList) {
        final CardItemDiffCallback diffCallback =
                new CardItemDiffCallback(this.cardItemList, filteredList);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.cardItemList = new ArrayList<>(filteredList);
        diffResult.dispatchUpdatesTo(this);
        notifyDataSetChanged();
    }

    /**
     * Method that set the list in the Home
     * @param list the list to display in the home
     */
    public void setData(List<CardItem> list){
        final CardItemDiffCallback diffCallback =
                new CardItemDiffCallback(this.cardItemList, list);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.cardItemList = new ArrayList<>(list);
        this.cardItemListNotFiltered = new ArrayList<>(list);
        for (  CardItem cardItem : list) {
            if(Utilities.getConnectDeviceNames().contains(cardItem.getPlaceName())){
                cardItem.setStatus("Connected");
            }
            else{
                cardItem.setStatus("Not Connected");
            }
        }
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     *
     * @param position the position of the item selected in the list displayed
     * @return the item selected
     */
    public CardItem getItemSelected(int position) {
        //log position of the item selected
        Log.d("CardAdapter", "getItemSelected: " + position);
        //log length of the list
        Log.d("CardAdapter", "getItemSelected: " + cardItemList.size());
        return cardItemList.get(position);
    }

    public void deleteItem(int position){
        //alert dialog to confirm the deletion of the item
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Delete device?");
        builder.setMessage("Are you sure you want to delete the device?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.deleteItem(position);
                removeItem(position);
                notifyItemRemoved(position);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                //refresh the list
                notifyDataSetChanged();
            }
        });
        builder.show();
    }

    private void removeItem(int position) {
        this.cardItemList.remove(position);
    }

    public Activity getActivity() {
        return activity;
    }

    public void updateAllItems(){
        for (  CardItem cardItem : cardItemList) {
            if(Utilities.getConnectDeviceNames().contains(cardItem.getPlaceName())){
                cardItem.setStatus("Connected");
            }
            else{
                cardItem.setStatus("Not Connected");
            }
        }
        notifyDataSetChanged();
    }
}
