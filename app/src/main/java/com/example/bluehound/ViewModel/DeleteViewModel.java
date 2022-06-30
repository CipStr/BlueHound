package com.example.bluehound.ViewModel;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.bluehound.CardItem;
import com.example.bluehound.Database.CardItemRepository;
import com.example.bluehound.RecyclerView.CardAdapter;

/**
 * ViewModel that handles the data flow between the MainActivity and its Fragment for deleting a card item or all card items
 */
public class DeleteViewModel extends AndroidViewModel {
    private final MutableLiveData<CardItem> itemSelected = new MutableLiveData<>();
    private final CardItemRepository repository;
    public DeleteViewModel(@NonNull Application application) {
        super(application);
        repository = new CardItemRepository(application);
    }


    public MutableLiveData<CardItem> getItemSelected() {
        return itemSelected;
    }
    public void setItemSelected(CardItem cardItem) {
        itemSelected.setValue(cardItem);
    }
    public void deleteCardItem(CardItem cardItem) {
        repository.deleteCardItem(cardItem);
    }
    public void deleteAllCardItems() {
        repository.deleteAllCardItems();
    }

}
