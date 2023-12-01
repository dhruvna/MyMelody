package edu.ucsb.ece251.DATQ.mymelody;

import android.content.Intent;
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
    private User currentUser;
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
    public void onBackPressed() {
        Intent goBack = new Intent(this, LoginActivity.class);
        Log.println(Log.VERBOSE, "Back to Login", "BACK BUTTON PRESSED");
        goBack.putExtra("Access Token", accessToken);
        goBack.putExtra("currentUser", currentUser.toString());
        startActivity(goBack);
        finish();
    }
}