package com.player.melophile;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {

    private Context mContext;
    static ArrayList<MusicFiles> mFiles;
    private static String siz;

    MusicAdapter(Context mContext, ArrayList<MusicFiles> mFiles){
        MusicAdapter.mFiles = mFiles;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull  ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.music_items,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicAdapter.MyViewHolder holder, int position) {
        holder.file_name.setText(mFiles.get(position).getTitle());
        size(mFiles.get(position).getSize());
        holder.file_name.setHorizontallyScrolling(true);
        holder.file_name.setSelected(true);
        int durationTotal = Integer.parseInt(mFiles.get(position).getDuration()) / 1000;
        holder.file_size.setText(siz+", "+formatedTime(durationTotal)+" Mins");
        byte[] image = getAlbumArt(mFiles.get(position).getPath());
        if(image != null){
            Glide.with(mContext).asBitmap().load(image).into(holder.album_art);
        }
        else{
            Glide.with(mContext).load(R.drawable.splashscreen).into(holder.album_art);
        }
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(mContext,PlayerActivity.class);
            intent.putExtra("position",position);
            mContext.startActivity(intent);
        });
        holder.menuMore.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(mContext, v);
            popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());
            popupMenu.show();

            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.delete) {
                    deleteFile(position, v);
                }
                if (item.getItemId() == R.id.more_options) {
                    Uri uri = Uri.parse(mFiles.get(position).getPath());
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("audio/*");
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    mContext.startActivity(Intent.createChooser(share, "Share Sound File"));
                }
                return true;
            });
        });
    }

    private void deleteFile(int position, View view){
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,Long.parseLong(mFiles.get(position).getId()));
        File file = new File(mFiles.get(position).getPath());
        boolean deleted = file.delete();
        if(deleted){
            mContext.getContentResolver().delete(contentUri,null,null);
            mFiles.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position,mFiles.size());
            Snackbar.make(view,"File Deleted : ",Snackbar.LENGTH_LONG)
                    .show();
        }
        else{
            notifyItemRangeChanged(position,mFiles.size());
            Snackbar.make(view,"File Can't be Deleted : ",Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{

        TextView file_name,file_size;
        ImageView album_art,menuMore;
        public MyViewHolder(@NonNull View itemView){
            super(itemView);
            file_name = itemView.findViewById(R.id.music_file_name);
            album_art = itemView.findViewById(R.id.music_img);
            file_size = itemView.findViewById(R.id.music_size);
            menuMore = itemView.findViewById(R.id.menu_more);
        }
    }

    private byte[] getAlbumArt(String uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return  art;
    }

    void updateList(ArrayList<MusicFiles> musicFilesArrayList){
        mFiles = new ArrayList<>();
        mFiles.addAll(musicFilesArrayList);
        notifyDataSetChanged();
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
