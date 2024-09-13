package aplicacionesmoviles.debianita86.music2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SongPlayActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private ListView listViewSongs;
    private String playlistName;
    private PlaylistDatabaseHelper dbHelper;
    private ArrayList<String> songPaths;
    private ArrayAdapter<String> adapter;
    private MediaPlayer mediaPlayer;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;

    // Views del CardView
    private ImageView albumCover;
    private TextView songTitle, artistName, timeElapsed, timeTotal;
    private SeekBar songProgress;
    private ImageButton btnPlay, btnNext, btnPrevious;
    private int currentSongIndex = 0; // Índice de la canción actual
    private boolean isPlaying = false; // Controlar si la canción está en pausa o en reproducción

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_play);

        // Enlazar los componentes de la interfaz
        listViewSongs = findViewById(R.id.lista_canciones_en_tarjeta);
        albumCover = findViewById(R.id.album_cover);
        songTitle = findViewById(R.id.song_title);
        artistName = findViewById(R.id.artist_name);
        timeElapsed = findViewById(R.id.time_elapsed);
        timeTotal = findViewById(R.id.time_total);
        songProgress = findViewById(R.id.song_progress);
        btnPlay = findViewById(R.id.btn_play);
        btnNext = findViewById(R.id.btn_next);
        btnPrevious = findViewById(R.id.btn_previous);

        dbHelper = new PlaylistDatabaseHelper(this);

        // Verificación de playlistName
        playlistName = getIntent().getStringExtra("playlistName");
        if (playlistName == null) {
            Toast.makeText(this, "Error: Playlist name no recibido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Verificar y solicitar permisos de almacenamiento
        checkStoragePermission();

        // Configurar botones de reproducción
        configurePlaybackControls();
        // Registrar el callback para manejar el botón "atrás"
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Aquí manejamos la acción del botón atrás
                Intent intent = new Intent(SongPlayActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Evita múltiples instancias de MainActivity
                startActivity(intent); // Inicia MainActivity
                // No liberamos el MediaPlayer para que la música continúe
            }
        };
        // Registrar el callback con el dispatcher
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                loadSongs();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                loadSongs();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                loadSongs();
            } else {
                Toast.makeText(this, "Permiso no concedido", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongs();
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Método para cargar las canciones de la base de datos
    private void loadSongs() {
        songPaths = dbHelper.getSongsFromPlaylist(playlistName); // Cargar las URIs desde la base de datos

        // Mostrar las URIs directamente en el ListView
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, songPaths);
        listViewSongs.setAdapter(adapter);

        // Al seleccionar una canción, reproducirla y actualizar los detalles
        listViewSongs.setOnItemClickListener((parent, view, position, id) -> {
            currentSongIndex = position; // Actualizar el índice de la canción actual
            playSong(songPaths.get(currentSongIndex));
        });
    }

    private void playSong(String songUriString) {
        Uri songUri = Uri.parse(songUriString);

        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();

        try {
            Log.d("SongPath", "Reproduciendo: " + songUri.toString());

            mediaPlayer.setDataSource(getApplicationContext(), songUri);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mediaPlayer.start();
                btnPlay.setImageResource(R.drawable.ic_pause); // Cambiar icono a "pausa" al iniciar reproducción
                isPlaying = true;
                updateProgressBar();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                // Avanzar a la siguiente canción cuando la actual termine
                if (currentSongIndex < songPaths.size() - 1) {
                    currentSongIndex++;
                    playSong(songPaths.get(currentSongIndex));
                } else {
                    Toast.makeText(this, "Última canción reproducida", Toast.LENGTH_SHORT).show();
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("MediaPlayerError", "Error en la reproducción (what: " + what + ", extra: " + extra + ")");
                Toast.makeText(this, "Error en la reproducción", Toast.LENGTH_SHORT).show();
                return true;
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al reproducir la canción", Toast.LENGTH_SHORT).show();
        }
    }

    // Actualizar la barra de progreso y los tiempos
    private void updateProgressBar() {
        songProgress.setMax(mediaPlayer.getDuration());

        new Thread(() -> {
            while (mediaPlayer != null && isPlaying) {
                try {
                    Thread.sleep(1000);
                    runOnUiThread(() -> {
                        if (mediaPlayer != null) {
                            songProgress.setProgress(mediaPlayer.getCurrentPosition());
                            timeElapsed.setText(formatTime(mediaPlayer.getCurrentPosition()));
                            timeTotal.setText(formatTime(mediaPlayer.getDuration()));
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Mover el SeekBar al cambiar su posición manualmente
        songProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    timeElapsed.setText(formatTime(mediaPlayer.getCurrentPosition()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No se necesita implementar nada aquí
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No se necesita implementar nada aquí
            }
        });
    }

    private String formatTime(int millis) {
        int minutes = (millis / 1000) / 60;
        int seconds = (millis / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    // Configuración de los controles de reproducción
    private void configurePlaybackControls() {
        btnPlay.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnPlay.setImageResource(R.drawable.ic_play); // Cambiar a icono de "play"
                isPlaying = false;
            } else if (mediaPlayer != null) {
                mediaPlayer.start();
                btnPlay.setImageResource(R.drawable.ic_pause); // Cambiar a icono de "pausa"
                isPlaying = true;
                updateProgressBar();
            }
        });

        btnNext.setOnClickListener(v -> {
            // Pasar a la siguiente canción manualmente
            if (currentSongIndex < songPaths.size() - 1) {
                currentSongIndex++;
                playSong(songPaths.get(currentSongIndex));
            } else {
                Toast.makeText(this, "No hay más canciones", Toast.LENGTH_SHORT).show();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            // Retroceder a la canción anterior manualmente
            if (currentSongIndex > 0) {
                currentSongIndex--;
                playSong(songPaths.get(currentSongIndex));
            } else {
                Toast.makeText(this, "Ya estás en la primera canción", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

}
