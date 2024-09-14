package aplicacionesmoviles.debianita86.music2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class PlaylistDatabaseHelper extends SQLiteOpenHelper {
    // Nombre y versión de la base de datos
    private static final String DATABASE_NAME = "musicApp.db";
    private static final int DATABASE_VERSION = 1;

    // Constructor de la clase, que inicializa el ayudante de la base de datos con el nombre y versión especificados
    public PlaylistDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Método que se ejecuta la primera vez que se crea la base de datos
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear la tabla 'playlists' con dos columnas: id (clave primaria) y name (nombre de la playlist)
        db.execSQL("CREATE TABLE playlists (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");

        // Crear la tabla 'songs' con tres columnas: id (clave primaria), playlist_id (relación con la tabla playlists) y file_path (ruta del archivo de la canción)
        db.execSQL("CREATE TABLE songs (id INTEGER PRIMARY KEY AUTOINCREMENT, playlist_id INTEGER, file_path TEXT, FOREIGN KEY (playlist_id) REFERENCES playlists(id))");
    }

    // Método que se ejecuta cuando la base de datos necesita ser actualizada (por ejemplo, al cambiar la versión)
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Si la base de datos necesita ser actualizada, elimina las tablas existentes
        db.execSQL("DROP TABLE IF EXISTS songs");
        db.execSQL("DROP TABLE IF EXISTS playlists");

        // Crea nuevamente las tablas con las actualizaciones correspondientes
        onCreate(db);
    }

    // Método para obtener todas las playlists en la base de datos
    public ArrayList<String> getAllPlaylists() {
        ArrayList<String> playlists = new ArrayList<>(); // Lista para almacenar las playlists
        SQLiteDatabase db = this.getReadableDatabase();  // Obtén la base de datos en modo lectura
        Cursor cursor = db.rawQuery("SELECT name FROM playlists", null);  // Consulta para obtener el nombre de todas las playlists

        // Recorre los resultados y añade cada nombre de playlist a la lista
        if (cursor.moveToFirst()) {
            do {
                playlists.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();  // Cierra el cursor después de usarlo
        return playlists;  // Retorna la lista de playlists
    }

    // Método para agregar una nueva playlist a la base de datos
    public void addPlaylist(String playlistName) {
        SQLiteDatabase db = this.getWritableDatabase();  // Obtén la base de datos en modo escritura
        ContentValues values = new ContentValues();  // Crea un objeto para almacenar los valores de la nueva playlist
        values.put("name", playlistName);  // Añade el nombre de la playlist a los valores
        db.insert("playlists", null, values);  // Inserta los valores en la tabla playlists
    }

    // Método para eliminar una playlist de la base de datos
    public void deletePlaylist(String playlistName) {
        SQLiteDatabase db = this.getWritableDatabase();  // Obtén la base de datos en modo escritura

        // Elimina primero las canciones asociadas a la playlist usando el nombre de la playlist
        db.delete("songs", "playlist_id = (SELECT id FROM playlists WHERE name = ?)", new String[]{playlistName});

        // Elimina la playlist de la tabla playlists
        db.delete("playlists", "name = ?", new String[]{playlistName});
    }

    // Método para obtener todas las canciones de una playlist específica
    public ArrayList<String> getSongsFromPlaylist(String playlistName) {
        ArrayList<String> songs = new ArrayList<>();  // Lista para almacenar las canciones
        SQLiteDatabase db = this.getReadableDatabase();  // Obtén la base de datos en modo lectura

        // Consulta SQL para obtener las rutas de los archivos de las canciones de una playlist específica
        String query = "SELECT file_path FROM songs WHERE playlist_id = (SELECT id FROM playlists WHERE name = ?)";
        Cursor cursor = db.rawQuery(query, new String[]{playlistName});  // Ejecuta la consulta

        // Recorre los resultados y añade cada ruta de archivo a la lista de canciones
        if (cursor.moveToFirst()) {
            do {
                songs.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();  // Cierra el cursor después de usarlo
        return songs;  // Retorna la lista de canciones
    }

    // Método para agregar una canción a una playlist específica
    public void addSongToPlaylist(String playlistName, String filePath) {
        SQLiteDatabase db = this.getWritableDatabase();  // Obtén la base de datos en modo escritura

        // Consulta SQL para obtener el ID de la playlist dado su nombre
        String playlistIdQuery = "SELECT id FROM playlists WHERE name = ?";
        Cursor cursor = db.rawQuery(playlistIdQuery, new String[]{playlistName});  // Ejecuta la consulta

        // Si se encuentra la playlist, añade la canción a la tabla songs con el ID de la playlist
        if (cursor.moveToFirst()) {
            int playlistId = cursor.getInt(0);  // Obtén el ID de la playlist
            ContentValues values = new ContentValues();  // Crea un objeto para almacenar los valores de la nueva canción
            values.put("playlist_id", playlistId);  // Añade el ID de la playlist
            values.put("file_path", filePath);  // Añade la ruta del archivo de la canción
            db.insert("songs", null, values);  // Inserta los valores en la tabla songs
        }
        cursor.close();  // Cierra el cursor después de usarlo
    }

    // Método para eliminar una canción de una playlist específica
    public boolean deleteSongFromPlaylist(String playlistName, String filePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        String playlistIdQuery = "SELECT id FROM playlists WHERE name = ?";
        Cursor cursor = db.rawQuery(playlistIdQuery, new String[]{playlistName});
        boolean isDeleted = false;

        if (cursor.moveToFirst()) {
            int playlistId = cursor.getInt(0);
            Log.d("PlaylistDatabaseHelper", "Playlist ID: " + playlistId);

            // Elimina la canción de la tabla 'songs'
            int rowsAffected = db.delete("songs", "playlist_id = ? AND file_path = ?", new String[]{String.valueOf(playlistId), filePath});
            Log.d("PlaylistDatabaseHelper", "Rows affected: " + rowsAffected);
            Log.d("deleteSongFromPlaylist", "Playlist Name: " + playlistName);
            Log.d("deleteSongFromPlaylist", "File Path: " + filePath);

            // Si se afectó al menos una fila, la eliminación fue exitosa
            if (rowsAffected > 0) {
                isDeleted = true;
            }
        }

        cursor.close();
        return isDeleted; // Devuelve true si la eliminación fue exitosa
    }


}
