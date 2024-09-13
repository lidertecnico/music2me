package aplicacionesmoviles.debianita86.music2;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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


public class AddSongToPlaylistActivity extends AppCompatActivity {
    // Variables para botones, texto, URI del archivo seleccionado, base de datos, y lista de canciones
    private Button btnSelectFile, btnAddSong;  // Botones para seleccionar archivo y agregar canción
    private TextView selectedFileText;  // Texto que muestra el archivo seleccionado
    private Uri selectedFileUri;  // URI del archivo seleccionado
    private PlaylistDatabaseHelper dbHelper;  // Ayudante de base de datos para gestionar playlists
    private String playlistName;  // Nombre de la playlist actual
    private ListView listView;  // Lista de canciones en la playlist
    private ArrayAdapter<String> adapter;  // Adaptador para mostrar la lista de canciones
    private ArrayList<String> songs;  // Lista de rutas de canciones

    // Lanzador de actividad para seleccionar archivo, reemplaza onActivityResult
    private final ActivityResultLauncher<Intent> selectFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Comprueba si la selección fue exitosa
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        selectedFileUri = data.getData();  // Obtiene la URI del archivo seleccionado
                        if (selectedFileUri != null) {
                            // Obtiene y muestra el nombre del archivo seleccionado
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
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_song_to_play_list);  // Establece el layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Inicialización de componentes de la interfaz
        btnSelectFile = findViewById(R.id.botonSeleccionarArchivo);  // Botón para seleccionar archivo
        btnAddSong = findViewById(R.id.botonAgregarFileToPlayList);  // Botón para agregar canción
        selectedFileText = findViewById(R.id.textoArchivoSeleccionado);  // Texto para mostrar el archivo seleccionado
        listView = findViewById(R.id.ListaDeCancionesDeLaPlayList);  // ListView que muestra las canciones

        dbHelper = new PlaylistDatabaseHelper(this);  // Inicializa el ayudante de base de datos
        playlistName = getIntent().getStringExtra("playlistName");  // Obtiene el nombre de la playlist

        // Carga las canciones de la playlist desde la base de datos
        songs = dbHelper.getSongsFromPlaylist(playlistName);
        ArrayList<String> songNames = new ArrayList<>();

        // Obtiene los nombres de las canciones de sus rutas
        for (String path : songs) {
            songNames.add(new File(path).getName());
        }

        // Inicializa el adaptador con las canciones
        adapter = new SongAdapter(this, songNames, dbHelper, playlistName);
        listView.setAdapter(adapter);  // Asocia el adaptador al ListView

        checkReadPermission();  // Verifica permisos de lectura

        // Al hacer click en el botón para seleccionar archivo
        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);  // Inicia el selector de documentos
            intent.setType("audio/*");  // Filtra para archivos de audio
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            selectFileLauncher.launch(intent);  // Lanza la actividad de selección de archivo
        });

        // Al hacer click en el botón para agregar la canción seleccionada a la playlist
        btnAddSong.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                try {
                    // Guarda el archivo seleccionado en el directorio de la aplicación
                    String savedPath = saveFileToAppDirectory(selectedFileUri);
                    if (savedPath != null) {
                        // Agrega la canción a la playlist en la base de datos
                        dbHelper.addSongToPlaylist(playlistName, savedPath);
                        songs.clear();  // Limpia la lista de canciones actual
                        songs.addAll(dbHelper.getSongsFromPlaylist(playlistName));  // Carga todas las canciones de nuevo

                        songNames.clear();  // Limpia los nombres de las canciones
                        for (String path : songs) {
                            songNames.add(new File(path).getName());  // Añade los nombres de las canciones
                        }
                        adapter.notifyDataSetChanged();  // Notifica que los datos han cambiado

                        Toast.makeText(this, "Canción añadida a la playlist", Toast.LENGTH_SHORT).show();  // Mensaje de éxito
                    } else {
                        Toast.makeText(this, "Error al guardar el archivo", Toast.LENGTH_SHORT).show();  // Mensaje de error
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("AddSongActivity", "Error al guardar el archivo", e);  // Log de error
                }
            } else {
                Toast.makeText(this, "Por favor selecciona un archivo", Toast.LENGTH_SHORT).show();  // Mensaje si no se ha seleccionado un archivo
            }
        });
    }

    // Obtener el nombre del archivo desde la URI
    private String getFileName(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        String fileName = null;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                fileName = cursor.getString(nameIndex);  // Obtiene el nombre del archivo
            }
        } finally {
            if (cursor != null) {
                cursor.close();  // Cierra el cursor para evitar fugas de memoria
            }
        }
        return fileName;  // Devuelve el nombre del archivo
    }

    // Guardar el archivo en el directorio de la aplicación
    private String saveFileToAppDirectory(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);  // Abre el InputStream del archivo seleccionado
        if (inputStream == null) {
            return null;  // Si no hay InputStream, retorna null
        }

        // Crea un directorio de música dentro del almacenamiento externo de la aplicación
        File musicDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "MyMusicFiles");
        if (!musicDir.exists()) {
            musicDir.mkdirs();  // Si el directorio no existe, lo crea
        }

        String fileName = getFileName(uri);  // Obtiene el nombre del archivo
        File file = new File(musicDir, fileName);  // Crea un archivo en el directorio de música

        FileOutputStream outputStream = new FileOutputStream(file);  // Abre un FileOutputStream para escribir en el archivo

        byte[] buffer = new byte[1024];
        int bytesRead;
        // Escribe el contenido del archivo en el nuevo archivo en bloques de 1024 bytes
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();  // Cierra el InputStream
        outputStream.close();  // Cierra el OutputStream

        return file.getAbsolutePath();  // Retorna la ruta absoluta del archivo guardado
    }

    // Verificar permisos de lectura
    private void checkReadPermission() {
        // Si la versión de Android es mayor o igual a Android R (API 30)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // Si no tiene permisos de administrador de archivos, solicita el permiso
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        } else {
            // Para versiones menores, solicita el permiso de lectura del almacenamiento externo
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    // Manejar el resultado de la solicitud de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            // Si el permiso es concedido
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de lectura concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso de lectura denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
