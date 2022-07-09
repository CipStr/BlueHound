package com.example.bluehound.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluehound.R;
import com.example.bluehound.ViewModel.DeleteViewModel;

/**
 * A ViewHolder describes an item view and the metadata about its place within the RecyclerView.
 */
public class CardViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    ImageView placeImageView;
    TextView placeTextView;
    TextView dateTextView;
    TextView locationTextView;
    TextView statusTextView;
    ImageView statusImageView;

    private final OnItemListener itemListener;

    public CardViewHolder(@NonNull View itemView, OnItemListener listener) {
        super(itemView);
        placeImageView = itemView.findViewById(R.id.device_image);
        placeTextView = itemView.findViewById(R.id.name_textview);
        dateTextView = itemView.findViewById(R.id.date_textview);
        itemListener = listener;
        locationTextView = itemView.findViewById(R.id.location_textview);
        statusTextView = itemView.findViewById(R.id.status_textview);
        statusImageView = itemView.findViewById(R.id.imageStatus);
        itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        itemListener.onItemClick(getAdapterPosition());
    }
}
