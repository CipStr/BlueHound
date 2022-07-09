package com.example.bluehound;

import android.util.Log;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.osmdroid.util.GeoPoint;

/**
 * Class which represents every card item with its information (image, name, data, location)
 */
@Entity(tableName = "item")
public class CardItem {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "item_id")
    private int id;

    @ColumnInfo(name = "item_image")
    private String imageResource;
    @ColumnInfo(name = "item_name")
    private String placeName;
    @ColumnInfo(name = "item_location")
    private String placeDescription;
    @ColumnInfo(name = "item_date")
    private String date;
    @ColumnInfo(name = "item_status")
    private String status;

    public CardItem(String imageResource, String placeName, String placeDescription, String date) {
        this.imageResource = imageResource;
        this.placeName = placeName;
        this.placeDescription = placeDescription;
        this.date = date;
        if(Utilities.getConnectDeviceNames().contains(placeName)){
            this.status = "Connected";
        }
        else {
            this.status = "Not Connected";
        }
    }

    public String getImageResource() {
        return imageResource;
    }

    public String getPlaceName() {
        return placeName;
    }

    public String getPlaceDescription() {
        return placeDescription;
    }

    public String getDate() {
        return date;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getShortAddress() {
        String[] address = placeDescription.split(",");
        if(address.length > 1) {
            return address[1];
        }
        else {
            return address[0];
        }
    }

    public GeoPoint getGeoPoint() {
        //get the coordinates from the description
        String[] coordinates = this.placeDescription.split( " coordinates:" );
        //check if the coordinates are valid
        if(coordinates.length != 2) {
            return new GeoPoint(44.148021238440485, 12.23538187241765);
        }
        String[] latLng = coordinates[1].split( "," );
        double lat = Double.parseDouble( latLng[0] );
        double lng = Double.parseDouble( latLng[1] );
        return new GeoPoint( lat, lng );
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
