package aplicacionesmoviles.debianita86.music2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class PlaylistDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "musicApp.db";
    private static final int DATABASE_VERSION = 1;

    // Constructor
    public PlaylistDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear tabla para playlists
        db.execSQL("CREATE TABLE playlists (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");

        // Crear tabla para canciones con relación de clave foránea a playlists
        db.execSQL("CREATE TABLE songs (id INTEGER PRIMARY KEY AUTOINCREMENT, playlist_id INTEGER, file_path TEXT, FOREIGN KEY (playlist_id) REFERENCES playlists(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Actualizar la base de datos si es necesario
        db.execSQL("DROP TABLE IF EXISTS songs");
        db.execSQL("DROP TABLE IF EXISTS playlists");
        onCreate(db);
    }

    // Obtener todas las playlists
    public ArrayList<String> getAllPlaylists() {
        ArrayList<String> playlists = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM playlists", null);
        if (cursor.moveToFirst()) {
            do {
                playlists.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return playlists;
    }

    // Agregar nueva playlist
    public void addPlaylist(String playlistName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", playlistName);
        db.insert("playlists", null, values);
    }

    // Eliminar una playlist
    public void deletePlaylist(String playlistName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("songs", "playlist_id = (SELECT id FROM playlists WHERE name = ?)", new String[]{playlistName});
        db.delete("playlists", "name = ?", new String[]{playlistName});
    }

    // Obtener canciones de una playlist
    public ArrayList<String> getSongsFromPlaylist(String playlistName) {
        ArrayList<String> songs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT file_path FROM songs WHERE playlist_id = (SELECT id FROM playlists WHERE name = ?)";
        Cursor cursor = db.rawQuery(query, new String[]{playlistName});
        if (cursor.moveToFirst()) {
            do {
                songs.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    // Agregar canción a una playlist
    public void addSongToPlaylist(String playlistName, String filePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        String playlistIdQuery = "SELECT id FROM playlists WHERE name = ?";
        Cursor cursor = db.rawQuery(playlistIdQuery, new String[]{playlistName});
        if (cursor.moveToFirst()) {
            int playlistId = cursor.getInt(0);
            ContentValues values = new ContentValues();
            values.put("playlist_id", playlistId);
            values.put("file_path", filePath);
            db.insert("songs", null, values);
        }
        cursor.close();
    }
    // Eliminar una canción de la playlist
    public void deleteSongFromPlaylist(String playlistName, String filePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        String playlistIdQuery = "SELECT id FROM playlists WHERE name = ?";
        Cursor cursor = db.rawQuery(playlistIdQuery, new String[]{playlistName});
        if (cursor.moveToFirst()) {
            int playlistId = cursor.getInt(0);
            db.delete("songs", "playlist_id = ? AND file_path = ?", new String[]{String.valueOf(playlistId), filePath});
        }
        cursor.close();
    }

}