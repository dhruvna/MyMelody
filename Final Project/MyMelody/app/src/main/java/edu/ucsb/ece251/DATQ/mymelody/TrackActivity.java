package edu.ucsb.ece251.DATQ.mymelody;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;

public class TrackActivity extends AppCompatActivity {
    private ArrayList<Track> trackArrayList; // Use Track model
    private TrackAdapter trackAdapter; // Use TrackAdapter
    private SpotifyService spotifyService;
    private String accessToken;
    private User currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        spotifyService = new SpotifyService(this);
        trackArrayList = new ArrayList<>();
        trackAdapter = new TrackAdapter(this, trackArrayList); // Initialize TrackAdapter with the track list
        ListView trackListView = findViewById(R.id.TrackList);
        trackListView.setAdapter(trackAdapter); // Set the adapter for the ListView

        Button sortButton = findViewById(R.id.btnSortTracks);
        sortButton.setOnClickListener(view -> sortTrackByScore());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String userInfo = extras.getString("User Info");
            currentUser = parseUserString(userInfo);
            accessToken = currentUser.getAccessToken();
        }
        if (accessToken != null) {
            Log.println(Log.VERBOSE, "Received token", accessToken);
            fetchUserTopTracks(accessToken);
        }
    }
    private void sortTrackByScore() {
        Collections.sort(trackArrayList, (track1, track2) -> Integer.compare(track2.getRating(), track1.getRating()));
        trackAdapter.notifyDataSetChanged();
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
    public User parseUserString(String userString) {
        String[] lines = userString.split("\n");
        if(lines.length != 6) {
            return null;
        }
        User user = new User(
                lines[0].substring(lines[0].indexOf(": ") + 2),
                lines[1].substring(lines[1].indexOf(": ") + 2),
                lines[2].substring(lines[2].indexOf(": ") + 2),
                lines[3].substring(lines[3].indexOf(": ") + 2),
                lines[4].substring(lines[4].indexOf(": ") + 2),
                lines[5].substring(lines[5].indexOf(": ") + 2)
        );
        Log.println(Log.VERBOSE, "TESTING PARSE", user.toString());
        return user;
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}