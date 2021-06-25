package com.player.melophile;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.palette.graphics.Palette;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.player.melophile.AlbumDetailsAdapter.albumFiles;
import static com.player.melophile.MainActivity.repeatButton;
import static com.player.melophile.MainActivity.shuffleButton;
import static com.player.melophile.MusicAdapter.mFiles;

@SuppressLint("StaticFieldLeak")
public class PlayerActivity extends AppCompatActivity
        implements ActionPlaying, ServiceConnection, onSwipeListener {

    TextView song_name, artist_name, duration_played, duration_total, volumePercentage;
    ImageView cover_art, nextBtn, prevBtn, backBtn, shuffleBtn, repeatBtn, moreOptions, volumeImage, goBack, info;
    private BottomSheetDialog bottomSheetDialog, music_info;

    static SeekBar seekBar;
    static int position = -1;
    static ArrayList<MusicFiles> listSongs = new ArrayList<>();
    static Uri uri;
    //    static MediaPlayer mediaPlayer;
    static final Handler handler = new Handler();
    private Thread playThread, prevThread, nextThread;
    static MusicService musicService;
    @SuppressLint("StaticFieldLeak")
    static ImageView play;
    String sharePath;
    OnSwipeTouchListener onSwipeTouchListener;
    private List<File> files;
    private static String siz;
    TextView textView, textView1, textView2, textView3, textView4, textView5;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.holo_red_dark));
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_player);

        initView();
        getIntentMethod();

        onSwipeTouchListener = new OnSwipeTouchListener(this, findViewById(R.id.mContainer));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (musicService != null && fromUser) {
                    musicService.seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (musicService != null) {
                    int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                    seekBar.setProgress(mCurrentPosition);
                    duration_played.setText(formatedTime(mCurrentPosition));

                }
                handler.postDelayed(this, 1000);
            }
        });

        goBack.setOnClickListener(v -> finish());
        info.setOnClickListener(v -> infoBottom());
        shuffleBtn.setOnClickListener(v -> {
            if (shuffleButton) {
                shuffleButton = false;
                shuffleBtn.setImageResource(R.drawable.shuffle_off);
            } else {
                shuffleButton = true;
                shuffleBtn.setImageResource(R.drawable.shuffle);
            }
        });
        repeatBtn.setOnClickListener(v -> {
            if (repeatButton) {
                repeatButton = false;
                repeatBtn.setImageResource(R.drawable.repeat_off);
            } else {
                repeatButton = true;
                repeatBtn.setImageResource(R.drawable.repeat);
            }
        });
        moreOptions.setOnClickListener(v -> BottomLayout());

        volumeImage.setOnClickListener(v -> {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        });
    }

    @SuppressLint("SetTextI18n")
    private void infoBottom() {
        View sheetView;
        music_info = new BottomSheetDialog(PlayerActivity.this, R.style.BottomSheetTheme);

        sheetView = LayoutInflater.from(PlayerActivity.this).inflate(R.layout.music_info, (ViewGroup) findViewById(R.id.musicInfo));

        size(mFiles.get(position).getSize());

        textView = sheetView.findViewById(R.id.music_name);
        textView1 = sheetView.findViewById(R.id.music_artist);
        textView2 = sheetView.findViewById(R.id.music_modified);
        textView3 = sheetView.findViewById(R.id.music_duration);
        textView4 = sheetView.findViewById(R.id.music_path);
        textView5 = sheetView.findViewById(R.id.music_size);

        textView.setText(song_name.getText().toString());
        textView1.setText(artist_name.getText().toString());
        textView3.setText("Duration - " + duration_total.getText().toString() + " minutes");
        textView4.setText(mFiles.get(position).getPath());
        textView5.setText(siz);

        textView.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Music Name", textView.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(PlayerActivity.this, "Music name copied to Clipboard", Toast.LENGTH_SHORT).show();
            return true;
        });
        textView4.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Music Path", textView4.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(PlayerActivity.this, "Path copied to Clipboard", Toast.LENGTH_SHORT).show();
            return true;
        });
        textView1.setOnLongClickListener(v -> {
            if (textView1.getText().toString().equals("Artist N/A")) {
                Toast.makeText(PlayerActivity.this, "Name of the artist could not be copied", Toast.LENGTH_SHORT).show();
            } else {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Music Artist", textView1.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(PlayerActivity.this, "Artist name copied to Clipboard", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        music_info.setContentView(sheetView);
        music_info.show();

    }

    private void BottomLayout() {
        View sheetView;
        bottomSheetDialog = new BottomSheetDialog(PlayerActivity.this, R.style.BottomSheetTheme);

        sheetView = LayoutInflater.from(PlayerActivity.this).inflate(R.layout.layout_more_options, (ViewGroup) findViewById(R.id.layoutMoreOptions));

        LinearLayout linearLayout, linearLayout1, linearLayout2, linearLayout3, linearLayout4, linearLayout5, linearLayout6;

        linearLayout = sheetView.findViewById(R.id.share);
        linearLayout1 = sheetView.findViewById(R.id.findLyrics);
        linearLayout2 = sheetView.findViewById(R.id.setAlarm);
        linearLayout3 = sheetView.findViewById(R.id.setRingtone);
        linearLayout4 = sheetView.findViewById(R.id.findGoogle);
        linearLayout5 = sheetView.findViewById(R.id.youtubeVideo);
        linearLayout6 = sheetView.findViewById(R.id.openPlayer);


        linearLayout.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Uri uri = Uri.parse(sharePath);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("audio/*");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(share, "Share Sound File"));
        });
        linearLayout1.setOnClickListener(v -> {

        });
        linearLayout2.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(getApplicationContext())) {
                    File ringtoneFile = new File(sharePath);
                    ContentValues content = new ContentValues();
                    content.put(MediaStore.MediaColumns.DATA, ringtoneFile.getAbsolutePath());
                    content.put(MediaStore.MediaColumns.TITLE, listSongs.get(position).getTitle());
                    content.put(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.SIZE);
                    content.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*");
                    content.put(MediaStore.Audio.Media.ARTIST, listSongs.get(position).getArtist());
                    content.put(MediaStore.Audio.Media.DURATION, listSongs.get(position).getDuration());
                    content.put(MediaStore.Audio.Media.IS_RINGTONE, false);
                    content.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                    content.put(MediaStore.Audio.Media.IS_ALARM, true);
                    content.put(MediaStore.Audio.Media.IS_MUSIC, false);
                    Uri uri = MediaStore.Audio.Media.getContentUriForPath(
                            ringtoneFile.getAbsolutePath());
                    Uri newUri = getApplicationContext().getContentResolver().insert(uri, content);
                    RingtoneManager.setActualDefaultRingtoneUri(getApplicationContext(),
                            RingtoneManager.TYPE_ALARM, newUri);
                    final Uri currentTone =
                            RingtoneManager.getActualDefaultRingtoneUri(PlayerActivity.this,
                                    RingtoneManager.TYPE_ALARM);
                    Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentTone);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                    startActivityForResult(intent, 999);
                } else {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + PlayerActivity.this.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }

        });
        linearLayout3.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(getApplicationContext())) {
                    File ringtoneFile = new File(sharePath);
                    ContentValues content = new ContentValues();
                    content.put(MediaStore.MediaColumns.DATA, ringtoneFile.getAbsolutePath());
                    content.put(MediaStore.MediaColumns.TITLE, listSongs.get(position).getTitle());
                    content.put(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.SIZE);
                    content.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*");
                    content.put(MediaStore.Audio.Media.ARTIST, listSongs.get(position).getArtist());
                    content.put(MediaStore.Audio.Media.DURATION, listSongs.get(position).getDuration());
                    content.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                    content.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                    content.put(MediaStore.Audio.Media.IS_ALARM, false);
                    content.put(MediaStore.Audio.Media.IS_MUSIC, false);
                    Uri uri = MediaStore.Audio.Media.getContentUriForPath(
                            ringtoneFile.getAbsolutePath());
                    Uri newUri = getApplicationContext().getContentResolver().insert(uri, content);
                    RingtoneManager.setActualDefaultRingtoneUri(getApplicationContext(),
                            RingtoneManager.TYPE_RINGTONE, newUri);
                    final Uri currentTone =
                            RingtoneManager.getActualDefaultRingtoneUri(PlayerActivity.this,
                                    RingtoneManager.TYPE_RINGTONE);
                    Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentTone);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                    startActivityForResult(intent, 999);
                } else {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + PlayerActivity.this.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }
        });
        linearLayout4.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Music Name", textView.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(PlayerActivity.this, "Artist name copied to Clipboard", Toast.LENGTH_SHORT).show();

            try {

                PackageManager pm = getPackageManager();
                Intent intent2 = pm.getLaunchIntentForPackage("com.android.chrome");
                startActivity(intent2);
            } catch (Exception e) {
                Intent intent;
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com"));
                startActivity(intent);
            }
        });
        linearLayout5.setOnClickListener(v -> {

        });
        linearLayout6.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Uri uri = Uri.parse(sharePath);
            Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
            pdfIntent.setDataAndType(uri, "audio/x-wav");
            pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                musicService.pause();
                play.setImageResource(R.drawable.play);
                startActivity(pdfIntent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(PlayerActivity.this, "No Applications found to open this format file. You can download relevant application to view this file format", Toast.LENGTH_LONG).show();
            }
        });
        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();

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

    @SuppressLint("ResourceType")
    private void getIntentMethod() {
        position = getIntent().getIntExtra("position", -1);
        String sender = getIntent().getStringExtra("sender");
        if (sender != null && sender.equals("albumDetails")) {
            listSongs = albumFiles;
        } else {
            listSongs = mFiles;
        }

        if (listSongs != null) {
            play.setImageResource(R.drawable.pause);
            uri = Uri.parse(listSongs.get(position).getPath());
        }
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("servicePosition", position);
        startService(intent);
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        song_name = findViewById(R.id.songName);
        artist_name = findViewById(R.id.songArtist);
        duration_played = findViewById(R.id.durationPlayed);
        duration_total = findViewById(R.id.durationTotal);
        cover_art = findViewById(R.id.cover_art);
        nextBtn = findViewById(R.id.skip_next);
        prevBtn = findViewById(R.id.skip_previous);
        shuffleBtn = findViewById(R.id.shuffle);
        repeatBtn = findViewById(R.id.repeat);
        play = findViewById(R.id.playPause);
        seekBar = findViewById(R.id.seekBar);
        moreOptions = findViewById(R.id.more_options);
        volumeImage = findViewById(R.id.volumeImage);
        goBack = findViewById(R.id.goBack);
        info = findViewById(R.id.info);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);

    }

    public void metaData(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());
        int durationTotal = Integer.parseInt(listSongs.get(position).getDuration()) / 1000;
        duration_total.setText(formatedTime(durationTotal));
        byte[] art = retriever.getEmbeddedPicture();
        Bitmap bitmap;
        if (art != null) {
            Glide.with(this).asBitmap().load(art).into(cover_art);
            bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
            imageAnimation(this, cover_art, bitmap);
            Palette.from(bitmap).generate(palette -> {
                assert palette != null;
                Palette.Swatch swatch = palette.getDominantSwatch();
                RelativeLayout gradient = findViewById(R.id.card);
                RelativeLayout mContainer = findViewById(R.id.mContainer);
                gradient.setBackgroundResource(R.drawable.gradient_bg);
                mContainer.setBackgroundResource(R.drawable.main_bg);
                GradientDrawable gradientDrawable;
                if (swatch != null) {
                    gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{swatch.getRgb(), 0x00000000});
                    gradient.setBackground(gradientDrawable);
                    GradientDrawable gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{swatch.getRgb(), 0x00000000});
                } else {
                    gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{0xff000000, 0x00000000});
                    gradient.setBackground(gradientDrawable);
                    GradientDrawable gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{0xff000000, 0xff000000});
                }
            });
        } else {
            Glide.with(this).asBitmap().load(R.drawable.splashscreen).into(
                    cover_art);
            ImageView gradient = findViewById(R.id.imageViewGradient);
            RelativeLayout mContainer = findViewById(R.id.mContainer);
            gradient.setBackgroundResource(R.drawable.gradient_bg);
            mContainer.setBackgroundResource(R.drawable.main_bg);
            song_name.setTextColor(Color.WHITE);
            artist_name.setTextColor(Color.DKGRAY);
        }
    }

    @Override
    protected void onResume() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, BIND_AUTO_CREATE);
        playThreadBtn();
        prvThreadBtn();
        nextThreadBtn();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void playThreadBtn() {
        playThread = new Thread() {
            @Override
            public void run() {
                super.run();
                play.setOnClickListener(v -> playBtnClicked());
            }
        };
        playThread.start();
    }

    public void playBtnClicked() {
        if (musicService.isPlaying()) {
            play.setImageResource(R.drawable.play);
            musicService.showNotification(R.drawable.notification_play);
            musicService.pause();
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);

                    }
                    handler.postDelayed(this, 1000);
                }
            });
        } else {
            play.setImageResource(R.drawable.pause);
            musicService.showNotification(R.drawable.notification_pause);
            musicService.start();
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);

                    }
                    handler.postDelayed(this, 1000);
                }
            });
        }
    }

    private void nextThreadBtn() {
        nextThread = new Thread() {
            @Override
            public void run() {
                super.run();
                nextBtn.setOnClickListener(v -> nextBtnClicked());
            }
        };
        nextThread.start();
    }

    public void nextBtnClicked() {
        if (musicService.isPlaying()) {
            musicService.stop();
            musicService.release();
            if (shuffleButton && !repeatButton) {
                position = getRandom(listSongs.size() - 1);
            } else if (!shuffleButton && !repeatButton) {
                position = ((position) % listSongs.size() + 1);
            }
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            if (artist_name.getText().toString().equals("<unknown>")) {
                artist_name.setText("Artist N/A");
            } else {
                artist_name.setText(listSongs.get(position).getArtist());
            }
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);

                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotification(R.drawable.notification_pause);
            play.setImageResource(R.drawable.pause);
            musicService.start();
        } else {
            musicService.stop();
            musicService.release();
            if (shuffleButton && !repeatButton) {
                position = getRandom(listSongs.size() - 1);
            } else if (!shuffleButton && !repeatButton) {
                position = ((position + 1) % listSongs.size());
            }
            position = ((position) % listSongs.size() + 1);
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            if (artist_name.getText().toString().equals("<unknown>")) {
                artist_name.setText("Artist N/A");
            } else {
                artist_name.setText(listSongs.get(position).getArtist());
            }
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);

                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotification(R.drawable.notification_play);
            play.setImageResource(R.drawable.play);
        }
    }

    public static int getRandom(int i) {
        Random random = new Random();
        return random.nextInt(i + 1);
    }

    private void prvThreadBtn() {
        prevThread = new Thread() {
            @Override
            public void run() {
                super.run();
                prevBtn.setOnClickListener(v -> prevBtnClicked());
            }
        };
        prevThread.start();
    }

    public void prevBtnClicked() {
        if (musicService.isPlaying()) {
            musicService.stop();
            musicService.release();
            if (shuffleButton && !repeatButton) {
                position = getRandom(listSongs.size() - 1);
            } else if (!shuffleButton && !repeatButton) {
                position = ((position - 1) < 0 ? (listSongs.size() - 1) : (position - 1));
            }
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            if (artist_name.getText().toString().equals("<unknown>")) {
                artist_name.setText("Artist N/A");
            } else {
                artist_name.setText(listSongs.get(position).getArtist());
            }
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);

                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotification(R.drawable.notification_pause);
            play.setImageResource(R.drawable.pause);
            musicService.start();
        } else {
            musicService.stop();
            musicService.release();
            if (shuffleButton && !repeatButton) {
                position = getRandom(listSongs.size() - 1);
            } else if (!shuffleButton && !repeatButton) {
                position = ((position - 1) < 0 ? (listSongs.size() - 1) : (position - 1));
            }
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            if (artist_name.getText().toString().equals("<unknown>")) {
                artist_name.setText("Artist N/A");
            } else {
                artist_name.setText(listSongs.get(position).getArtist());
            }
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);

                    }
                    handler.postDelayed(this, 1000);
                }
            });

            musicService.onCompleted();
            musicService.showNotification(R.drawable.notification_play);
            play.setImageResource(R.drawable.play);
        }
    }

    public void imageAnimation(Context context, ImageView imageView, Bitmap bitmap) {
        Animation animOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        Animation animIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                Glide.with(context).load(bitmap).into(imageView);
                animIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageView.startAnimation(animIn);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        imageView.startAnimation(animOut);

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder myBinder = (MusicService.MyBinder) service;
        musicService = myBinder.getService();
        musicService.setCallBack(this);
        seekBar.setMax(musicService.getDuration() / 1000);
        metaData(uri);
        song_name.setText(listSongs.get(position).getTitle());
        artist_name.setText(listSongs.get(position).getArtist());
        if (artist_name.getText().toString().equals("<unknown>")) {
            artist_name.setText("Artist N/A");
        } else {
            artist_name.setText(listSongs.get(position).getArtist());
        }
        sharePath = listSongs.get(position).getPath();
        musicService.onCompleted();
        musicService.showNotification(R.drawable.notification_pause);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeTop() {

    }

    @Override
    public void swipeBottom() {

    }

    @Override
    public void swipeLeft() {

    }

    @SuppressLint("SetTextI18n")
    public class OnSwipeTouchListener implements View.OnTouchListener {
        private final GestureDetector gestureDetector;
        Context context;
        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        OnSwipeTouchListener(Context ctx, View mainView) {
            gestureDetector = new GestureDetector(ctx, new GestureListener());
            mainView.setOnTouchListener(this);
            context = ctx;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        public class GestureListener extends
                GestureDetector.SimpleOnGestureListener {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean result = false;
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                onSwipeRight();
                            } else {
                                onSwipeLeft();
                            }
                            result = true;
                        }
                    } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            onSwipeBottom();
                        } else {
                            onSwipeTop();
                        }
                        result = true;
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }
        }

        void onSwipeRight() {
            prevBtnClicked();
            this.onSwipe.swipeRight();
        }

        void onSwipeLeft() {
            nextBtnClicked();
            this.onSwipe.swipeLeft();
        }

        void onSwipeTop() {
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
            this.onSwipe.swipeTop();
        }

        void onSwipeBottom() {
            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
            this.onSwipe.swipeBottom();
        }

        onSwipeListener onSwipe;
    }

    public void onCompleteNextBtnClicked() {
        if (musicService.isPlaying()) {
            musicService.stop();
            musicService.release();
            if (shuffleButton && !repeatButton) {
                position = getRandom(listSongs.size());
            } else if (!shuffleButton && !repeatButton) {
                position = ((position) % listSongs.size());
            }
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            if (artist_name.getText().toString().equals("<unknown>")) {
                artist_name.setText("Artist N/A");
            } else {
                artist_name.setText(listSongs.get(position).getArtist());
            }
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);

                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotification(R.drawable.notification_pause);
            play.setImageResource(R.drawable.pause);
            musicService.start();
        } else {
            musicService.stop();
            musicService.release();
            if (shuffleButton && !repeatButton) {
                position = getRandom(listSongs.size() - 1);
            } else if (!shuffleButton && !repeatButton) {
                position = ((position) % listSongs.size() + 1);
            }
            position = ((position) % listSongs.size());
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            if (artist_name.getText().toString().equals("<unknown>")) {
                artist_name.setText("Artist N/A");
            } else {
                artist_name.setText(listSongs.get(position).getArtist());
            }
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);

                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotification(R.drawable.notification_play);
            play.setImageResource(R.drawable.play);
        }
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


}