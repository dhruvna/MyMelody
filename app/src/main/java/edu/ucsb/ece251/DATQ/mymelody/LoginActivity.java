package edu.ucsb.ece251.DATQ.mymelody;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.squareup.picasso.Picasso;

import java.util.Locale;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private TextView LoginPrompt;
    private ImageView PFP;
    private SpotifyService spotifyService;
    private Button loginButton, logoutButton;
    private User currentUser;
    private boolean loggedIn;
    private Toolbar toolbar;

    //Spotify Player Widget
    private String currentSong = "";
    private ImageView currentlyPlayingAlbumArt;
    private TextView currentlyPlayingSongName, currentlyPlayingArtistName;
    private ProgressBar songProgressBar;
    private TextView elapsedView, durationView;
    private ImageView playPauseBtn, shuffleBtn, repeatBtn;
    private boolean isPlaying = false;
    private String shuffleState = "shuffleOff";
    private String repeatState = "repeatOff";
    private static final int FETCH_INTERVAL = 1050;
    private final Handler handler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        toolbar = findViewById(R.id.Toolbar);
        setSupportActionBar(toolbar);
        // Initialize UI elements
        spotifyService = new SpotifyService(this);
        initializeUIElements();
        setupButtonBehavior();
        if (savedInstanceState != null) {
            // Restore the logged-in state and current user
            loggedIn = savedInstanceState.getBoolean("LoggedIn", false);
            String userString = savedInstanceState.getString("CurrentUser");
            if (userString != null) {
                currentUser = parseUserString(userString);
                // Update the UI with the restored user details
                toolbar.setTitle(currentUser.getUsername());
                String pfpURL = currentUser.getPFPLink();
                Picasso.get().load(pfpURL).into(PFP);
                // Set visibility of login/logout buttons based on loggedIn state
                loginButton.setVisibility(loggedIn ? View.INVISIBLE : View.VISIBLE);
                logoutButton.setVisibility(loggedIn ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }
    private void initializeUIElements() {
        LoginPrompt = findViewById(R.id.LoginPrompt);
        currentlyPlayingAlbumArt = findViewById(R.id.currentlyPlayingAlbumArt);
        currentlyPlayingSongName = findViewById(R.id.currentlyPlayingSongName);
        currentlyPlayingSongName.setSelected(true);
        currentlyPlayingArtistName = findViewById(R.id.currentlyPlayingArtistName);
        songProgressBar = findViewById(R.id.songProgressBar);
        elapsedView = findViewById(R.id.currentlyPlayingTrackElapsed);
        durationView = findViewById(R.id.currentlyPlayingTrackDuration);
        loginButton = findViewById(R.id.LoginButton);
        logoutButton = findViewById(R.id.LogoutButton);
        PFP = findViewById(R.id.pfp);
        playPauseBtn = findViewById(R.id.playPauseButton);
        shuffleBtn = findViewById(R.id.shuffleButton);
        repeatBtn = findViewById(R.id.repeatButton);
    }
    private void setupButtonBehavior() {
        //Button Behavior
        loginButton.setOnClickListener(view -> spotifyService.authenticateSpotify(this));
        logoutButton.setOnClickListener(view-> {
            if(spotifyService.logOut()) {
                logout();
            }
        });
        PFP.setOnClickListener(view -> openProfile());
        ImageView goBackBtn = findViewById(R.id.goBackButton);
        ImageView fastForwardBtn = findViewById(R.id.fastForwardButton);
        goBackBtn.setOnClickListener(v-> {
            if(loggedIn)
                skipSong(currentUser.getAccessToken(), "previous");
        });
        fastForwardBtn.setOnClickListener(v-> {
            if(loggedIn)
                skipSong(currentUser.getAccessToken(), "next");
        });
        playPauseBtn.setOnClickListener(v-> {
            if(loggedIn) {
                isPlaying = !isPlaying;
                if(isPlaying) {
                    shufRepPlayPause(currentUser.getAccessToken(), "play");
                }
                else {
                    shufRepPlayPause(currentUser.getAccessToken(), "pause");
                }
            }
        });
        shuffleBtn.setOnClickListener(v-> {
            if(shuffleState.equals("shuffleOff"))
                shufRepPlayPause(currentUser.getAccessToken(), "shuffleOn");
            else if (shuffleState.equals("shuffleOn"))
                shufRepPlayPause(currentUser.getAccessToken(), "shuffleOff");
        });
        repeatBtn.setOnClickListener(v-> {
            if(repeatState.equals("repeatAll")) {
                shufRepPlayPause(currentUser.getAccessToken(), "repeatOne");
            }
            if(repeatState.equals("repeatOne")) {
                shufRepPlayPause(currentUser.getAccessToken(), "repeatOff");
            }
            if(repeatState.equals("repeatOff")) {
                shufRepPlayPause(currentUser.getAccessToken(), "repeatAll");
            }
        });
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("LoggedIn", loggedIn);
        if (currentUser != null) {
            outState.putString("CurrentUser", currentUser.toString());
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final Toolbar myToolbar = findViewById(R.id.Toolbar);
        myToolbar.inflateMenu(R.menu.toolbar_menu);
        myToolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        int myID = item.getItemId();
        if(loggedIn) {
            Intent menuIntent = null;
            if (myID == R.id.artists){
                menuIntent = new Intent(this, ArtistActivity.class);
            } else if (myID == R.id.tracks){
                menuIntent = new Intent(this, TrackActivity.class);
            } else if (myID == R.id.charts) {
                menuIntent = new Intent(this, GoogleChartsWebView.class);
            }
            if (menuIntent != null) {
                menuIntent.putExtra("User Info", currentUser.toString());
                startActivity(menuIntent);
            }
            return true;
        } else {
            showToast("Log in first to see user information!");
            return false;
        }
    }
    private void setupFetchCurrentTrackTask() {
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
                        updateWidgetVisibility(true);
                        updateStatus(currentUser.getAccessToken());
                        if(!currentSong.equals(trackName)) {
                            currentlyPlayingSongName.setText(trackName);
                            currentlyPlayingArtistName.setText(artistName);
                            Picasso.get().load(albumArtUrl).into(currentlyPlayingAlbumArt);
                            currentlyPlayingAlbumArt.setVisibility(View.VISIBLE);
                            currentSong = trackName;
                        }
                        // Load the album art into the ImageView using Picasso
                        updateProgressBar(progress, duration);
                    }
                    @Override
                    public void onNoActiveSession() {
                        updateWidgetVisibility(false);
                    }
                    @Override
                    public void onError() {
                        // Handle error
//                        showToast("Failed to fetch current song information.");
                        updateWidgetVisibility(false);
                    }
                });
                // Schedule the next execution
                if (loggedIn)
                    handler.postDelayed(this, FETCH_INTERVAL);
            }
        };
        // Start the initial fetch
        handler.post(fetchCurrentTrackRunnable);
    }
    private void updateProgressBar(int progress, int duration) {
        currentlyPlayingSongName.setVisibility(View.VISIBLE);
        currentlyPlayingArtistName.setVisibility(View.VISIBLE);
        currentlyPlayingAlbumArt.setVisibility(View.VISIBLE);
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
            widget.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        }
    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String accessToken = prefs.getString("AccessToken", null);
        if(accessToken != null) {
            loggedIn = prefs.getBoolean("LoginStatus", true);
            currentUser = parseUserString(Objects.requireNonNull(prefs.getString("CurrentUser", null)));
            loginStuff(accessToken);
        }
    }
    private void fetchDeviceStatus(String accessToken) {
        spotifyService.fetchCurrentDeviceStatus(accessToken, new SpotifyService.FetchDeviceStatusCallback() {
            @Override
            public void onDeviceStatusFetched(String deviceId, Boolean is_playing, String repeat_state, Boolean shuffle_state) {
                isPlaying = is_playing;
                switch (repeat_state) {
                    case "off":
                        repeatState = "repeatOff";
                        break;
                    case "context":
                        repeatState = "repeatAll";
                        break;
                    case "track":
                        repeatState = "repeatOne";
                        break;
                }
                shuffleState = (shuffle_state) ? "shuffleOn" : "shuffleOff";
            }
            @Override
            public void onNoActiveSession() {
//                showToast("NO ACTIVE SESSION");
            }
            @Override
            public void onError() {
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
                //showToast("Failed to fetch user information.");
            }
        });
    }
    private void skipSong(String accessToken, String direction) {
        spotifyService.skipSong(accessToken, direction, new SpotifyService.skipSongCallback() {
            @Override
            public void onSkipSongSuccess() {
                showToast("Skipping to " + direction + " track.");
            }
            @Override
            public void onError() {
                showToast("Failed to skip to " + direction + " track.");
            }
        });
    }
    private void shufRepPlayPause(String accessToken, String ShufRepPlayPause) {
        spotifyService.shufRepPlayPause(accessToken, ShufRepPlayPause, new SpotifyService.shufRepPlayPauseCallback() {
            @Override
            public void onShufRepPlayPauseSuccess() {
                switch (ShufRepPlayPause) {
                    case "pause":
                        showToast("Playback Paused.");
                        isPlaying = false;
                        break;
                    case "play":
                        showToast("Playback Resumed.");
                        isPlaying = true;
                        break;
                    case "shuffleOn":
                        showToast("Shuffle on.");
                        shuffleState = "shuffleOn";
                        break;
                    case "shuffleOff":
                        showToast("Shuffle off.");
                        shuffleState = "shuffleOff";
                        break;
                    case "repeatAll":
                        showToast("Repeat off.");
                        repeatState = "repeatAll";
                        break;
                    case "repeatOne":
                        showToast("Repeat one on.");
                        repeatState = "repeatOne";
                        break;
                    case "repeatOff":
                        showToast("Repeat off.");
                        repeatState = "repeatOff";
                        break;
                }
            }
            @Override
            public void onError() {
                showToast("Failed to change shuffle/repeat state.");
            }
        });
    }
    //Only gets called after login authorization
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String accessToken = spotifyService.handleAuthResponse(intent);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String userInfo = extras.getString("currentUser");
            if (userInfo != null) currentUser = parseUserString(userInfo);
            accessToken = currentUser.getAccessToken();
        }
        if(accessToken != null) {
            loggedIn = true;
            loginStuff(accessToken);
        } else {
            LoginPrompt.setText(R.string.fail_msg);
            showToast("Log in failure.");
        }
    }
    private void logout() {
        toolbar.setTitle("My Melody");
        LoginPrompt.setText(R.string.login_msg);
        showToast("Logged out.");
        loginButton.setVisibility(View.VISIBLE);
        logoutButton.setVisibility(View.INVISIBLE);
        PFP.setVisibility(View.INVISIBLE);
        loggedIn = false;
        handleSharedPreferences(false);
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        updateWidgetVisibility(false);
    }
    private void setLoginPrompt() {
        String loginPrompt = "Welcome! " + "\n" +
                "Click your profile picture below to go to your Spotify profile!"
                ;
        LoginPrompt.setText(loginPrompt);
    }
    private void loginStuff(String accessToken) {
        setLoginPrompt();
        loginButton.setVisibility(View.INVISIBLE);
        logoutButton.setVisibility(View.VISIBLE);
        fetchUserInfo(accessToken);
        // Fetch and display the currently playing track
        updateStatus(accessToken);
        setupFetchCurrentTrackTask();
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
        return new User(
                lines[0].substring(lines[0].indexOf(": ") + 2),
                lines[1].substring(lines[1].indexOf(": ") + 2),
                lines[2].substring(lines[2].indexOf(": ") + 2),
                lines[3].substring(lines[3].indexOf(": ") + 2),
                lines[4].substring(lines[4].indexOf(": ") + 2),
                lines[5].substring(lines[5].indexOf(": ") + 2)
        );
    }

    private String formatMillisToTime(int millis) {
        int seconds = (millis / 1000) % 60;
        int minutes = (millis / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
    private void updateStatus(String accessToken) {
        fetchDeviceStatus(accessToken);
        if(!isPlaying) {
            playPauseBtn.setImageResource(R.drawable.play);
        } else {
            playPauseBtn.setImageResource(R.drawable.pause);
        }
        if(shuffleState.equals("shuffleOn")) {
            shuffleBtn.setImageResource(R.drawable.shuffle_white);
        } else if(shuffleState.equals("shuffleOff")) {
            shuffleBtn.setImageResource(R.drawable.shuffle);
        }
        switch (repeatState) {
            case "repeatOff":
                repeatBtn.setImageResource(R.drawable.repeat);
                break;
            case "repeatOne":
                repeatBtn.setImageResource(R.drawable.repeat_one);
                break;
            case "repeatAll":
                repeatBtn.setImageResource(R.drawable.repeat_white);
                break;
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        handleSharedPreferences(true);
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(loggedIn){
            SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            String accessToken = prefs.getString("AccessToken", null);
            if(accessToken != null) {
                loggedIn = prefs.getBoolean("LoginStatus", true);
                currentUser = parseUserString(Objects.requireNonNull(prefs.getString("CurrentUser", null)));
                loginStuff(accessToken);
            }
        }
    }
    private void handleSharedPreferences(boolean save) {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (save && loggedIn) {
            editor.putString("AccessToken", currentUser.getAccessToken());
            editor.putBoolean("LoginStatus", loggedIn);
            editor.putString("CurrentUser", currentUser.toString());
        } else {
            editor.clear();
        }
        editor.apply();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        spotifyService.shutdownThreads();
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}