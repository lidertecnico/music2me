package aplicacionesmoviles.debianita86.music2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;

public class AddSongToPlaylistActivity extends AppCompatActivity {
    private Button btnSelectFile, btnAddSong;
    private TextView selectedFileText;
    private Uri selectedFileUri;
    private PlaylistDatabaseHelper dbHelper;
    private String playlistName;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> songs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_song_to_play_list);

        btnSelectFile = findViewById(R.id.botonSeleccionarArchivo);
        btnAddSong = findViewById(R.id.botonAgregarFileToPlayList);
        selectedFileText = findViewById(R.id.textoArchivoSeleccionado);
        listView = findViewById(R.id.ListaDeCancionesDeLaPlayList);

        dbHelper = new PlaylistDatabaseHelper(this);
        playlistName = getIntent().getStringExtra("playlistName");

        songs = dbHelper.getSongsFromPlaylist(playlistName);
        SongAdapter adapter = new SongAdapter(this, songs, dbHelper, playlistName);
        listView.setAdapter(adapter);

        checkReadPermission();

        // Selección de archivos
        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("audio/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 1);
        });

        // Añadir la canción seleccionada
        btnAddSong.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                try {
                    // Guardar el archivo en una ruta accesible
                    String savedPath = saveFileToAppDirectory(selectedFileUri);
                    if (savedPath != null) {
                        dbHelper.addSongToPlaylist(playlistName, savedPath); // Guardar la ruta del archivo
                        songs.clear();
                        songs.addAll(dbHelper.getSongsFromPlaylist(playlistName));
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Canción añadida a la playlist", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error al guardar el archivo", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("AddSongActivity", "Error al guardar el archivo", e);
                }
            } else {
                Toast.makeText(this, "Por favor selecciona un archivo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Manejo de la actividad de selección de archivo
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null) {
                selectedFileUri = data.getData();
                if (selectedFileUri != null) {
                    // Obtener y mostrar el nombre del archivo
                    String fileName = getFileName(selectedFileUri);
                    selectedFileText.setText(fileName != null ? fileName : "Archivo seleccionado");
                } else {
                    Log.d("AddSongActivity", "URI es nulo");
                }
            } else {
                Log.d("AddSongActivity", "Data es nulo");
            }
        } else {
            Log.d("AddSongActivity", "No se seleccionó ningún archivo o la actividad fue cancelada.");
        }
    }

    // Obtener el nombre del archivo desde la URI
    private String getFileName(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        String fileName = null;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                fileName = cursor.getString(nameIndex);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return fileName;
    }

    // Guardar el archivo en el directorio de la aplicación
    private String saveFileToAppDirectory(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            return null;
        }

        File musicDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "MyMusicFiles");
        if (!musicDir.exists()) {
            musicDir.mkdirs();
        }

        String fileName = getFileName(uri);
        File file = new File(musicDir, fileName);

        FileOutputStream outputStream = new FileOutputStream(file);

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();
        outputStream.close();

        return file.getAbsolutePath();
    }

    // Verificar permisos de lectura
    private void checkReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de lectura concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso de lectura denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}