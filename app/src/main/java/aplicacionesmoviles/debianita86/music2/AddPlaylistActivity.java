package aplicacionesmoviles.debianita86.music2;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class AddPlaylistActivity extends AppCompatActivity {
    // Declaración de variables para el input de texto, botón, lista de vistas y otros componentes
    private EditText inputNewPlaylist;  // Campo de texto para ingresar el nombre de la nueva playlist
    private Button btnAddPlaylist;  // Botón para agregar la nueva playlist
    private ListView listViewPlaylists;  // ListView para mostrar todas las playlists
    private PlaylistDatabaseHelper dbHelper;  // Ayudante para manejar la base de datos de playlists
    private PlaylistAdapter adapter;  // Adaptador para gestionar la lista de playlists en la interfaz
    private ArrayList<String> playlists;  // Lista de strings que contiene los nombres de las playlists

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_play_list);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Inicialización de los elementos de la interfaz, buscando los views por su ID
        inputNewPlaylist = findViewById(R.id.inputAgregarPlayList);  // Campo de texto para el nombre de playlist
        btnAddPlaylist = findViewById(R.id.botonAgregarPlayList);  // Botón para agregar la playlist
        listViewPlaylists = findViewById(R.id.listasPlayLists);  // ListView que mostrará todas las playlists
        dbHelper = new PlaylistDatabaseHelper(this);  // Inicialización del ayudante de base de datos

        // Se obtienen todas las playlists almacenadas en la base de datos
        playlists = dbHelper.getAllPlaylists();

        // Se configura el adaptador para la lista, enlazando la lista de playlists con la interfaz
        adapter = new PlaylistAdapter(this, playlists, dbHelper);
        listViewPlaylists.setAdapter(adapter);

        // Configura la acción al hacer click en el botón para agregar una nueva playlist
        btnAddPlaylist.setOnClickListener(v -> {
            // Se obtiene el texto ingresado por el usuario
            String playlistName = inputNewPlaylist.getText().toString();

            // Si el nombre de la playlist no está vacío, se agrega a la base de datos
            if (!playlistName.isEmpty()) {
                dbHelper.addPlaylist(playlistName);  // Agregar la playlist a la base de datos
                playlists.clear();  // Limpia la lista de playlists actual
                playlists.addAll(dbHelper.getAllPlaylists());  // Recarga la lista con todas las playlists
                adapter.notifyDataSetChanged();  // Notifica al adaptador que los datos han cambiado
                inputNewPlaylist.setText("");  // Limpia el campo de texto después de agregar
                Toast.makeText(this, "Playlist agregada", Toast.LENGTH_SHORT).show();  // Muestra un mensaje de éxito
            } else {
                // Si el nombre está vacío, muestra un mensaje de advertencia
                Toast.makeText(this, "Por favor ingresa un nombre", Toast.LENGTH_SHORT).show();
            }
        });
    }
}