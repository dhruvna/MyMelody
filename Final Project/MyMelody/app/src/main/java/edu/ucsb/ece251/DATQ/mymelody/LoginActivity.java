package edu.ucsb.ece251.DATQ.mymelody;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.squareup.picasso.Picasso;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {
    private TextView LoginPrompt;
    private ImageView PFP;
    private SpotifyService spotifyService;
    private Button loginButton;
    private Button logoutButton;
    private User currentUser;
    private boolean loggedIn;
    private Toolbar toolbar;
    RelativeLayout currentlyPlayingContainer;
    private ProgressBar songProgressBar;
    private Handler handler = new Handler();
    private Runnable fetchCurrentTrackRunnable;
    private TextView elapsedView;
    private TextView durationView;
    private static final int FETCH_INTERVAL = 1000; // Interval in milliseconds (e.g., 5000ms for 5 seconds)
    private ImageView currentlyPlayingAlbumArt;
    private TextView currentlyPlayingSongName, currentlyPlayingArtistName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        currentlyPlayingContainer = findViewById(R.id.currentlyPlayingContainer);
        toolbar = findViewById(R.id.Toolbar);
        setSupportActionBar(toolbar);

        LoginPrompt = findViewById(R.id.LoginPrompt);
        PFP = findViewById(R.id.pfp);
        loginButton = findViewById(R.id.LoginButton);
        logoutButton = findViewById(R.id.LogoutButton);
        spotifyService = new SpotifyService(this);
        loginButton.setOnClickListener(view -> {
            spotifyService.authenticateSpotify(this);
            loggedIn = true;
        });
        logoutButton.setOnClickListener(view-> {
            if(spotifyService.logOut()) logout();
        });
        PFP.setOnClickListener(view -> openProfile());

        // Initialize UI elements
        currentlyPlayingAlbumArt = findViewById(R.id.currentlyPlayingAlbumArt);
        currentlyPlayingSongName = findViewById(R.id.currentlyPlayingSongName);
        currentlyPlayingArtistName = findViewById(R.id.currentlyPlayingArtistName);
        songProgressBar = findViewById(R.id.songProgressBar);
        elapsedView = findViewById(R.id.currentlyPlayingTrackElapsed);
        durationView = findViewById(R.id.currentlyPlayingTrackDuration);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final Toolbar myToolbar = findViewById(R.id.Toolbar);
        myToolbar.inflateMenu(R.menu.toolbar_menu);
        myToolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int myID = item.getItemId();
        if(loggedIn) {
            if (myID == R.id.artists){
                Intent artistIntent = new Intent(this, ArtistActivity.class);
                artistIntent.putExtra("User Info", currentUser.toString());
                startActivity(artistIntent);
                return true;
            } else if (myID == R.id.tracks){
                Intent trackIntent = new Intent(this, TrackActivity.class);
                trackIntent.putExtra("User Info", currentUser.toString());
                startActivity(trackIntent);
                return true;
            }
        } else {
            showToast("Log in first to see user information!");
            return false;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupFetchCurrentTrackTask() {
        RelativeLayout currentlyPlayingContainer = findViewById(R.id.currentlyPlayingContainer);
        currentlyPlayingContainer.setVisibility(View.VISIBLE);
        fetchCurrentTrackRunnable = new Runnable() {
            @Override
            public void run() {
                if (!loggedIn) {
                    // If not logged in, don't continue
                    handler.removeCallbacks(this);
                    return;
                }
                // Fetch the currently playing track
                spotifyService.fetchCurrentSong(new SpotifyService.FetchSongCallback() {
                    @Override
                    public void onSongFetched(String trackName, String artistName, String albumArtUrl, int progress, int duration) {
                        currentlyPlayingContainer.setVisibility(View.VISIBLE);
                        currentlyPlayingSongName.setText(trackName);
                        currentlyPlayingArtistName.setText(artistName);
                        // Load the album art into the ImageView using Glide or Picasso
                        Picasso.get().load(albumArtUrl).into(currentlyPlayingAlbumArt);
                        updateProgressBar(progress, duration);

                    }
                    @Override
                    public void onError() {
                        // Handle error
//                        showToast("Failed to fetch current song information.");
                    }
                });

                // Schedule the next execution
                if (loggedIn) {
                    handler.postDelayed(this, FETCH_INTERVAL);
                }
            }
        };
        // Start the initial fetch
        handler.post(fetchCurrentTrackRunnable);
    }
    private void updateProgressBar(int progress, int duration) {
        if (duration > 0) {
            long progressPercentage = (100 * progress) / duration;
            songProgressBar.setProgress((int) progressPercentage);
            elapsedView.setText(formatMillisToTime(progress));
            durationView.setText(formatMillisToTime(duration));
        }
    }

    private void fetchUserInfo(String accessToken) {
        spotifyService.fetchUserInfo(accessToken, new SpotifyService.FetchUserInfoCallback() {
            @Override
            public void onUserInfoFetched(User user) {
                currentUser = user;
                toolbar.setTitle(currentUser.getUsername());
                String pfpURL = currentUser.getPFPLink();
                Picasso.get().load(pfpURL).into(PFP);
                PFP.setVisibility(View.VISIBLE);
            }
            @Override
            public void onError() {
                showToast("Failed to fetch user information.");
            }
        });
    }
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String accessToken = spotifyService.handleAuthResponse(intent);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String userInfo = extras.getString("currentUser");
            if (userInfo != null) currentUser = parseUserString(userInfo);
            accessToken = currentUser.getAccessToken();
            Log.println(Log.VERBOSE, "Testing token after back button", accessToken);
        }
        if(accessToken != null) {
            setLoginPrompt();
            loginButton.setVisibility(View.INVISIBLE);
            logoutButton.setVisibility(View.VISIBLE);
            fetchUserInfo(accessToken);
            // Fetch and display the currently playing track
            setupFetchCurrentTrackTask();
        } else {
            LoginPrompt.setText(R.string.fail_msg);
            showToast("Log in failure.");
        }
    }
    private void logout() {
        LoginPrompt.setText(R.string.login_msg);
        showToast("Logged out.");
        loginButton.setVisibility(View.VISIBLE);
        logoutButton.setVisibility(View.INVISIBLE);
        PFP.setVisibility(View.INVISIBLE);
        currentlyPlayingContainer.setVisibility(View.INVISIBLE);
        loggedIn = false;
    }
    private void setLoginPrompt() {
        String loginPrompt = "Welcome! " + "\n" +
                "Click your profile picture below to go to your Spotify profile!"
                ;
        LoginPrompt.setText(loginPrompt);
    }

    private void openProfile() {
        if (loggedIn) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(currentUser.getProfileLink()));
            startActivity(intent);
        }
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

    private String formatMillisToTime(int millis) {
        int seconds = (millis / 1000) % 60;
        int minutes = (millis / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}