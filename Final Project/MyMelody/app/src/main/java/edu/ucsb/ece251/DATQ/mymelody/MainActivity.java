package edu.ucsb.ece251.DATQ.mymelody;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView LoginStatus;
    private SpotifyService spotifyService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LoginStatus = findViewById(R.id.LoginStatus);
        Button loginButton = findViewById(R.id.LoginButton);
        Button logoutButton = findViewById(R.id.LogoutButton);
        Button fetchButton = findViewById(R.id.FetchButton);
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
        fetchButton.setOnClickListener(view -> {
            spotifyService.fetchUserTopTracks();
        });
        fetchButton.setOnClickListener(view -> {
            spotifyService.fetchUsername(new SpotifyService.FetchUsernameCallback() {
                @Override
                public void onUsernameFetched(String username) {
                    LoginStatus.setText("Username: " + username);
                }

                @Override
                public void onError() {
                    showToast("Failed to fetch username.");
                }
            });
        });
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


