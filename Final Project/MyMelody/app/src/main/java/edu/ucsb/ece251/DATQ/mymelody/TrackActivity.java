package edu.ucsb.ece251.DATQ.mymelody;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;

public class TrackActivity extends AppCompatActivity {
    private ArrayList<String> TrackArray;
    private ArrayAdapter adapter;
    private SpotifyService spotifyService;
    private String accessToken;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        spotifyService = new SpotifyService(this);
        TrackArray = new ArrayList<>();
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, TrackArray);
        ListView TrackList = findViewById(R.id.TrackList);
        TrackList.setAdapter(adapter);
        Bundle extras = getIntent().getExtras();
        if (extras != null) accessToken = extras.getString("Access Token");
        if (accessToken != null) {
            Log.println(Log.VERBOSE, "Received token", accessToken);
            fetchUserTopTracks(accessToken);
        }
    }
    private void fetchUserTopTracks(String accessToken) {
        spotifyService.fetchUserTopTracks(accessToken, new SpotifyService.FetchTrackCallback() {
            @Override
            public void onTrackFetched(String tracks) {
                String[] trackList = tracks.split("%20");
                int numTracks = Integer.parseInt(trackList[0]);
                TrackArray.add("Fetching Top " + numTracks + " Tracks!");
                TrackArray.addAll(Arrays.asList(trackList).subList(1, numTracks));
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onError() {
                showToast("Failed to fetch top tracks.");
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}