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


public class TrackAdapter extends ArrayAdapter<Track> {
    public TrackAdapter(Context context, ArrayList<Track> tracks) {
        super(context, 0, tracks);
    }

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

        if (track != null) {
            holder.tvTrackName.setText(track.getName());

            // Remove the existing TextWatcher
            if (holder.etTrackScore.getTag() instanceof TextWatcher) {
                holder.etTrackScore.removeTextChangedListener((TextWatcher) holder.etTrackScore.getTag());
            }

            holder.etTrackScore.setText(track.getRating() >= 0 ? String.valueOf(track.getRating()) : "");

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

    static class ViewHolder {
        TextView tvTrackName;
        EditText etTrackScore;
    }

}
