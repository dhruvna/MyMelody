package edu.ucsb.ece251.DATQ.mymelody;

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

import java.util.ArrayList;
import java.util.Collections;

public class TrackActivity extends AppCompatActivity {
    private ArrayList<Track> trackArrayList; // Use Track model
    private TrackAdapter trackAdapter; // Use TrackAdapter
    private SpotifyService spotifyService;
    private String accessToken;
    private User currentUser;

    private int rangeSetting;
    final static int lastMonth = 0;
    final static int last6Months = 1;
    final static int allTime = 2;

    private SeekBar trackSeekBar;
    private TextView trackCountTextView;
    private int numTracks = 10;  // Default value

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
                handleTimeRangeSelection(rangeSetting, numTracks);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        trackSeekBar = findViewById(R.id.trackSeekBar);
        trackCountTextView = findViewById(R.id.trackCountTextView);

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
                fetchUserTopTracks(accessToken, rangeSetting, numTracks);
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String userInfo = extras.getString("User Info");
            if (userInfo != null) currentUser = parseUserString(userInfo);
            accessToken = currentUser.getAccessToken();
        }
        if (accessToken != null) {
            Log.println(Log.VERBOSE, "Received token", accessToken);
            fetchUserTopTracks(accessToken, rangeSetting, numTracks);
        }
    }

    private void sortTrackByScore() {
        Collections.sort(trackArrayList, (track1, track2) -> Integer.compare(track2.getRating(), track1.getRating()));
        trackAdapter.notifyDataSetChanged();
    }

    private void fetchUserTopTracks(String accessToken, int rangeSetting, int numTracks) {
        spotifyService.fetchUserTopTracks(accessToken, rangeSetting, numTracks, new SpotifyService.FetchTrackCallback() {
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}