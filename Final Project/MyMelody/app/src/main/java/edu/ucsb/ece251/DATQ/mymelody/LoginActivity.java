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
    private Button loginButton, logoutButton;
    private User currentUser;
    private boolean loggedIn;
    private Toolbar toolbar;

    //Spotify Player Widget
    private ImageView currentlyPlayingAlbumArt;
    private TextView currentlyPlayingSongName, currentlyPlayingArtistName;
    private ProgressBar songProgressBar;
    private TextView elapsedView, durationView;
    private ImageView playPauseBtn;
    private boolean isPlaying = false;
    private String currentDeviceID;
    private static final int FETCH_INTERVAL = 1000; 
    private final Handler handler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

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
        ImageView goBackBtn = findViewById(R.id.goBackButton);
        playPauseBtn = findViewById(R.id.playPauseButton);
        ImageView fastForwardBtn = findViewById(R.id.fastForwardButton);
        goBackBtn.setOnClickListener(v-> {
            if(loggedIn) {
//                fetchDeviceID(currentUser.getAccessToken());
                skipSong(currentUser.getAccessToken(), "previous");
            }
//            spotifyService.skipToPrevious(currentDeviceID);
        });
        playPauseBtn.setOnClickListener(v-> {
            if(loggedIn) {
//                fetchDeviceID(currentUser.getAccessToken());
                isPlaying = !isPlaying;
                playPause(currentUser.getAccessToken(), isPlaying);
            }
//            spotifyService.playPause(currentDeviceID);

        });
        fastForwardBtn.setOnClickListener(v-> {
            if(loggedIn) {
//                fetchDeviceID(currentUser.getAccessToken());
                skipSong(currentUser.getAccessToken(), "next");
            }
//            spotifyService.skipToNext(currentDeviceID);
        });
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
//                finish();
                return true;
            } else if (myID == R.id.tracks){
                Intent trackIntent = new Intent(this, TrackActivity.class);
                trackIntent.putExtra("User Info", currentUser.toString());
                startActivity(trackIntent);
//                finish();
                return true;
            }
        } else {
            showToast("Log in first to see user information!");
            return false;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupFetchCurrentTrackTask() {
        // If not logged in, don't continue
        // Fetch the currently playing track
        // Load the album art into the ImageView using Glide or Picasso
        // Handle error
        // Schedule the next execution
        Runnable fetchCurrentTrackRunnable = new Runnable() {
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
                        currentlyPlayingSongName.setText(trackName);
                        currentlyPlayingArtistName.setText(artistName);
                        // Load the album art into the ImageView using Glide or Picasso
                        Picasso.get().load(albumArtUrl).into(currentlyPlayingAlbumArt);
                        updateProgressBar(progress, duration);
                        updateWidgetVisibility(true);
                    }

                    @Override
                    public void onError() {
                        // Handle error

//                        showToast("Failed to fetch current song information.");
                        showToast("Failed to fetch current song information.");
                        updateWidgetVisibility(false);
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
            long progressPercentage = (100L * progress) / duration;
            songProgressBar.setProgress((int) progressPercentage);
            elapsedView.setText(formatMillisToTime(progress));
            durationView.setText(formatMillisToTime(duration));
        }
    }
    private void updateWidgetVisibility(boolean isVisible) {
        View widget = findViewById(R.id.spotifyWidgetContainer);
        if (widget != null) {
            Log.d("LoginActivity", "Updating widget visibility: " + isVisible); // Debugging log
            widget.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        } else {
            Log.e("LoginActivity", "Widget container not found. Make sure the ID is correct.");
        }
    }
    private void skipSong(String accessToken, String direction) {
        spotifyService.skipSong(accessToken, direction, new SpotifyService.skipSongCallback() {
            @Override
            public void onSkipSongSuccess() {
                showToast("Skipping to " + direction + "track.");
            }
            @Override
            public void onError() {
                showToast("Failed to skip to " + direction +"track.");
            }
        });
    }
    private void playPause(String accessToken, boolean isPlaying) {
        spotifyService.playPause(accessToken, isPlaying, new SpotifyService.playPauseCallback() {
            @Override
            public void onPlayPauseSuccess() {
                if(isPlaying) {
                    showToast("Playback Paused.");
                } else showToast("Playback Resumed.");
                updatePauseIcon();
            }
            @Override
            public void onError() {
                showToast("Failed to play/pause current track.");
            }
        });
    }
    private void fetchDeviceID(String accessToken) {
        spotifyService.fetchCurrentDeviceId(accessToken, new SpotifyService.FetchDeviceIdCallback() {
            @Override
            public void onDeviceIdFetched(String deviceId) {
                currentDeviceID = deviceId;
                Log.println(Log.VERBOSE, "Device ID Fetcher","Device ID: " + currentDeviceID);
            }
            @Override
            public void onError() {
                Log.println(Log.VERBOSE, "Device ID Error","Failed to fetch current device ID");
            }
        });
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
            fetchDeviceID(accessToken);
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
        updateWidgetVisibility(false);
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
    private void updatePauseIcon() {
        if(!isPlaying) {
            playPauseBtn.setImageResource(R.drawable.pause);
        } else {
            playPauseBtn.setImageResource(R.drawable.play);
        }
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}