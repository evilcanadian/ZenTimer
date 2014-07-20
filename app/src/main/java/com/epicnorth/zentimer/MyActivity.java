package com.epicnorth.zentimer;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.view.View;
import com.epicnorth.zentimer.MusicService.MusicBinder;


public class MyActivity extends Activity implements TextToSpeech.OnInitListener {

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;
    TextView countDown;
    Button btnBeginPractice;

    private MySegmentTimer segmentTimer;
    private SoundPool soundPool;
    private int soundID;
    private TextToSpeech tts;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;

    boolean loaded = false;
    int currentSegment = 0;
    Practice practice;

    private ArrayList<Song> songList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        btnBeginPractice = (Button) findViewById(R.id.btnBeginPractice);
        expListView = (ExpandableListView)findViewById(R.id.lvExp);
        countDown = (TextView)findViewById(R.id.countDown);

        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
           @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status){
               loaded = true;
           }
        });

        soundID = soundPool.load(this, R.raw.zenbell1, 1);
        songList = new ArrayList<Song>();


        getSongList();
        prepareListData();




        tts = new TextToSpeech(this, this);
        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);

        expListView.setAdapter(listAdapter);
    }

    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder) service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onStart(){
        super.onStart();
        if (playIntent == null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    public void getSongList(){
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()){
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);

            do{
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);

                songList.add(new Song(thisId, thisTitle, thisArtist));
            } while(musicCursor.moveToNext());

        }
    }

    public void BeginPractice(View v) throws InterruptedException {

        musicSrv.setSong(0);
        musicSrv.playSong();

        practice = new Practice();

        Segment seg1 = new Segment();
        seg1.Duration = 10000;
        seg1.Name = "Contrary Breathing";
        seg1.Ordinal = 1;

        practice.Segments.add(seg1);

        Segment seg2 = new Segment();
        seg2.Duration = 10000;
        seg2.Name = "Big Heaven";
        seg2.Ordinal = 2;

        practice.Segments.add(seg2);

        segmentTimer = new MySegmentTimer(seg1);
        segmentTimer.start();
        Toast.makeText(this.getBaseContext(),"Practice Button Clicked", Toast.LENGTH_SHORT).show();
    }

    private void executeNextSegment(){

        if (tts != null){
            String text = "Past five minutes";
            if (!tts.isSpeaking()){
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }

        AudioManager audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        float actualVolume = (float)audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = (float)audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float volume = actualVolume / maxVolume;

        currentSegment++;
        if (loaded){
            soundPool.play(soundID, volume, volume, 1, 0, 1f);
        }
        if (currentSegment < practice.Segments.size()){
            segmentTimer = new MySegmentTimer(practice.Segments.get(currentSegment));
            segmentTimer.start();
        } else{

            Toast.makeText(this.getBaseContext(), "Finished all segments", Toast.LENGTH_SHORT).show();
        }
    }

    public class MySegmentTimer extends CountDownTimer{
        private Segment segment;

        public MySegmentTimer(Segment segment){
            super(segment.Duration, 1000);
            this.segment = segment;
        }

        @Override
        public void onTick(long millisUntilFinished){
            countDown.setText("Seconds remaining: " + millisUntilFinished / 1000 + " on " + segment.Name);
        }

        @Override
        public void onFinish(){
            countDown.setText("Finished");
            this.cancel();
            executeNextSegment();
        }

    }
    private void prepareListData(){
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        for (int i = 0; i < songList.size(); i++){
            listDataHeader.add(songList.get(i).getTitle());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onInit(int code){
        if (code == TextToSpeech.SUCCESS){
            tts.setLanguage(Locale.getDefault());
        }else{
            tts = null;
            Toast.makeText(this, "Failed to initialize text to speech engine.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy(){
        if (tts!= null){
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
}
