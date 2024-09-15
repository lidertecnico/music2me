package aplicacionesmoviles.debianita86.music2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
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
    private SongListAdapter adapter;
    private MediaPlayer mediaPlayer;
    private BluetoothAdapter bluetoothAdapter;

    // Vistas del CardView para mostrar la información de la canción
    private ImageView albumCover;
    private TextView songTitle, artistName, timeElapsed, timeTotal;
    private SeekBar songProgress;
    private ImageButton btnPlay, btnNext, btnPrevious;
    private int currentSongIndex = 0; // Índice de la canción actual
    private boolean isPlaying = false; // Controla si la canción está en reproducción o en pausa

    // MediaSession para capturar los eventos multimedia (play, pause, etc.)
    private MediaSession mediaSession;

    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;

    // ActivityResultLauncher para manejar la habilitación de Bluetooth
    private ActivityResultLauncher<Intent> bluetoothActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Notifica al usuario si el Bluetooth se habilitó correctamente
                    Toast.makeText(this, "Bluetooth habilitado", Toast.LENGTH_SHORT).show();
                } else {
                    // Notifica al usuario si no se habilitó el Bluetooth
                    Toast.makeText(this, "Bluetooth no habilitado", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_play);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Enlaza los componentes visuales con sus correspondientes IDs del layout
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

        dbHelper = new PlaylistDatabaseHelper(this); // Inicializa la base de datos para las playlists

        // Obtiene el nombre de la playlist pasada por Intent
        playlistName = getIntent().getStringExtra("playlistName");
        if (playlistName == null) {
            // Si no se recibe el nombre de la playlist, muestra un error y cierra la actividad
            Toast.makeText(this, "Error: Playlist name no recibido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Verifica y solicita los permisos necesarios para acceder al almacenamiento
        checkStoragePermission();

        // Configura el Bluetooth
        setupBluetooth();

        // Configura los controles de reproducción (botones de play, next, previous)
        configurePlaybackControls();

        // Configura el comportamiento del botón de "atrás" de Android
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Vuelve a la MainActivity cuando se presiona el botón "atrás"
                Intent intent = new Intent(SongPlayActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        // Configura la MediaSession para capturar los eventos de control de medios
        setupMediaSession();
    }

    // Configurar Bluetooth y comprobar si está habilitado
    private void setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Obtiene el adaptador Bluetooth
        if (bluetoothAdapter == null) {
            // Si el dispositivo no soporta Bluetooth, muestra un mensaje
            Toast.makeText(this, "Bluetooth no soportado en este dispositivo", Toast.LENGTH_SHORT).show();
        } else if (!bluetoothAdapter.isEnabled()) {
            // Si el Bluetooth no está habilitado, solicita habilitarlo
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
                // Si un dispositivo Bluetooth se conecta, muestra un mensaje y reproduce la canción
                Log.d("BluetoothReceiver", "Device connected");
                Toast.makeText(context, "Dispositivo Bluetooth conectado", Toast.LENGTH_SHORT).show();
                if (!isPlaying && mediaPlayer != null) {
                    playSong(songPaths.get(currentSongIndex));
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                // Si un dispositivo Bluetooth se desconecta, pausa la reproducción y actualiza el botón
                Log.d("BluetoothReceiver", "Device disconnected");
                Toast.makeText(context, "Dispositivo Bluetooth desconectado", Toast.LENGTH_SHORT).show();
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnPlay.setImageResource(R.drawable.ic_play);
                    isPlaying = false;
                }
            }
        }
    };

    // Registra y desregistra el BroadcastReceiver cuando la actividad se reanuda o se pausa
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

    // Verifica si se tiene el permiso de almacenamiento y lo solicita si no está concedido
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Para versiones de Android 11 o superiores, solicita acceso a todos los archivos
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                bluetoothActivityResultLauncher.launch(intent);
            } else {
                loadSongs(); // Carga las canciones si se tiene el permiso
            }
        } else {
            // Para versiones anteriores a Android 11, solicita el permiso de lectura de almacenamiento
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                loadSongs(); // Carga las canciones si el permiso ya está concedido
            }
        }
    }

    // Método para cargar las canciones desde la base de datos
    private void loadSongs() {
        songPaths = dbHelper.getSongsFromPlaylist(playlistName); // Obtiene las rutas de las canciones de la base de datos
        ArrayList<String> songNames = new ArrayList<>();

        // Extrae solo el nombre de cada archivo de la ruta completa
        for (String path : songPaths) {
            songNames.add(new File(path).getName());
        }

        // Configura el adaptador para mostrar los nombres de las canciones en el ListView
        // Inicializar el adaptador con el tipo SongListAdapter
        adapter = new SongListAdapter(this, songPaths);
        listViewSongs.setAdapter(adapter);

        listViewSongs.setOnItemClickListener((parent, view, position, id) -> {
            currentSongIndex = position;
            playSong(songPaths.get(currentSongIndex));
        });
    }

    // Método para reproducir una canción dada su URI
    private void playSong(String songUriString) {
        Uri songUri = Uri.parse(songUriString);

        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();

        try {
            File songFile = new File(songUri.getPath());
            String songName = songFile.getName();
            String artist = "Artista Desconocido";

            if (songName.contains("-")) {
                String[] parts = songName.split("-");
                artist = parts[0].trim();
                songName = parts[1].trim();
            }

            if (songName.length() > 20) {
                songName = songName.substring(0, 20) + "...";
            }
            if (artist.length() > 20) {
                artist = artist.substring(0, 20) + "...";
            }

            songTitle.setText(songName);
            artistName.setText(artist);

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(songUri.getPath());
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                Bitmap albumArt = BitmapFactory.decodeByteArray(art, 0, art.length);
                albumCover.setImageBitmap(albumArt);
            } else {
                albumCover.setImageResource(R.drawable.ic_neo);
            }
            retriever.release();

            mediaPlayer.setDataSource(getApplicationContext(), songUri);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mediaPlayer.start();
                btnPlay.setImageResource(R.drawable.ic_pause);
                isPlaying = true;
                updateProgressBar();

                // Actualizar la posición en el adaptador
                adapter.setPlayingPosition(currentSongIndex);
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



    // Método para actualizar la barra de progreso de la canción
    private void updateProgressBar() {
        songProgress.setMax(mediaPlayer.getDuration());

        // Hilo que actualiza la barra de progreso cada segundo
        new Thread(() -> {
            while (mediaPlayer != null && isPlaying) {
                try {
                    Thread.sleep(1000); // Espera un segundo entre actualizaciones
                    runOnUiThread(() -> {
                        if (mediaPlayer != null) {
                            songProgress.setProgress(mediaPlayer.getCurrentPosition()); // Actualiza el progreso
                            timeElapsed.setText(formatTime(mediaPlayer.getCurrentPosition())); // Muestra el tiempo transcurrido
                            timeTotal.setText(formatTime(mediaPlayer.getDuration())); // Muestra el tiempo total
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Listener para manejar los cambios en la barra de progreso
        songProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Permite al usuario avanzar o retroceder en la canción
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

    // Configura los controles de reproducción (botones de play, next, previous)
    private void configurePlaybackControls() {
        btnPlay.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause(); // Pausa la canción si está reproduciendo
                btnPlay.setImageResource(R.drawable.ic_play); // Cambia el icono a "play"
                isPlaying = false;
            } else if (mediaPlayer != null) {
                mediaPlayer.start(); // Reproduce la canción si estaba en pausa
                btnPlay.setImageResource(R.drawable.ic_pause); // Cambia el icono a "pausa"
                isPlaying = true;
                updateProgressBar(); // Actualiza la barra de progreso
            }
        });

        // Avanza a la siguiente canción cuando se presiona el botón de "next"
        btnNext.setOnClickListener(v -> {
            if (currentSongIndex < songPaths.size() - 1) {
                currentSongIndex++;
                playSong(songPaths.get(currentSongIndex));
            } else {
                Toast.makeText(this, "No hay más canciones", Toast.LENGTH_SHORT).show();
            }
        });

        // Retrocede a la canción anterior cuando se presiona el botón de "previous"
        btnPrevious.setOnClickListener(v -> {
            if (currentSongIndex > 0) {
                currentSongIndex--;
                playSong(songPaths.get(currentSongIndex));
            } else {
                Toast.makeText(this, "Ya estás en la primera canción", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Configura la MediaSession para capturar eventos de control multimedia
    private void setupMediaSession() {
        mediaSession = new MediaSession(this, "MusicSession");

        // Define las acciones posibles (play, pause, skip)
        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS);

        mediaSession.setPlaybackState(stateBuilder.build());

        // Configura los callbacks para manejar los eventos de control multimedia
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                // Maneja el evento de "play"
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                    btnPlay.setImageResource(R.drawable.ic_pause);
                    isPlaying = true;
                    updateProgressBar();
                    mediaSession.setActive(true);
                }
            }

            @Override
            public void onPause() {
                // Maneja el evento de "pause"
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnPlay.setImageResource(R.drawable.ic_play);
                    isPlaying = false;
                }
            }

            @Override
            public void onSkipToNext() {
                // Maneja el evento de "next"
                if (currentSongIndex < songPaths.size() - 1) {
                    currentSongIndex++;
                    playSong(songPaths.get(currentSongIndex));
                }
            }

            @Override
            public void onSkipToPrevious() {
                // Maneja el evento de "previous"
                if (currentSongIndex > 0) {
                    currentSongIndex--;
                    playSong(songPaths.get(currentSongIndex));
                }
            }
        });

        mediaSession.setActive(true); // Activa la MediaSession
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Libera el MediaPlayer al destruir la actividad
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Libera la MediaSession
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
    }

    // Método auxiliar para formatear el tiempo en minutos y segundos
    private String formatTime(int millis) {
        int minutes = (millis / 1000) / 60;
        int seconds = (millis / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
