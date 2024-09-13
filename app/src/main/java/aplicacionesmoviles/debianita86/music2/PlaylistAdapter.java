package aplicacionesmoviles.debianita86.music2;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class PlaylistAdapter extends ArrayAdapter<String> {

    private PlaylistDatabaseHelper dbHelper;

    public PlaylistAdapter(Context context, ArrayList<String> playlists, PlaylistDatabaseHelper dbHelper) {
        super(context, 0, playlists);
        this.dbHelper = dbHelper;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String playlistName = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_playlist, parent, false);
        }

        // Obtén los componentes del layout
        TextView tvPlaylistName = convertView.findViewById(R.id.tvPlaylistName);
        Button btnAddSong = convertView.findViewById(R.id.btnAddSong);
        Button btnDeletePlaylist = convertView.findViewById(R.id.btnDeletePlaylist);

        // Setea el nombre de la playlist
        tvPlaylistName.setText(playlistName);

        // Configura el botón para añadir canciones
        btnAddSong.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddSongToPlaylistActivity.class);
            intent.putExtra("playlistName", playlistName);
            getContext().startActivity(intent);
        });

        // Configura el botón para eliminar la playlist
        btnDeletePlaylist.setOnClickListener(v -> {
            dbHelper.deletePlaylist(playlistName);
            remove(playlistName);  // Elimina el ítem de la lista en la interfaz
            notifyDataSetChanged();
            Toast.makeText(getContext(), "Playlist eliminada", Toast.LENGTH_SHORT).show();
        });

        return convertView;
    }
}