package com.player.melophile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Objects;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    private  static final int REQUEST_CODE = 1;
    static ArrayList<MusicFiles> musicFiles;
    static boolean shuffleButton = false , repeatButton = false;
    static ArrayList<MusicFiles> albums = new ArrayList<>();
    private EditText editText;
    private TextView typeName;
    private ImageView more,playlist,album;
    private String MY_SORT_PREF = "SortOrder";
    public static final String MUSIC_FILE_LAST_PLAYED = "LAST_PLAYED";
    public static final String MUSIC_FILE = "STORED_MUSIC";
    public static final String ARTIST_NAME = "ARTIST NAME";
    public static final String SONG_NAME = "SONG NAME";
    public static boolean SHOW_MINI_PLAYER = false;
    public static String PATH_TO_FRAG = null;
    public static String ARTIST_TO_FRAG = null;
    public static String SONG_NAME_TO_FRAG = null;
    RecyclerView recyclerView,recyclerView1;
    static MusicAdapter musicAdapter;
    AlbumAdapter albumAdapter;
    LinearLayout playlistLayout,albumLayout,search;
    private BottomSheetDialog bottomSheetDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getResources().getColor(R.color.red_color));
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_main);
        permission();

        if(!(musicFiles.size() < 1)){
            musicAdapter = new MusicAdapter(MainActivity.this,musicFiles);
            recyclerView.setAdapter(musicAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this,RecyclerView.VERTICAL,false));
        }

        if(!(albums.size() < 1)){
            albumAdapter = new AlbumAdapter(MainActivity.this,albums);
            recyclerView1.setAdapter(albumAdapter);
            recyclerView1.setLayoutManager(new GridLayoutManager(MainActivity.this,2));
        }
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String userInput = s.toString().toLowerCase();
                ArrayList<MusicFiles> myFiles = new ArrayList<>();
                for(MusicFiles song : musicFiles){
                    if(song.getTitle().toLowerCase().contains(userInput)){
                        myFiles.add(song);
                    }
                }
                MainActivity.musicAdapter.updateList(myFiles);
            }
        });
        playlist.setOnClickListener(v -> {
            playlist.setVisibility(View.GONE);
            album.setVisibility(View.VISIBLE);
            playlistLayout.setVisibility(View.VISIBLE);
            search.setVisibility(View.VISIBLE);
            typeName.setText("Playlist");
            albumLayout.setVisibility(View.GONE);
        });
        album.setOnClickListener(v -> {
            playlist.setVisibility(View.VISIBLE);
            album.setVisibility(View.GONE);
            playlistLayout.setVisibility(View.GONE);
            search.setVisibility(View.INVISIBLE);
            typeName.setText("Albums");
            albumLayout.setVisibility(View.VISIBLE);
        });
        more.setOnClickListener(v -> SortingBottom());
    }

    private void SortingBottom() {
        View sheetView;
        bottomSheetDialog = new BottomSheetDialog(MainActivity.this, R.style.BottomSheetTheme);
        SharedPreferences.Editor editor =  getSharedPreferences(MY_SORT_PREF,MODE_PRIVATE).edit();

        sheetView = LayoutInflater.from(MainActivity.this).inflate(R.layout.sortingbottomsheet, (ViewGroup) findViewById(R.id.sorting));

        LinearLayout linearLayout,linearLayout1,linearLayout2,linearLayout3,linearLayout4,linearLayout5;

        linearLayout = sheetView.findViewById(R.id.newestFirst);
        linearLayout1 = sheetView.findViewById(R.id.oldestFirst);
        linearLayout2 = sheetView.findViewById(R.id.largestFirst);
        linearLayout3 = sheetView.findViewById(R.id.smallestFirst);
        linearLayout4 = sheetView.findViewById(R.id.nameAZ);
        linearLayout5 = sheetView.findViewById(R.id.nameZA);

        linearLayout.setOnClickListener(v -> {
            editor.putString("sorting","sortByNewest");
            editor.apply();
            refreshRecycler();
            bottomSheetDialog.dismiss();
        });
        linearLayout1.setOnClickListener(v -> {
            editor.putString("sorting","sortByOldest");
            editor.apply();
            refreshRecycler();
            bottomSheetDialog.dismiss();
        });
        linearLayout2.setOnClickListener(v -> {
            editor.putString("sorting","sortByLargest");
            editor.apply();
            refreshRecycler();
            bottomSheetDialog.dismiss();
        });
        linearLayout3.setOnClickListener(v -> {
            editor.putString("sorting","sortBySmallest");
            editor.apply();
            refreshRecycler();
            bottomSheetDialog.dismiss();
        });
        linearLayout4.setOnClickListener(v -> {
            editor.putString("sorting","sortByAZ");
            editor.apply();
            refreshRecycler();
            bottomSheetDialog.dismiss();
        });
        linearLayout5.setOnClickListener(v -> {
            editor.putString("sorting","sortByZA");
            editor.apply();
            refreshRecycler();
            bottomSheetDialog.dismiss();
        });
        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private void refreshRecycler() {
        musicFiles = getAllAudio(this);
        initViewPager();
        if(!(musicFiles.size() < 1)){
            musicAdapter = new MusicAdapter(MainActivity.this,musicFiles);
            recyclerView.setAdapter(musicAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this,RecyclerView.VERTICAL,false));
        }

        if(!(albums.size() < 1)){
            albumAdapter = new AlbumAdapter(MainActivity.this,albums);
            recyclerView1.setAdapter(albumAdapter);
            recyclerView1.setLayoutManager(new GridLayoutManager(MainActivity.this,2));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull  String[] permissions, @NonNull  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                musicFiles = getAllAudio(this);
                initViewPager();
            }
            else{
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE);
            }
        }
    }

    private void permission() {
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE);
        }
        else{
            musicFiles = getAllAudio(this);
            initViewPager();
        }
    }

    private void initViewPager() {
        editText = findViewById(R.id.search);
        more = findViewById(R.id.more);
        album = findViewById(R.id.appIcon2);
        playlist = findViewById(R.id.appIcon);
        albumLayout = findViewById(R.id.LayoutAlbums);
        playlistLayout = findViewById(R.id.LayoutPlaylist);
        search = findViewById(R.id.LayoutSearch);
        typeName = findViewById(R.id.typeName);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView1 = findViewById(R.id.recyclerViewAlbum);
        recyclerView1.setHasFixedSize(true);
    }

    public ArrayList<MusicFiles> getAllAudio(Context context){
        SharedPreferences preferences = getSharedPreferences(MY_SORT_PREF,MODE_PRIVATE);
        String sortOrder = preferences.getString("sorting","sortByName");
        ArrayList<String> duplicate = new ArrayList<>();
        albums.clear();
        ArrayList<MusicFiles> tempAudioList = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String order = null;
        switch (sortOrder){
            case "sortByNewest":
                order = MediaStore.MediaColumns.DATE_ADDED + " DESC";
                break;
            case "sortByOldest":
                order = MediaStore.MediaColumns.DATE_ADDED + " ASC";
                break;
            case "sortByLargest":
                order = MediaStore.MediaColumns.SIZE + " DESC";
                break;
            case "sortBySmallest":
                order = MediaStore.MediaColumns.SIZE + " ASC";
                break;
            case "sortByAZ":
                order = MediaStore.MediaColumns.DISPLAY_NAME + " ASC";
                break;
            case "sortByZA":
                order = MediaStore.MediaColumns.DISPLAY_NAME + " DESC";
                break;
        }
        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.SIZE
        };
        Cursor cursor = context.getContentResolver().query(uri,projection,
                null,null,order);
        if(cursor != null){
            while (cursor.moveToNext()){
                String album = cursor.getString(0);
                String title = cursor.getString(1);
                String duration = cursor.getString(2);
                String path = cursor.getString(3);
                String artist = cursor.getString(4);
                String id = cursor.getString(5);
                int size = cursor.getInt(6);
                MusicFiles musicFiles = new MusicFiles(path,title,artist,album,duration,id,size);
                tempAudioList.add(musicFiles);
                if(!duplicate.contains(album)){
                    albums.add(musicFiles);
                    duplicate.add(album);
                }
            }
            cursor.close();
        }
        return  tempAudioList;
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences = getSharedPreferences(MUSIC_FILE_LAST_PLAYED,MODE_PRIVATE);
        String value = preferences.getString(MUSIC_FILE, null);

        if(value != null){
            SHOW_MINI_PLAYER = true;
            PATH_TO_FRAG = value;
        }
        else{
            SHOW_MINI_PLAYER = false;
            PATH_TO_FRAG = null;

        }
    }


}