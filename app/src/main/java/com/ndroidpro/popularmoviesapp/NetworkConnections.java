package com.ndroidpro.popularmoviesapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Nikhil Vashistha on 9/5/2016.
 */
public class NetworkConnections {

    /**
     *  This method checks for a network connection.
     * @param context
     * @return Boolean value
     */
    public static Boolean isInternetConnected(Context context){

        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()){
            return true;
        }
        return false;
    }
}