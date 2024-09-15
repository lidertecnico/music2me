package aplicacionesmoviles.debianita86.music2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SongListAdapter extends ArrayAdapter<String> {
    private Context context;
    private ArrayList<String> songPaths;
    private int playingPosition = -1; // Índice de la canción actual

    public SongListAdapter(Context context, ArrayList<String> songPaths) {
        super(context, 0, songPaths);
        this.context = context;
        this.songPaths = songPaths;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_song_2, parent, false);
        }

        String path = songPaths.get(position);
        String songName = new File(path).getName();
        String artistName = "Unknown Artist"; // Extract artist name if available

        TextView songNameView = convertView.findViewById(R.id.song_name);
        TextView songArtistView = convertView.findViewById(R.id.song_artist);
        ImageView thumbnailView = convertView.findViewById(R.id.song_thumbnail);

        songNameView.setText(songName);
        songArtistView.setText(artistName);

        // Optional: Load a thumbnail for the song if available
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                Bitmap albumArt = BitmapFactory.decodeByteArray(art, 0, art.length);
                thumbnailView.setImageBitmap(albumArt);
            } else {
                thumbnailView.setImageResource(R.drawable.ic_neo); // Default icon
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Manejo de errores si es necesario
            thumbnailView.setImageResource(R.drawable.ic_launcher_foreground); // Icono por defecto si ocurre un error
        } finally {
            // Asegurarse de liberar el retriever
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Cambiar el estado del item según la posición
        if (position == playingPosition) {
            convertView.setActivated(true);
        } else {
            convertView.setActivated(false);
        }

        return convertView;
    }


    public void setPlayingPosition(int position) {
        this.playingPosition = position;
        notifyDataSetChanged();
    }
}
