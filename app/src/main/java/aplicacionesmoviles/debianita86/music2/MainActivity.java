package aplicacionesmoviles.debianita86.music2;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ListView listViewPlaylists;
    private Button btnAddPlaylist;
    private PlaylistDatabaseHelper dbHelper;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> playlists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listViewPlaylists = findViewById(R.id.listasPlayListReproduccion);
        btnAddPlaylist = findViewById(R.id.btnAddPlaylistInicio);
        dbHelper = new PlaylistDatabaseHelper(this);

        // Verificar y crear la base de datos si no existe
        dbHelper.getWritableDatabase();

        // Cargar playlists desde la base de datos
        playlists = dbHelper.getAllPlaylists();
        if (playlists == null) {
            playlists = new ArrayList<>();
        }
        adapter = new ArrayAdapter<String>(this, R.layout.list_item_playlist, R.id.playlist_name, playlists);        listViewPlaylists.setAdapter(adapter);

        // Al hacer click en una playlist
        listViewPlaylists.setOnItemClickListener((parent, view, position, id) -> {
            String playlistName = playlists.get(position);
            if (playlistName == null) {
                Toast.makeText(this, "Error: Playlist name no disponible", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MainActivity.this, SongPlayActivity.class);
            intent.putExtra("playlistName", playlistName);
            startActivity(intent);
        });

        // Al hacer click en agregar playlist
        btnAddPlaylist.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddPlaylistActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refrescar la lista de playlists
        playlists.clear();
        playlists.addAll(dbHelper.getAllPlaylists());
        if (playlists == null) {
            playlists = new ArrayList<>();
        }
        adapter.notifyDataSetChanged();
    }
}
