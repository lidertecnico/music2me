package aplicacionesmoviles.debianita86.music2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class SongAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> songs;
    private final PlaylistDatabaseHelper dbHelper;
    private final String playlistName;

    public SongAdapter(Context context, ArrayList<String> songs, PlaylistDatabaseHelper dbHelper, String playlistName) {
        super(context, R.layout.list_item_song, songs);
        this.context = context;
        this.songs = songs;
        this.dbHelper = dbHelper;
        this.playlistName = playlistName;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_song, parent, false);
        }

        TextView songName = convertView.findViewById(R.id.song_name);
        Button deleteButton = convertView.findViewById(R.id.btn_delete_song);

        String currentSong = songs.get(position);
        songName.setText(currentSong);

        // Eliminar la canción al hacer clic en el botón
        deleteButton.setOnClickListener(v -> {
            dbHelper.deleteSongFromPlaylist(playlistName, currentSong);
            songs.remove(position);
            notifyDataSetChanged();
            Toast.makeText(context, "Canción eliminada", Toast.LENGTH_SHORT).show();
        });

        return convertView;
    }
}
