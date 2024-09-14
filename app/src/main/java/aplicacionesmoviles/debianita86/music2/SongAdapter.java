package aplicacionesmoviles.debianita86.music2;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;

public class SongAdapter extends ArrayAdapter<String> {
    // Variables miembro que contienen el contexto, la lista de canciones, el ayudante de base de datos y el nombre de la playlist
    private final Context context;
    private final ArrayList<String> songs;
    private final PlaylistDatabaseHelper dbHelper;
    private final String playlistName;

    // Constructor del adaptador que recibe el contexto, la lista de canciones, el ayudante de base de datos y el nombre de la playlist
    public SongAdapter(Context context, ArrayList<String> songs, PlaylistDatabaseHelper dbHelper, String playlistName) {
        // Llama al constructor de la clase base ArrayAdapter, pasando el layout y la lista de canciones
        super(context, R.layout.list_item_song, songs);
        this.context = context;          // Asigna el contexto
        this.songs = songs;              // Asigna la lista de canciones
        this.dbHelper = dbHelper;        // Asigna el ayudante de la base de datos
        this.playlistName = playlistName; // Asigna el nombre de la playlist
    }

    // Método que crea o reutiliza una vista para cada canción en la lista
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Si no existe una vista reutilizable, infla una nueva usando el layout 'list_item_song'
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_song, parent, false);
        }

        // Obtén los componentes visuales de la vista (TextView para el nombre de la canción y el botón de eliminar)
        TextView songName = convertView.findViewById(R.id.song_name);
        Button deleteButton = convertView.findViewById(R.id.btn_delete_song);

        // Obtén la canción actual de la lista en la posición especificada
        String songPath = songs.get(position);
        String currentSongName = new File(songPath).getName();

        // Establece el nombre de la canción en el TextView correspondiente
        songName.setText(currentSongName);

        // Configura el botón de eliminar para que, al hacer clic, elimine la canción de la playlist y la base de datos
        deleteButton.setOnClickListener(v -> {
            // Elimina la canción de la base de datos usando el nombre de la playlist y la ruta de la canción
            boolean isDeleted = dbHelper.deleteSongFromPlaylist(playlistName, songPath);

            if (isDeleted) {
                // Elimina la canción de la lista de la interfaz
                songs.remove(position);
                // Notifica al adaptador que los datos han cambiado para actualizar la vista
                notifyDataSetChanged();
                // Muestra un mensaje confirmando que la canción fue eliminada
                Toast.makeText(context, "Canción eliminada", Toast.LENGTH_SHORT).show();
            } else {
                // Si la eliminación falla, muestra un mensaje de error
                Toast.makeText(context, "Error al eliminar la canción", Toast.LENGTH_SHORT).show();
            }

            // Log para depuración
            Log.d("SongAdapter", "Eliminando canción: " + songPath + ", Resultado: " + isDeleted);
        });

        // Retorna la vista configurada para este ítem
        return convertView;
    }
}
