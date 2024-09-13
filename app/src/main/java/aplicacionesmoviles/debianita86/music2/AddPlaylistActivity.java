package aplicacionesmoviles.debianita86.music2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class AddPlaylistActivity extends AppCompatActivity {
    private EditText inputNewPlaylist;
    private Button btnAddPlaylist;
    private ListView listViewPlaylists;
    private PlaylistDatabaseHelper dbHelper;
    private PlaylistAdapter adapter;
    private ArrayList<String> playlists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_play_list);

        inputNewPlaylist = findViewById(R.id.inputAgregarPlayList);
        btnAddPlaylist = findViewById(R.id.botonAgregarPlayList);
        listViewPlaylists = findViewById(R.id.listasPlayLists);
        dbHelper = new PlaylistDatabaseHelper(this);

        playlists = dbHelper.getAllPlaylists();
        adapter = new PlaylistAdapter(this, playlists, dbHelper);
        listViewPlaylists.setAdapter(adapter);

        // Al hacer click en agregar playlist
        btnAddPlaylist.setOnClickListener(v -> {
            String playlistName = inputNewPlaylist.getText().toString();
            if (!playlistName.isEmpty()) {
                dbHelper.addPlaylist(playlistName);
                playlists.clear();
                playlists.addAll(dbHelper.getAllPlaylists());
                adapter.notifyDataSetChanged();
                inputNewPlaylist.setText("");  // Limpiar el campo de texto
                Toast.makeText(this, "Playlist agregada", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Por favor ingresa un nombre", Toast.LENGTH_SHORT).show();
            }
        });
    }
}