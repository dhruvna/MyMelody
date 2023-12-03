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

public class ArtistActivity extends AppCompatActivity {
    private ArrayList<Artist> artistArrayList;
    private ArtistAdapter artistAdapter;
    private SpotifyService spotifyService;
    private String accessToken;
    private User currentUser;

    private int rangeSetting;
    final static int lastMonth = 0;
    final static int last6Months = 1;
    final static int allTime = 2;
    private TextView artistCountTextView;
    private int numArtists = 10;  // Default value

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist);

        spotifyService = new SpotifyService(this);
        artistArrayList = new ArrayList<>();
        artistAdapter = new ArtistAdapter(this, artistArrayList); // Initialize ArtistAdapter with the artist list
        ListView ArtistList = findViewById(R.id.ArtistList);
        ArtistList.setAdapter(artistAdapter); // Set the adapter for the ListView

        Button sortButton = findViewById(R.id.btnSortArtists);
        sortButton.setOnClickListener(view -> sortArtistsByScore());

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
                handleTimeRangeSelection(rangeSetting, numArtists);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        SeekBar artistSeekBar = findViewById(R.id.artistSeekBar);
        artistCountTextView = findViewById(R.id.artistCountTextView);

        artistSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                numArtists = progress + 1;  // Since the range is 1 to 50
                artistCountTextView.setText(numArtists + " Artists");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Optionally implement
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Optionally implement
                // Fetch tracks here if you want to fetch them immediately after user selection
                fetchUserTopArtists(accessToken, rangeSetting, numArtists);
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
            fetchUserTopArtists(accessToken, rangeSetting, numArtists);
        }
    }

    private void sortArtistsByScore() {
        artistArrayList.sort((artist1, artist2) -> Integer.compare(artist2.getRating(), artist1.getRating()));
        showToast("Sorting by your scores!");
        artistAdapter.notifyDataSetChanged();
    }

    private void fetchUserTopArtists(String accessToken, int rangeSetting, int numArtists) {
        spotifyService.fetchUserTopArtists(accessToken, rangeSetting, numArtists, new SpotifyService.FetchArtistCallback() {
            @Override
            public void onArtistFetched(String Artists) {
                String[] ArtistList = Artists.split("%20");
                int numArtists = Integer.parseInt(ArtistList[0]);
                artistArrayList.clear(); // Clear the current artist list
                for (int i = 1; i <= numArtists; i++) {
                    // Assuming a default rating of 0 for all artists initially
                    artistArrayList.add(new Artist(ArtistList[i],0));
                }
                artistAdapter.notifyDataSetChanged(); // Notify the adapter that the data set has changed
            }
            @Override
            public void onError() {
                showToast("Failed to fetch top Artists.");
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

    private void handleTimeRangeSelection(int rangeSetting, int numArtists) {
        // Implement your logic based on the selected time range
        // For example, fetch data for 'Last Month', 'Last 6 Months', or 'All Time'
        fetchUserTopArtists(accessToken, rangeSetting, numArtists);
        artistAdapter.notifyDataSetChanged();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}