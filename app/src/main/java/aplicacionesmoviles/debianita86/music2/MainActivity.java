package aplicacionesmoviles.debianita86.music2;

import android.content.Intent;

import android.os.Bundle;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    // Variables para la lista de playlists, botón de agregar playlist, base de datos y adaptador
    private ListView listViewPlaylists;  // ListView que mostrará las playlists
    private Button btnAddPlaylist;  // Botón para agregar una nueva playlist
    private PlaylistDatabaseHelper dbHelper;  // Ayudante de base de datos para gestionar playlists
    private ArrayAdapter<String> adapter;  // Adaptador para enlazar la lista de playlists con la interfaz
    private ArrayList<String> playlists;  // Lista que contiene los nombres de las playlists

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // Establece el layout de la actividad principal

        // Inicialización de los componentes de la interfaz
        listViewPlaylists = findViewById(R.id.listasPlayListReproduccion);  // Lista de reproducción de playlists
        btnAddPlaylist = findViewById(R.id.btnAddPlaylistInicio);  // Botón para agregar una playlist
        dbHelper = new PlaylistDatabaseHelper(this);  // Inicializa el ayudante de base de datos

        // Verificar y crear la base de datos si no existe
        dbHelper.getWritableDatabase();

        // Cargar playlists desde la base de datos
        playlists = dbHelper.getAllPlaylists();
        if (playlists == null) {
            playlists = new ArrayList<>();  // Si no hay playlists, inicializa una lista vacía
        }

        // Inicializa el adaptador con las playlists obtenidas
        adapter = new ArrayAdapter<>(this, R.layout.list_item_playlist, R.id.playlist_name, playlists);
        listViewPlaylists.setAdapter(adapter);  // Asigna el adaptador al ListView

        // Al hacer click en una playlist de la lista
        listViewPlaylists.setOnItemClickListener((parent, view, position, id) -> {
            String playlistName = playlists.get(position);  // Obtiene el nombre de la playlist seleccionada
            if (playlistName == null) {
                // Muestra un mensaje si el nombre de la playlist no está disponible
                Toast.makeText(this, "Error: Playlist name no disponible", Toast.LENGTH_SHORT).show();
                return;
            }
            // Inicia la actividad de reproducción de canciones en la playlist seleccionada
            Intent intent = new Intent(MainActivity.this, SongPlayActivity.class);
            intent.putExtra("playlistName", playlistName);  // Pasa el nombre de la playlist a la nueva actividad
            startActivity(intent);
        });

        // Al hacer click en el botón para agregar una nueva playlist
        btnAddPlaylist.setOnClickListener(v -> {
            // Inicia la actividad para agregar una nueva playlist
            Intent intent = new Intent(MainActivity.this, AddPlaylistActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresca la lista de playlists cada vez que se reanuda la actividad
        playlists.clear();  // Limpia la lista de playlists actual
        playlists.addAll(dbHelper.getAllPlaylists());  // Carga todas las playlists desde la base de datos
        if (playlists == null) {
            playlists = new ArrayList<>();  // Si no hay playlists, inicializa una lista vacía
        }
        adapter.notifyDataSetChanged();  // Notifica al adaptador que los datos han cambiado para actualizar la vista
    }
}
