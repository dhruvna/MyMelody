package edu.ucsb.ece251.DATQ.mymelody;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class TrackActivity extends AppCompatActivity {
    private ArrayList<Track> trackArrayList; // Use Track model
    private TrackAdapter trackAdapter; // Use TrackAdapter
    private SpotifyService spotifyService;
    private String accessToken;
    private User currentUser;
    private boolean isDataLoaded = false;
    private int rangeSetting;
    final static int lastMonth = 0;
    final static int last6Months = 1;
    final static int allTime = 2;
    private TextView trackCountTextView;
    private int numTracks = 10;  // Default value
    SeekBar trackSeekBar;
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        spotifyService = new SpotifyService(this);
        trackArrayList = new ArrayList<>();
        trackAdapter = new TrackAdapter(this, trackArrayList); // Initialize TrackAdapter with the track list
        ListView trackListView = findViewById(R.id.TrackList);
        trackListView.setAdapter(trackAdapter); // Set the adapter for the ListView
        FirebaseApp.initializeApp(this);
        trackCountTextView = findViewById(R.id.trackCountTextView);
        trackSeekBar = findViewById(R.id.trackSeekBar);

        Button sortButton = findViewById(R.id.btnSortTracks);
        sortButton.setOnClickListener(view -> showSortingOptions());

        Spinner timeRange = findViewById(R.id.timeRange);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.time_range_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeRange.setAdapter(adapter);

        timeRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTimeRange = (String) parent.getItemAtPosition(position);
                switch(selectedTimeRange) {
                    case "Last Month":
                        rangeSetting = lastMonth;
                        break;
                    case "Last 6 Months":
                        rangeSetting = last6Months;
                        break;
                    case "All Time":
                        rangeSetting = allTime;
                        break;
                }
                // Handle the selected item
                Log.println(Log.VERBOSE, "Range selected", "Range: " + selectedTimeRange);
                if(isDataLoaded)
                {
                    isDataLoaded = false;
                }
                else {
                    handleTimeRangeSelection(rangeSetting, numTracks);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        if(trackSeekBar != null) {
            trackSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    numTracks = progress + 1;  // Since the range is 1 to 50
                    trackCountTextView.setText(numTracks + " Tracks");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Optionally implement
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Optionally implement
                    // Fetch tracks here if you want to fetch them immediately after user selection
                    //if (trackArrayList.isEmpty()) {
                    fetchUserTopTracks(accessToken, rangeSetting, numTracks);
                    //}
                }
            });
        }
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String userInfo = extras.getString("User Info");
            if (userInfo != null) currentUser = parseUserString(userInfo);
            accessToken = currentUser.getAccessToken();
        }
        loadPreferences();

    }
    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences("TrackPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(trackArrayList);
        Log.d("SaveData", "Saving JSON: " + json);
        editor.putString("trackList", json);
        editor.putInt("numTracks", numTracks);
        editor.putInt("rangeSetting", rangeSetting);
        editor.putInt("seekBarPosition", numTracks - 1);
        editor.apply();
    }


    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences("TrackPrefs", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("trackList", null);
        Type type = new TypeToken<ArrayList<Track>>() {}.getType();
        trackArrayList = gson.fromJson(json, type);
        if (trackArrayList != null) {
            Log.d("LoadData", "Loaded track list size: " + trackArrayList.size());
            isDataLoaded = true;
            for (int i = 0; i < Math.min(5, trackArrayList.size()); i++) {
                Log.d("LoadData", "Track " + i + ": " + trackArrayList.get(i).getName() + ", Rating: " + trackArrayList.get(i).getRating());
            }
        }
        if (trackArrayList == null) {
            trackArrayList = new ArrayList<>();
            Log.d("TrackArrayList","Empty");
            isDataLoaded = false;
        }

        int seekBarPosition = prefs.getInt("seekBarPosition", 9);
        numTracks = seekBarPosition + 1;
        trackSeekBar.setProgress(seekBarPosition);
        trackCountTextView.setText(numTracks + " Tracks");
        rangeSetting = prefs.getInt("rangeSetting", lastMonth);
        numTracks = prefs.getInt("numTracks", 10);
        Spinner timeRange = findViewById(R.id.timeRange);
        if (timeRange != null) {
            timeRange.setSelection(rangeSetting);
        }

        if (accessToken != null && trackArrayList.isEmpty()) {
            fetchUserTopTracks(accessToken, rangeSetting, numTracks);
        } else {
            trackAdapter = new TrackAdapter(this, trackArrayList);
            ListView trackListView = findViewById(R.id.TrackList);
            trackListView.setAdapter(trackAdapter);
        }
    }

    private void sortTrackByScore(boolean ascending) {
        if (ascending) {
            // Sort ascending by name
            trackArrayList.sort((track1, track2) -> {
                int scoreComparison = Integer.compare(track1.getRating(), track2.getRating());
                if (scoreComparison == 0) {
                    return track1.getName().compareToIgnoreCase(track2.getName());
                }
                return scoreComparison;
            });
        } else {
            // Sort descending by score and then by name if scores are equal
            trackArrayList.sort((track1, track2) -> {
                int scoreComparison = Integer.compare(track2.getRating(), track1.getRating());
                if (scoreComparison == 0) {
                    return track1.getName().compareToIgnoreCase(track2.getName());
                }
                return scoreComparison;
            });
        }

        trackAdapter.notifyDataSetChanged();
    }

    private void fetchUserTopTracks(String accessToken, int rangeSetting, int numTracks) {
        spotifyService.fetchUserTopTracks(accessToken, rangeSetting, numTracks, new SpotifyService.FetchTrackCallback() {
            @Override
            public void onTrackFetched(String tracks) {
                String[] trackList = tracks.split("%20");
                int numTracksFetched = Integer.parseInt(trackList[0]);
                trackArrayList.clear();

                for (int i = 1; i <= numTracksFetched; i++) {
                    String[] trackInfo = trackList[i].split("%21"); // Assuming trackList[i] is the track ID
                    Log.d("TrackId", trackInfo[1]);
                    checkAndStoreTrack(trackInfo[1]);
                }
                trackAdapter.notifyDataSetChanged();
            }
            @Override
            public void onError() {
                showToast("Failed to fetch top tracks.");
            }
        });
    }

    private void checkAndStoreTrack(String trackId) {
        databaseReference.child("tracks").child(trackId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    // Track not in Firebase, so fetch details from Spotify and store
                    fetchTrackDetailsFromSpotifyAndStore(trackId);
                } else {
                    Log.d("Firebase", "Data updated: " + dataSnapshot.getValue());
                    // Track exists in Firebase, use it
                    Track track = dataSnapshot.getValue(Track.class);
                    if (track != null) {
                        trackArrayList.add(track);
                        trackAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("Firebase", "Failed to read track", databaseError.toException());
            }
        });
    }

    private void fetchTrackDetailsFromSpotifyAndStore(String trackId) {
        // Fetch track details from Spotify
        // This is a placeholder, you need to implement this based on your SpotifyService
        spotifyService.fetchTrackDetails(trackId, new SpotifyService.FetchTrackDetailsCallback() {
            @Override
            public void onTrackDetailsFetched(Track track) {
                // Store in Firebase
                databaseReference.child("tracks").child(trackId).setValue(track);
                trackArrayList.add(track);
                trackAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError() {
                Log.e("Spotify", "Failed to fetch track details");
            }
        });
    }

    public User parseUserString(String userString) {
        String[] lines = userString.split("\n");
        if (lines.length != 6) return null;
        return new User(
                lines[0].substring(lines[0].indexOf(": ") + 2),
                lines[1].substring(lines[1].indexOf(": ") + 2),
                lines[2].substring(lines[2].indexOf(": ") + 2),
                lines[3].substring(lines[3].indexOf(": ") + 2),
                lines[4].substring(lines[4].indexOf(": ") + 2),
                lines[5].substring(lines[5].indexOf(": ") + 2)
        );
    }

    private void handleTimeRangeSelection(int rangeSetting, int numTracks) {
        // Implement your logic based on the selected time range
        // For example, fetch data for 'Last Month', 'Last 6 Months', or 'All Time'
        fetchUserTopTracks(accessToken, rangeSetting, numTracks);
        trackAdapter.notifyDataSetChanged();
    }
    @Override
    protected void onPause() {
        super.onPause();
        savePreferences();
    }
    @Override
    protected void onResume() {
        super.onResume();
        //loadPreferences();
    }
    private void showSortingOptions() {
        String[] options = {"Sort Ascending", "Sort Descending"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Sorting Option");
        builder.setItems(options, (dialog, which) -> {
            // Descending
            sortTrackByScore(which == 0); // Ascending
        });
        builder.show();
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}