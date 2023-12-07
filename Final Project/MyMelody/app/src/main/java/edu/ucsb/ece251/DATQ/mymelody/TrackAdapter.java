package edu.ucsb.ece251.DATQ.mymelody;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import java.util.ArrayList;


public class TrackAdapter extends ArrayAdapter<Track> {
    private final String Userid;
    private final SpotifyService spotifyService; // Add this line

    public TrackAdapter(Context context, ArrayList<Track> tracks, String id, SpotifyService spotifyService) {
        super(context, 0, tracks);
        this.Userid = id;
        this.spotifyService = spotifyService;
    }
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.tracklist, parent, false); // Ensure this is the correct layout
            holder = new ViewHolder();
            holder.tvTrackName = convertView.findViewById(R.id.tvTrackName);
            holder.etTrackScore = convertView.findViewById(R.id.etTrackScore);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Track track = getItem(position);
        convertView.setOnClickListener(v -> {
            Log.println(Log.VERBOSE, "Test onclick", "TESTING on click for: " + track.getId());
            showConfirmationDialog(track);
        });

        if (track != null) {
            holder.tvTrackName.setText(track.getName());


            // Remove the existing TextWatcher
            if (holder.etTrackScore.getTag() instanceof TextWatcher) {
                holder.etTrackScore.removeTextChangedListener((TextWatcher) holder.etTrackScore.getTag());
            }

            holder.etTrackScore.setText(track.getRating() >= 0 && track.getRating()<=10 ? String.valueOf(track.getRating()) : "");
            onScoreChanged(track, track.getRating());
            TextWatcher textWatcher = new TextWatcher() {
                // beforeTextChanged, onTextChanged, afterTextChanged implementation
                @Override
                public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                    // No action needed here for this example
                }
                @Override
                public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                    // No action needed here for this example
                }
                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        int rating = Integer.parseInt(s.toString());
                        track.setRating(rating);
                        saveTrackRating(track);
                    } catch (NumberFormatException e) {
                        track.setRating(0); // Or handle this as you see fit
                    }
                }

                // Other methods (beforeTextChanged, onTextChanged) can remain empty if not needed
            };

            holder.etTrackScore.addTextChangedListener(textWatcher);
            holder.etTrackScore.setTag(textWatcher); // Store the watcher in the tag for later removal
        }

        return convertView;
    }
    public void onScoreChanged(Track track, int newScore) {
        track.setRating(newScore); // Update the score in the Track object
        databaseReference.child("tracks" + Userid).child(track.getId()).setValue(track); // Update Firebase
    }

    private void saveTrackRating(Track track) {
        SharedPreferences prefs = getContext().getSharedPreferences("TrackPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(track.getId(), track.getRating());
        editor.apply();
    }

    private void showConfirmationDialog(Track track) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext()); // Use 'getActivity()' if in a fragment
        builder.setTitle("Confirm Action");
        builder.setMessage("Are you sure you want to perform this action on " + track.getName() + "?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            // Handle the confirmation action here
            Log.println(Log.VERBOSE, "CONFIRMATION", "Confirming queue request for " + track.getName());
            spotifyService.addToQueue(track.getId());
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            Log.println(Log.VERBOSE, "CONFIRMATION", "Queue request denied.");
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    static class ViewHolder {
        TextView tvTrackName;
        EditText etTrackScore;
    }
}
