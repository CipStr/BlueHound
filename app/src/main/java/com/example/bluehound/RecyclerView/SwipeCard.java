package com.example.bluehound.RecyclerView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluehound.ViewModel.DeleteViewModel;

public class SwipeCard  extends ItemTouchHelper.SimpleCallback {

    CardAdapter myCardAdapter;

    public SwipeCard(CardAdapter cardAdapter) {
        super(0,ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.myCardAdapter=cardAdapter;
    }
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        this.myCardAdapter.deleteItem(position);
    }
}
