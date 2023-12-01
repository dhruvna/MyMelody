package edu.ucsb.ece251.DATQ.mymelody;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

// No need to import Track if it's in the same package

public class TrackAdapter extends ArrayAdapter<Track> {
    public TrackAdapter(Context context, ArrayList<Track> tracks) {
        super(context, 0, tracks);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.tracklist, parent, false);
        }

        TextView tvTrackName = convertView.findViewById(R.id.tvTrackName);
        EditText etTrackRating = convertView.findViewById(R.id.etTrackScore);

        // Now 'track' is correctly recognized as an instance of Track class
        Track track = getItem(position);

        if (track != null) {
            tvTrackName.setText(track.getName());
            etTrackRating.setText(track.getRating() > 0 && track.getRating()<=10 ? String.valueOf(track.getRating()) : "");

            etTrackRating.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                    // No action needed here for this example
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                    // No action needed here for this example
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    // Here you respond after the text has changed
                    try {
                        int rating = Integer.parseInt(editable.toString());
                        track.setRating(rating);
                    } catch (NumberFormatException e) {
                        track.setRating(0);
                    }
                }
                // Implement other required methods of TextWatcher (beforeTextChanged, onTextChanged)
            });
        }

        return convertView;
    }
}
