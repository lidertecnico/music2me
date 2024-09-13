package aplicacionesmoviles.debianita86.music2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SongPlayActivity extends AppCompatActivity {
    private ListView listViewSongs;
    private String playlistName;
    private PlaylistDatabaseHelper dbHelper;
    private ArrayList<String> songPaths;
    private ArrayAdapter<String> adapter;
    private MediaPlayer mediaPlayer;
    private BluetoothAdapter bluetoothAdapter;

    // Views del CardView
    private ImageView albumCover;
    private TextView songTitle, artistName, timeElapsed, timeTotal;
    private SeekBar songProgress;
    private ImageButton btnPlay, btnNext, btnPrevious;
    private int currentSongIndex = 0; // Índice de la canción actual
    private boolean isPlaying = false; // Controlar si la canción está en pausa o en reproducción

    // MediaSession para controlar los eventos multimedia
    private MediaSession mediaSession;

    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;

    // ActivityResultLauncher para manejar la habilitación de Bluetooth
    private ActivityResultLauncher<Intent> bluetoothActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, "Bluetooth habilitado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Bluetooth no habilitado", Toast.LENGTH_SHORT).show();
                }
            }
    );

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

        // Verificar Bluetooth
        setupBluetooth();

        // Configurar botones de reproducción
        configurePlaybackControls();

        // Registrar el callback para manejar el botón "atrás"
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(SongPlayActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Evita múltiples instancias de MainActivity
                startActivity(intent); // Inicia MainActivity
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        // Configurar la MediaSession para capturar los eventos de control de medios
        setupMediaSession();
    }

    // Configurar Bluetooth y comprobar si está habilitado
    private void setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no soportado en este dispositivo", Toast.LENGTH_SHORT).show();
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothActivityResultLauncher.launch(enableBtIntent);
        }
    }

    // BroadcastReceiver para detectar conexión y desconexión de dispositivos Bluetooth
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("BluetoothReceiver", "Action received: " + action);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.d("BluetoothReceiver", "Device connected");
                Toast.makeText(context, "Dispositivo Bluetooth conectado", Toast.LENGTH_SHORT).show();
                // Reproduce la canción actual al conectar el Bluetooth
                if (!isPlaying && mediaPlayer != null) {
                    playSong(songPaths.get(currentSongIndex));
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.d("BluetoothReceiver", "Device disconnected");
                Toast.makeText(context, "Dispositivo Bluetooth desconectado", Toast.LENGTH_SHORT).show();
                // Pausar la canción cuando se desconecta el Bluetooth
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnPlay.setImageResource(R.drawable.ic_play); // Cambiar el botón a "play"
                    isPlaying = false;
                }
            }
        }
    };

    // Registrar y desregistrar el BroadcastReceiver en onResume y onPause
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(bluetoothReceiver);
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                bluetoothActivityResultLauncher.launch(intent); // Actualizado para usar ActivityResultLauncher
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

    // Método para cargar las canciones de la base de datos
    private void loadSongs() {
        songPaths = dbHelper.getSongsFromPlaylist(playlistName); // Cargar las URIs desde la base de datos
        ArrayList<String> songNames = new ArrayList<>();

        // Extraer solo los nombres de los archivos a partir de las rutas
        for (String path : songPaths) {
            songNames.add(new File(path).getName());
        }

        // Mostrar los nombres de los archivos en el ListView
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, songNames);
        listViewSongs.setAdapter(adapter);

        // Al seleccionar una canción, reproducirla y actualizar los detalles
        listViewSongs.setOnItemClickListener((parent, view, position, id) -> {
            currentSongIndex = position; // Actualizar el índice de la canción actual
            playSong(songPaths.get(currentSongIndex)); // Mantener la ruta completa al reproducir la canción
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

        songProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    timeElapsed.setText(formatTime(mediaPlayer.getCurrentPosition()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    // Configurar los controles de reproducción
    private void configurePlaybackControls() {
        btnPlay.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnPlay.setImageResource(R.drawable.ic_play);
                isPlaying = false;
            } else if (mediaPlayer != null) {
                mediaPlayer.start();
                btnPlay.setImageResource(R.drawable.ic_pause);
                isPlaying = true;
                updateProgressBar();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentSongIndex < songPaths.size() - 1) {
                currentSongIndex++;
                playSong(songPaths.get(currentSongIndex));
            } else {
                Toast.makeText(this, "No hay más canciones", Toast.LENGTH_SHORT).show();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (currentSongIndex > 0) {
                currentSongIndex--;
                playSong(songPaths.get(currentSongIndex));
            } else {
                Toast.makeText(this, "Ya estás en la primera canción", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Configurar MediaSession para capturar eventos de control de medios
    private void setupMediaSession() {
        // Crear una nueva MediaSession
        mediaSession = new MediaSession(this, "MusicSession");

        // Configurar el estado inicial (idle, es decir, no está reproduciendo)
        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS);

        mediaSession.setPlaybackState(stateBuilder.build());

        // Asignar el callback para manejar eventos de control (play, pause, etc.)
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                    btnPlay.setImageResource(R.drawable.ic_pause);
                    isPlaying = true;
                    updateProgressBar();
                    mediaSession.setActive(true); // Activar la sesión cuando se reproduce
                }
            }

            @Override
            public void onPause() {
                super.onPause();
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnPlay.setImageResource(R.drawable.ic_play);
                    isPlaying = false;
                }
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                if (currentSongIndex < songPaths.size() - 1) {
                    currentSongIndex++;
                    playSong(songPaths.get(currentSongIndex));
                }
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                if (currentSongIndex > 0) {
                    currentSongIndex--;
                    playSong(songPaths.get(currentSongIndex));
                }
            }
        });

        // Activar la sesión para recibir eventos
        mediaSession.setActive(true);
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

        // Liberar la MediaSession al destruir la actividad
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
    }

    private String formatTime(int millis) {
        int minutes = (millis / 1000) / 60;
        int seconds = (millis / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}