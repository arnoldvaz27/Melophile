package com.player.melophile;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class AlbumDetailsAdapter extends RecyclerView.Adapter<AlbumDetailsAdapter.MyHolder> {

    private Context mContext;
    static ArrayList<MusicFiles> albumFiles;
    View view;
    private static String siz;

    public AlbumDetailsAdapter(Context mContext, ArrayList<MusicFiles> albumFiles) {
        this.mContext = mContext;
        this.albumFiles = albumFiles;
    }


    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull  ViewGroup parent, int viewType) {
        view = LayoutInflater.from(mContext).inflate(R.layout.music_items,parent,false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull  MyHolder holder, int position) {
        holder.album_name.setText(albumFiles.get(position).getTitle());
        size(albumFiles.get(position).getSize());
        holder.album_name.setHorizontallyScrolling(true);
        holder.album_name.setSelected(true);
        int durationTotal = Integer.parseInt(albumFiles.get(position).getDuration()) / 1000;
        holder.album_size.setText(siz+", "+formatedTime(durationTotal)+" Mins");
        byte[] image = getAlbumArt(albumFiles.get(position).getPath());
        if(image != null){
            Glide.with(mContext).asBitmap().load(image).into(holder.album_image);
        }
        else{
            Glide.with(mContext).load(R.drawable.splashscreen).into(holder.album_image);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext,PlayerActivity.class);
                intent.putExtra("sender","albumDetails");
                intent.putExtra("position",position);
                mContext.startActivity(intent);
            }
        });
    }


    @Override
    public int getItemCount() {
        return albumFiles.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder{

        ImageView album_image;
        TextView album_name,album_size;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            album_image = itemView.findViewById(R.id.music_img);
            album_name = itemView.findViewById(R.id.music_file_name);
            album_size = itemView.findViewById(R.id.music_size);
        }
    }

    private byte[] getAlbumArt(String uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return  art;
    }

    public static String size(int size) {
        String modifiedFileSize = null;
        double fileSize = 0.0;

        fileSize = (double) size;//in Bytes
        if (fileSize < 1000) {
            modifiedFileSize = String.valueOf(fileSize).concat(" B");
        } else if (fileSize > 1024 && fileSize < (1000 * 1000)) {
            modifiedFileSize = String.valueOf(Math.round((fileSize / 1000 * 100.0)) / 100.0).concat(" KB");
        } else {
            modifiedFileSize = String.valueOf(Math.round((fileSize / (1000 * 1000) * 100.0)) / 100.0).concat(" MB");
        }
        siz = modifiedFileSize;
        return modifiedFileSize;
    }

    private String formatedTime(int mCurrentPosition) {
        String totalOut = "";
        String totalNew = "";
        String seconds = String.valueOf(mCurrentPosition % 60);
        String minutes = String.valueOf(mCurrentPosition / 60);
        totalOut = minutes + ":" + seconds;
        totalNew = minutes + ":" + "0" + seconds;
        if (seconds.length() == 1) {
            return totalNew;
        } else {
            return totalOut;
        }
    }
}
