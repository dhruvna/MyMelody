package edu.ucsb.ece251.DATQ.mymelody;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> TrackArray;
    private ArrayAdapter adapter;
    private Handler handler = new Handler();

    private TextView LoginStatus;
    private SpotifyService spotifyService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        TrackArray = new ArrayList<>();
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, TrackArray);
        ListView TrackList = findViewById(R.id.TrackList);
        TrackList.setAdapter(adapter);

        LoginStatus = findViewById(R.id.LoginStatus);
        Button loginButton = findViewById(R.id.LoginButton);
        Button logoutButton = findViewById(R.id.LogoutButton);
        Button fetchUserInfoButton = findViewById(R.id.FetchUserInfoButton);
        Button fetchTracksButton = findViewById(R.id.FetchTracksButton);
        //Initialize Spotify Service
        spotifyService = new SpotifyService(this);
        loginButton.setOnClickListener(view -> spotifyService.authenticateSpotify(this));
        logoutButton.setOnClickListener(view-> {
            boolean logOutSuccess = spotifyService.logOut();
            if(logOutSuccess) {
                LoginStatus.setText(R.string.reset_msg);
                showToast("Logged out.");
            }
        });
        fetchTracksButton.setOnClickListener(view -> spotifyService.fetchUserTopTracks(new SpotifyService.FetchTrackCallback() {
            @Override
            public void onTrackFetched(String tracks) {
                String[] trackList = tracks.split("%20");
                int numTracks = Integer.parseInt(trackList[0]);
                TrackArray.add("Fetching Top " + numTracks + " Tracks!");
                for(int i = 1; i < numTracks; i++) {
                    TrackArray.add(trackList[i]);
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onError() {
                showToast("Failed to fetch username.");
            }
        }));
        fetchUserInfoButton.setOnClickListener(view -> spotifyService.fetchUsername(new SpotifyService.FetchUsernameCallback() {
            @Override
            public void onUsernameFetched(String usernameEmail) {
                String[] userInfo = usernameEmail.split(",");
                String username = userInfo[0];
                String email = userInfo[1];
                LoginStatus.setText("Username: " + username + "\nEmail: " + email);
            }

            @Override
            public void onError() {
                showToast("Failed to fetch username.");
            }
        }));
        // Initialize WebView for Google Charts
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean loginSuccess = spotifyService.handleAuthResponse(intent);

        if(loginSuccess) {
            LoginStatus.setText(R.string.success_msg);
        } else {
            LoginStatus.setText(R.string.fail_msg);
            showToast("Log in failure.");
        }
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}


