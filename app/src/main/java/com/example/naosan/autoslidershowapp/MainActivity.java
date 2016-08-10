package com.example.naosan.autoslidershowapp;
import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PERMISSIONS_REQUEST_CODE = 100;

    // 画像の情報を取得する
    /*
    1. onCreateのタイミング
    　最初の1回だけ呼ばれるgetContentsInfoAtTheBeginningメソッドで、ContentProviderを使って端末内の画像の情報を取得して
    */
    ContentResolver resolver = null;
    Cursor cursor = null;
    boolean isPlay = true;

    private Timer timer;					//タイマー用
    private SlideTimerTask slideTimerTask = null;		//タイマタスククラス
    private Handler mHandler = new Handler();   //UI Threadへのpost用ハンドラ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                getContentsInfoAtTheBeginning();
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            }
            // Android 5系以下の場合
        } else {
            getContentsInfoAtTheBeginning();
        }

        Button buttonFW = (Button) findViewById(R.id.buttonFW);
        buttonFW.setOnClickListener(this);
        Button buttonBK = (Button) findViewById(R.id.buttonBK);
        buttonBK.setOnClickListener(this);
        Button buttonPLYSTP = (Button) findViewById(R.id.buttonPLYSTP);
        buttonPLYSTP.setOnClickListener(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
        Log.d("Android", "onDestroy");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContentsInfoAtTheBeginning();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {


        if (v.getId() == R.id.buttonFW) {
            getContentsInfoFW();
        } else if (v.getId() == R.id.buttonBK) {
            getContentsInfoBK();
        } else {
            Button b = (Button)v;
            if (isPlay) { /*再生ボタンを押下*/
                isPlay = false; /*停止を表示する*/
                b.setText("停止");
                // Timer インスタンスを生成
                timer = new Timer();
                // TimerTask インスタンスを生成
                slideTimerTask = new SlideTimerTask();
                // スケジュールを設定 2秒（2000msec)
                timer.schedule(slideTimerTask, 0, 2000);
            } else { /*停止ボタンを押下*/
                isPlay = true; /*停止を表示する*/
                b.setText("再生");
                timer.cancel();
                timer = null;
            }
        };
    }


    /*
       onCreateのときに1回だけよばれるメソッド
       本メソッドで、画像情報を取得する cursor をセットする。以降、「進む」「戻る」「再生」のボタンをおされると、この cursor を通じて対象となる画像を取得する。
       なお、cursor は onDestroyのときに close する。
     */
    private void getContentsInfoAtTheBeginning() {

        // 画像の情報を取得する
        resolver = getContentResolver();
        cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
                null, // 項目(null = 全項目)
                null, // フィルタ条件(null = フィルタなし)
                null, // フィルタ用パラメータ
                null // ソート (null ソートなし)
        );

        if (cursor.moveToFirst()) {
                // indexからIDを取得し、そのIDから画像のURIを取得する
                int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                Long id = cursor.getLong(fieldIndex);
                Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                ImageView imageVIew = (ImageView) findViewById(R.id.imageView);
                imageVIew.setImageURI(imageUri);
        }
    }


    /* 「進む」ボタンで呼ばれるメソッド */
    private void getContentsInfoFW() {

        if (cursor.moveToNext() || cursor.moveToFirst() ) { /*最後にきたら一番先頭にカーソルを移動*/
            // indexからIDを取得し、そのIDから画像のURIを取得する
            int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
            Long id = cursor.getLong(fieldIndex);
            Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

            ImageView imageVIew = (ImageView) findViewById(R.id.imageView);
            imageVIew.setImageURI(imageUri);

        }
    }

    /* 「戻る」ボタンで呼ばれるメソッド */
    private void getContentsInfoBK() {

        if (cursor.moveToPrevious() || cursor.moveToLast() ) { /*先頭にきたら一番最後にカーソルを移動*/
            // indexからIDを取得し、そのIDから画像のURIを取得する
            int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
            Long id = cursor.getLong(fieldIndex);
            Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

            ImageView imageVIew = (ImageView) findViewById(R.id.imageView);
            imageVIew.setImageURI(imageUri);

        }
    }

    /* 「再生」ボタンでよばれるタイマーのクラス
    以下、コメントは参照したサンプルコードに記載されていたコメント　（よく理解できていない）
    ここで、TimerTaskの別スレッドができますが、描画処理はmainスレッドでしかできませんのでHandlerを使ってpostで処理待ちにします。
    */
    class SlideTimerTask extends TimerTask {
        @Override
        public void run() {
            // handlerを使って処理をキューイングする
            mHandler.post(new Runnable() {
                public void run() {
                    getContentsInfoFW(); /*「進む」ボタンのメソッド */
                }
            });
        }
    }

}