package edu.ucsb.ece251.DATQ.mymelody;

import android.app.AlertDialog;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TrackActivity extends AppCompatActivity {
    private ArrayList<Track> trackArrayList; // Use Track model
    private TrackAdapter trackAdapter; // Use TrackAdapter
    private SpotifyService spotifyService;
    private String accessToken;
    private User currentUser;
    private boolean isDataLoaded = false;
    private boolean slider = false;
    private int rangeSetting;
    private String rangeSelection = "short_term";
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
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String userInfo = extras.getString("User Info");
            if (userInfo != null) currentUser = parseUserString(userInfo);
            accessToken = currentUser.getAccessToken();
        }

        spotifyService = new SpotifyService(this);
        trackArrayList = new ArrayList<>();
        trackAdapter = new TrackAdapter(this, trackArrayList, currentUser.getId(), spotifyService); // Initialize TrackAdapter with the track list
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
                        rangeSelection = "short_term";
                        break;
                    case "Last 6 Months":
                        rangeSetting = last6Months;
                        rangeSelection = "medium_term";
                        break;
                    case "All Time":
                        rangeSetting = allTime;
                        rangeSelection = "long_term";
                        break;
                }
                // Handle the selected item
                Log.println(Log.VERBOSE, "Range selected", "Range: " + selectedTimeRange);
                if(isDataLoaded)
                {
                    isDataLoaded = false;
                }
                else {
                    handleTimeRangeSelection(rangeSelection, numTracks);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        slider = false;
        if(trackSeekBar != null) {
            trackSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    numTracks = progress + 1;  // Since the range is 1 to 50
                    trackCountTextView.setText(numTracks + " Tracks");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    slider = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Fetch tracks immediately after user selection
                    if(slider) {
                        fetchUserTopTracks(accessToken, rangeSelection, numTracks);
                    }

                    slider = false;
                }
            });
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

        if (trackArrayList == null) {
            trackArrayList = new ArrayList<>();
            Log.d("TrackArrayList","Empty");
            isDataLoaded = false;
        }
        if (trackArrayList != null) {
            Log.d("LoadData", "Loaded track list size: " + trackArrayList.size());
            isDataLoaded = true;
            for (int i = 0; i < Math.min(5, trackArrayList.size()); i++) {
                Log.d("LoadData", "Track " + i + ": " + trackArrayList.get(i).getName() + ", Rating: " + trackArrayList.get(i).getRating());
            }
            trackAdapter = new TrackAdapter(this, trackArrayList, currentUser.getId(), spotifyService);
            ListView trackListView = findViewById(R.id.TrackList);
            trackListView.setAdapter(trackAdapter);
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
            fetchUserTopTracks(accessToken, rangeSelection, numTracks);
        } else {
            trackAdapter = new TrackAdapter(this, trackArrayList, currentUser.getId(), spotifyService);
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
        savePreferences();
    }

    private void fetchUserTopTracks(String accessToken, String rangeSelection, int numTracks) {
        spotifyService.fetchUserTopTracks(accessToken, rangeSelection, numTracks, new SpotifyService.FetchTrackCallback() {
            @Override
            public void onTrackFetched(List<Track> tracks) {
                trackArrayList.clear();

                for (int i = 0; i <= tracks.size()-1; i++) {
                    checkAndStoreTrack(tracks.get(i).getId());
                }
                trackAdapter.notifyDataSetChanged();
            }
            @Override
            public void onError() { }
        });
    }

    private boolean isTrackInList(Track track) {
        for (Track existingTrack : trackArrayList) {
            if (existingTrack.getId().equals(track.getId())) {
                // Found a matching track in the list
                return false;
            }
        }
        return true; // No matching track found
    }
    private void checkAndStoreTrack(String trackId) {
        databaseReference.child("tracks" + currentUser.getId()).child(trackId).addListenerForSingleValueEvent(new ValueEventListener() {
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
                        if (isTrackInList(track)) {
                            trackArrayList.add(track);
                            trackAdapter.notifyDataSetChanged();
                        }
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
        spotifyService.fetchTrackDetails(trackId, new SpotifyService.FetchTrackDetailsCallback() {
            @Override
            public void onTrackDetailsFetched(Track track) {
                Log.d("TrackActivity", "Displaying track: " + track.getName() + " - " + track.getArtist() + " - " + track.getAlbumCover());
                // Store in Firebase
                databaseReference.child("tracks" + currentUser.getId()).child(trackId).setValue(track);
                if (isTrackInList(track)) {
                    trackArrayList.add(track);
                    trackAdapter.notifyDataSetChanged();
                }
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

    private void handleTimeRangeSelection(String rangeSelection, int numTracks) {
        // Fetch data for 'Last Month', 'Last 6 Months', or 'All Time'
        fetchUserTopTracks(accessToken, rangeSelection, numTracks);
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
        loadPreferences();
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
}
