package com.example.bluehound.Database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.bluehound.CardItem;

import java.util.List;

@Dao
public interface CardItemDAO {

    //The selected OnConflictStrategy ignores a new CardItem if it's already in the list
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addCardItem(CardItem cardItem);

    // @Transaction: anything inside the method runs in a single transaction.
    @Transaction
    @Query("SELECT * FROM item ORDER BY item_id DESC")
    LiveData<List<CardItem>> getCardItems();

    //delete card item by id
    @Delete
    void deleteCardItem(CardItem cardItem);

    @Query("DELETE FROM item")
    void deleteAllCardItems();

}
