package com.ndroidpro.popularmoviesapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Nikhil Vashistha on 9/5/2016.
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    private static final String TAG = "MainActivity";
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w185";

    GridView mMainGrid;
    ArrayList<Movie> mPopularList;
    ArrayList<Movie> mTopVotedList;
    final static String POPULAR_LIST = "poppularList";
    final static String TOP_VOTE_LIST = "topVoteList";

    // Handling Saving Data before App is closed
    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putSerializable(POPULAR_LIST, mPopularList);
        outState.putSerializable(TOP_VOTE_LIST, mTopVotedList);
        super.onSaveInstanceState(outState);

    }


    // Loading SavedInstance
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Loading Saved data
        mPopularList = (ArrayList<Movie>) savedInstanceState.getSerializable(POPULAR_LIST);
        mTopVotedList = (ArrayList<Movie>) savedInstanceState.getSerializable(TOP_VOTE_LIST);
        loadPreferenceList();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mPopularList == null || mTopVotedList == null){ // Checking to see if data is present before loading
            if (NetworkConnections.isInternetConnected(MainActivity.this)) {
                new MainSync().execute();
            } else {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle(getString(R.string.network_alert_title));
                dialog.setMessage(getString(R.string.network_alert_message));
                dialog.setCancelable(false);
                dialog.show();
            }
        } else {
            loadPreferenceList();
        }

        mMainGrid.setOnItemClickListener(MainActivity.this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMainGrid = (GridView) findViewById(R.id.topMovieGrid);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent optionIntent = new Intent(this, SettingsActivity.class);
            startActivity(optionIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p/>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Movie movie = (Movie) parent.getAdapter().getItem(position);

        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
        intent.putExtra("movies_details", movie);
        startActivity(intent);
    }

    public class MainSync extends AsyncTask<Void, Void, Void> {
        private static final String POPULAR_MOVIE_API_URL = "http://api.themoviedb.org/3/movie/popular?api_key=";
        private static final String TOP_RATED_MOVIE_API_URL = "http://api.themoviedb.org/3/movie/top_rated?api_key=";
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setTitle("Loading...");
            dialog.show();

        }

        @Override
        protected Void doInBackground(Void... params) {

            String WebAddress;
            String WebAddressVote;

            WebAddress = POPULAR_MOVIE_API_URL
                   + BuildConfig.THE_MOVIE_DB_API_KEY;

            WebAddressVote = TOP_RATED_MOVIE_API_URL
                    + BuildConfig.THE_MOVIE_DB_API_KEY;

            mPopularList = new ArrayList<>();
            mTopVotedList = new ArrayList<>();

            URLResult(WebAddress, mPopularList);
            URLResult(WebAddressVote, mTopVotedList);

            mPopularList.toString();

            return null;
        }

        @Override
        protected void onPostExecute(Void  s) {
            super.onPostExecute(s);
            loadPreferenceList();

            dialog.cancel();

        }
    }

    private void loadPreferenceList() {
        // Checking Shared Preference for data
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        if (sharedPreferences.getString(getString(R.string.display_preferences_sort_order_key),
                getString(R.string.display_preferences_sort_default_value)).equals("popular")) {
            loadMovieAdapter(mPopularList);
        } else {
            loadMovieAdapter(mTopVotedList);

        }
    }

    private void loadMovieAdapter(ArrayList<Movie> _list) {
        CustomGridAdapter adapter = new CustomGridAdapter(MainActivity.this,
                _list);
        mMainGrid.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void URLResult(String webAddress, ArrayList<Movie> _List) {
        try {
            BufferedReader reader = null;
            URL url = new URL(webAddress);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            InputStream inputStream = connection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                return;
            }
            String results = buffer.toString();
//            String results = IOUtils.toString(inputStream);
            jsonParser(results, _List);
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();


        }
    }

    private void jsonParser(String s, ArrayList<Movie> movies) {
        try {
            JSONObject mainObject = new JSONObject(s);

            JSONArray resultsArray = mainObject.getJSONArray("results");
            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject indexObject = resultsArray.getJSONObject(i);
                Movie indexMovie = new Movie();

                indexMovie.setId(indexObject.getInt("id"));
                indexMovie.setOriginalTitle(indexObject.getString("original_title"));
                indexMovie.setOverview(indexObject.getString("overview"));
                indexMovie.setReleaseDate(indexObject.getString("release_date"));
                indexMovie.setPosterPath(IMAGE_BASE_URL + indexObject.getString("poster_path"));
                indexMovie.setPopularity(indexObject.getInt("popularity"));
                indexMovie.setTitle(indexObject.getString("title"));
                indexMovie.setVoteAverage(indexObject.getInt("vote_average"));
                indexMovie.setVoteCount(indexObject.getInt("vote_count"));

                movies.add(indexMovie); // Add each item to the list
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "JSON Error", e);
        }
    }

    public class CustomGridAdapter extends BaseAdapter {

        Context context;
        ArrayList<Movie> movieList;

        public CustomGridAdapter(Context context, ArrayList<Movie> movieDbList) {
            this.context = context;
            this.movieList = movieDbList;
        }

        /**
         * How many items are in the data set represented by this Adapter.
         *
         * @return Count of items.
         */
        @Override
        public int getCount() {
            return movieList.size();
        }

        /**
         * Get the data item associated with the specified position in the data set.
         *
         * @param position Position of the item whose data we want within the adapter's
         *                 data set.
         * @return The data at the specified position.
         */
        @Override
        public Movie getItem(int position) {
            return movieList.get(position);
        }

        /**
         * Get the row id associated with the specified position in the list.
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        @Override
        public long getItemId(int position) {
            return 123456000 + position;
        }

        /**
         * Get a View that displays the data at the specified position in the data set. You can either
         * create a View manually or inflate it from an XML layout file. When the View is inflated, the
         * parent View (GridView, ListView...) will apply default layout parameters unless you use
         * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
         * to specify a root view and to prevent attachment to the root.
         *
         * @param position    The position of the item within the adapter's data set of the item whose view
         *                    we want.
         * @param convertView The old view to reuse, if possible. Note: You should check that this view
         *                    is non-null and of an appropriate type before using. If it is not possible to convert
         *                    this view to display the correct data, this method can create a new view.
         *                    Heterogeneous lists can specify their number of view types, so that this View is
         *                    always of the right type (see {@link #getViewTypeCount()} and
         *                    {@link #getItemViewType(int)}).
         * @param parent      The parent that this view will eventually be attached to
         * @return A View corresponding to the data at the specified position.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.custom_item_row, parent, false);
            }
            Movie movieDb = getItem(position);

            ImageView imageViewcustom = (ImageView) convertView.findViewById(R.id.customImageView);
            Picasso.with(context).load(movieDb.getPosterPath())
                    .placeholder(R.mipmap.ic_launcher)
                    .into(imageViewcustom);

            return convertView;
        }
    }
}
