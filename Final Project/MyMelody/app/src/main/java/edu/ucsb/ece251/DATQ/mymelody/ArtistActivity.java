package edu.ucsb.ece251.DATQ.mymelody;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;

public class ArtistActivity extends AppCompatActivity {
    private ArrayList<String> ArtistArray;
    private ArrayAdapter adapter;
    private SpotifyService spotifyService;
    private String accessToken;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist);

        spotifyService = new SpotifyService(this);
        ArtistArray = new ArrayList<>();
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, ArtistArray);
        ListView ArtistList = findViewById(R.id.ArtistList);
        ArtistList.setAdapter(adapter);
        Bundle extras = getIntent().getExtras();
        if (extras != null) accessToken = extras.getString("Access Token");
        Log.println(Log.VERBOSE, "Received token", accessToken);
        fetchUserTopArtists(accessToken);

    }
    private void fetchUserTopArtists(String accessToken) {
        spotifyService.fetchUserTopArtists(accessToken, new SpotifyService.FetchArtistCallback() {
            @Override
            public void onArtistFetched(String Artists) {
                String[] ArtistList = Artists.split("%20");
                int numArtists = Integer.parseInt(ArtistList[0]);
                ArtistArray.add("Fetching Top " + numArtists + " Artists!");
                ArtistArray.addAll(Arrays.asList(ArtistList).subList(1, numArtists));
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onError() {
                showToast("Failed to fetch top Artists.");
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}