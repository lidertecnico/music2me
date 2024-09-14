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

    // Variable para ayudar con las operaciones de la base de datos de playlists
    private PlaylistDatabaseHelper dbHelper;

    // Constructor del adaptador, que recibe el contexto, una lista de playlists y el ayudante de la base de datos
    public PlaylistAdapter(Context context, ArrayList<String> playlists, PlaylistDatabaseHelper dbHelper) {
        // Llama al constructor de la clase ArrayAdapter, pasando el contexto y la lista de playlists
        super(context, 0, playlists);
        this.dbHelper = dbHelper; // Asigna el ayudante de la base de datos a la variable de clase
    }

    // Método que crea o reutiliza una vista para cada elemento de la lista
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Obtiene el nombre de la playlist actual según su posición en la lista
        String playlistName = getItem(position);

        // Si no existe una vista reutilizable, infla una nueva desde el layout item_playlist.xml
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_playlist, parent, false);
        }

        // Obtén los componentes visuales del layout para esta vista de playlist
        TextView tvPlaylistName = convertView.findViewById(R.id.tvPlaylistName);
        Button btnAddSong = convertView.findViewById(R.id.btnAddSong);
        Button btnDeletePlaylist = convertView.findViewById(R.id.btnDeletePlaylist);

        // Establece el nombre de la playlist en el TextView correspondiente
        tvPlaylistName.setText(playlistName);

        // Configura la acción del botón para añadir canciones a la playlist
        btnAddSong.setOnClickListener(v -> {
            // Crea un intent para abrir la actividad de agregar canciones a la playlist
            Intent intent = new Intent(getContext(), AddSongToPlaylistActivity.class);
            intent.putExtra("playlistName", playlistName); // Pasa el nombre de la playlist a la actividad
            getContext().startActivity(intent); // Inicia la actividad para agregar canciones
        });

        // Configura la acción del botón para eliminar la playlist
        btnDeletePlaylist.setOnClickListener(v -> {
            // Elimina la playlist de la base de datos
            dbHelper.deletePlaylist(playlistName);
            // Remueve el nombre de la playlist de la lista de la interfaz
            remove(playlistName);
            // Notifica al adaptador que los datos han cambiado para que actualice la vista
            notifyDataSetChanged();
            // Muestra un mensaje confirmando la eliminación de la playlist
            Toast.makeText(getContext(), "Playlist eliminada", Toast.LENGTH_SHORT).show();
        });

        // Retorna la vista configurada para este ítem
        return convertView;
    }
}