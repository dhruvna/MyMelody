package edu.ucsb.ece251.DATQ.mymelody;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class TrackActivity extends AppCompatActivity {
    private ArrayList<Track> trackArrayList; // Use Track model
    private TrackAdapter trackAdapter; // Use TrackAdapter
    private SpotifyService spotifyService;
    private String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        spotifyService = new SpotifyService(this);
        trackArrayList = new ArrayList<>();
        trackAdapter = new TrackAdapter(this, trackArrayList); // Initialize TrackAdapter with the track list
        ListView trackListView = findViewById(R.id.TrackList);
        trackListView.setAdapter(trackAdapter); // Set the adapter for the ListView
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            accessToken = extras.getString("Access Token");
            fetchUserTopTracks(accessToken);
        }
    }

    private void fetchUserTopTracks(String accessToken) {
        spotifyService.fetchUserTopTracks(accessToken, new SpotifyService.FetchTrackCallback() {
            @Override
            public void onTrackFetched(String tracks) {
                String[] trackList = tracks.split("%20");
                int numTracks = Integer.parseInt(trackList[0]);
                trackArrayList.clear(); // Clear the current track list
                for (int i = 1; i <= numTracks; i++) {
                    // Assuming a default rating of 0 for all tracks initially
                    trackArrayList.add(new Track(trackList[i], 0));
                }
                trackAdapter.notifyDataSetChanged(); // Notify the adapter that the data set has changed
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
