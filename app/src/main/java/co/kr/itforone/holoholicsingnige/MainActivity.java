package co.kr.itforone.holoholicsingnige;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import co.kr.itforone.holoholicsingnige.databinding.ActivityMainBinding;
import common.Common;
import common.LocationPosition;
import common.PermissionCheck;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity implements ComponentCallbacks2{
    public String firstUrl = "";
    public final String DEVICE_ID_TXT="Please connect the device ID after registration.\n Devicd ID : ";
    ActivityMainBinding binding;
    public String deviceId="";
    public int tabLayoutVisible= View.GONE;
    Socket socket;
    public ConnectivityManager connectivityManager;
    AudioManager audioManager;
    PermissionCheck permissionCheck;
    PowerManager.WakeLock wakeLock;
    private AlarmManager alarmManager;
    static public WebView MAIN_WEB_VIEW;
    // Other activity code ...

    /**
     * Release memory when the UI becomes hidden or when system resources become low.
     * @param level the memory-related event that was raised.
     */
    @Override
    public void onTrimMemory(int level) {
        Log.d("memory-level",level+"");

        // Determine which lifecycle or system event was raised.
        switch (level) {

            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:

                    /*
                       Release any UI objects that currently hold memory.
                       The user interface has moved to the background.
                    */

                break;

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:

                    /*
                       Release any memory that your app doesn't need to run.

                       The device is running low on memory while the app is running.
                       The event raised indicates the severity of the memory-related event.
                       If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                       begin killing background processes.
                    */
                finishAffinity();
                binding.webView.clearCache(true);
                MainActivity.this.onLowMemory();
                break;

            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:

                    /*
                       Release as much memory as the process can.

                       The app is on the LRU list and the system is running low on memory.
                       The event raised indicates where the app sits within the LRU list.
                       If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                       the first to be terminated.
                    */

                break;

            default:
                binding.webView.clearCache(true);
                    /*
                      Release any non-critical data structures.

                      The app received an unrecognized memory level value
                      from the system. Treat this as a generic low-memory message.
                    */
                break;
        }
        super.onTrimMemory(level);
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        deviceId=Common.getPref(this,"deviceId","");
        alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        if(deviceId.equals("")){
            deviceId=Common.getMyDeviceId(this);
            Common.savePref(this,"deviceId",deviceId);
        }
        //풀 스크린으로 변경하기
        //하단 메뉴 감추기
        if (Build.VERSION.SDK_INT >= 19)
        {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
        else
        {
            // getWindow().getDecorView().setSystemUiVisibility(View.GONE);
            getWindow().getDecorView().setSystemUiVisibility
                    ( View.SYSTEM_UI_FLAG_LOW_PROFILE |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //화면계속유지시키기
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire();

        binding= DataBindingUtil.setContentView(this,R.layout.activity_main);
        //권한설정하기
        permissionCheck = new PermissionCheck(MainActivity.this);
        permissionCheck.setPermission();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        //볼륨조절하기위한 객체
        audioManager=(AudioManager)getSystemService(AUDIO_SERVICE);
        firstUrl=getResources().getString(R.string.url);
        binding.setMainData(this);
        MAIN_WEB_VIEW = binding.webView;
        webViewSetting();
        Intent intent = getIntent();
        //시계일 때 초기화 하기
        try {
            String s_id=intent.getStringExtra("s_id");
            if(!s_id.equals("")&&!s_id.equals(null)) {
                String urls[] = getResources().getStringArray(R.array.urlArr);
                binding.signRegisterLayout.setVisibility(View.GONE);
                binding.webView.loadUrl(urls[5] + s_id);
                binding.webView.setVisibility(View.VISIBLE);
            }
        }catch (Exception e){

        }


        try{
            socket = IO.socket("http://14.48.175.184:8010");
            socket.on(Socket.EVENT_CONNECT, onConnect)
                    .on("message", new Emitter.Listener() {
                        //볼륨을 원격으로 조절하기
                        @Override
                        public void call(Object... args) {
                            JSONObject obj = (JSONObject)args[0];
                            String data="";
                            String mb_id="";
                            Log.d("TAG",obj.toString());
                            try {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        binding.signRegisterLayout.setVisibility(View.GONE);
                                    }
                                });

                                data= (String) obj.get("data");
                                String finalData = data;
                                String finalS_id = (String) obj.get("s_id");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                    }
                                });

                                if(data.equals("down")){
                                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);
                                    //볼륨 높이기
                                }else if(data.equals("up")){
                                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);
                                }else if(data.equals("connect")){
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            binding.signRegisterLayout.setVisibility(View.GONE);
                                            binding.webView.setVisibility(View.VISIBLE);
                                            Common.savePref(MainActivity.this, "connect", "1");
                                        }
                                    });
                                }else if(data.equals("dis_connect")){
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            binding.signRegisterLayout.setVisibility(View.VISIBLE);
                                            binding.webView.setVisibility(View.GONE);
                                            binding.webView.loadUrl("");
                                            Log.d("disconnect","disconnect");
                                            Common.savePref(MainActivity.this, "connect", "");
                                        }
                                    });

                                }else if(data.equals("scheduler")){

                                    String schedulerTime = (String) obj.get("schedulerTime");
                                    Log.d("scheduler",schedulerTime);
                                    if(!schedulerTime.equals("")){
                                        setAlram(schedulerTime);
                                    }else{

                                    }

                                }else{
                                    runOnUiThread(new Runnable() {
                                                      @Override
                                                      public void run() {
                                                          binding.signRegisterLayout.setVisibility(View.GONE);
                                                          binding.webView.setVisibility(View.VISIBLE);
                                                      }
                                                  });
                                    //각각의 url나오게
                                    int no=Integer.parseInt(data);
                                    String urls [] = getResources().getStringArray(R.array.urlArr);
                                    switch (no){
                                        case 2:

                                            binding.webView.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    binding.webView.clearCache(true);
                                                    try {
                                                        binding.webView.reload();
                                                        binding.webView.setVisibility(View.VISIBLE);
                                                        binding.webView.loadUrl(urls[no]+(String) obj.get("youtubeId"));

                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });
                                            break;
                                        case 3:
                                            binding.webView.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        binding.webView.setVisibility(View.VISIBLE);
                                                        binding.webView.loadUrl(urls[no]+(String)obj.get("instagramUrl"));
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });
                                            break;
                                        case 4:
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    binding.webView.setVisibility(View.VISIBLE);
                                                    try {
                                                        String viewType=(String)obj.get("viewType");
                                                        if(viewType.equals(null)||viewType.equals("")){
                                                            binding.webView.loadUrl(urls[no]+finalS_id+"&mb_id="+(String)obj.get("mb_id"));
                                                        }else {
                                                            Log.d("TAG", urls[no] + "&viewType=" + (String) obj.get("viewType"));
                                                            binding.webView.loadUrl(urls[no] +finalS_id + "&viewType=" + (String) obj.get("viewType")+"&date="+(String) obj.get("date")+"&mb_id="+(String)obj.get("mb_id"));
                                                        }
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });
                                            break;
                                        case 6:
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if(LocationPosition.lat!=0.0){
                                                        binding.webView.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                binding.webView.setVisibility(View.VISIBLE);

                                                                Log.d("TAG","lat="+LocationPosition.lat+"&lng="+LocationPosition.lng);
                                                                binding.webView.loadUrl(urls[no]+finalS_id+"&lat="+LocationPosition.lat+"&lng="+LocationPosition.lng);
                                                            }
                                                        });
                                                    }else{
                                                        LocationFind();
                                                        //Toast.makeText(MainActivity.this, "Location Searching...", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                            break;
                                        default:
                                            binding.webView.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    binding.webView.setVisibility(View.VISIBLE);
                                                    try {
                                                        binding.webView.loadUrl(urls[no]+finalS_id+"&mb_id="+(String)obj.get("mb_id"));
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });

                                            break;
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .on("wakeUp", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                }
                            });

                        }
                    })
                    .on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                }
                            });
                        }
                    })
                    .on(Socket.EVENT_RECONNECT, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    socket.connect();
                                    socketLogin();
                                }
                            });
                        }
                    });

            socket.connect();
            socketLogin();
        }catch (Exception e){
            //Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        /*socket.disconnect();
        socket.close();*/
        wakeLock.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*socket.connect();
        socketLogin();*/
        wakeLock.acquire();
    }

    //디바이스 연결이 되거나 또는 연결이 되어있으면 레이아웃 변경이 되게
     @RequiresApi(api = Build.VERSION_CODES.M)
     private void setViewVisible(){
         //고객과 연결이 되었으면 연결시키기
         if(Common.getPref(this,"connect","").equals("1")){
             binding.signRegisterLayout.setVisibility(View.GONE);
             binding.webView.setVisibility(View.VISIBLE);
         }
         //디바이스 아이디가 없는 경우에는 설명 레이아웃이 나오게
         if(deviceId.equals("")||deviceId.equals(null)) {
             tabLayoutVisible = View.VISIBLE;
             deviceId = Common.getMyDeviceId(this);
             Common.savePref(this, "deviceId", deviceId);
         }else{
             //binding.signRegisterLayout.setVisibility(View.GONE);
             //binding.webView.setVisibility(View.VISIBLE);
         }
     }
    //소켓에 디바이스 아이디로 로그인 시키기
    private void socketLogin(){
        JSONObject obj = new JSONObject();
        try {
            obj.put("id",deviceId);
            socket.emit("login", obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
     Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // your code...

                }
            });


        }
    };

    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.ECLAIR_MR1)
    public void webViewSetting() {
        //Common.setTOKEN(this);
        WebSettings setting = binding.webView.getSettings();//웹뷰 세팅용
        if(Build.VERSION.SDK_INT >= 21) {
            binding.webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        setting.setAllowFileAccess(true);//웹에서 파일 접근 여부
        setting.setAppCacheEnabled(false);//캐쉬 사용여부
        setting.setGeolocationEnabled(true);//위치 정보 사용여부
        setting.setDatabaseEnabled(true);//HTML5에서 db 사용여부
        setting.setDomStorageEnabled(true);//HTML5에서 DOM 사용여부
        setting.setCacheMode(WebSettings.LOAD_NO_CACHE);//캐시 사용모드 LOAD_NO_CACHE는 캐시를 사용않는다는 뜻
        setting.setJavaScriptEnabled(true);//자바스크립트 사용여부
        setting.setSupportMultipleWindows(true);//윈도우 창 여러개를 사용할 것인지의 여부 무조건 false로 하는 게 좋음
        setting.setUseWideViewPort(true);//웹에서 view port 사용여부
        setting.setAllowFileAccessFromFileURLs(true);
        setting.setAllowUniversalAccessFromFileURLs(true);
        // 웹뷰에 동영상을 바로 실행시키기 위함.
        setting.setMediaPlaybackRequiresUserGesture(false);
        setting.setTextZoom(100);
        setting.setSupportZoom(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND,true);
        }
        if (Build.VERSION_CODES.HONEYCOMB <= Build.VERSION.SDK_INT) {
            binding.webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        }
        binding.webView.setWebChromeClient(chrome);//웹에서 경고창이나 또는 컴펌창을 띄우기 위한 메서드
        binding.webView.setWebViewClient(client);//웹페이지 관련된 메서드 페이지 이동할 때 또는 페이지가 로딩이 끝날 때 주로 쓰임
        setting.setUserAgentString("Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Mobile Safari/537.36"+"/Holoholik");
        binding.webView.addJavascriptInterface(new WebJavascriptEvent(), "Android");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.webView.loadUrl("");
        }
    }

    WebChromeClient chrome;
    {
        chrome = new WebChromeClient() {
            @Override
            public void onHideCustomView() {
                super.onHideCustomView();
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                super.onShowCustomView(view, callback);
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onShowCustomView(View view, int requestedOrientation, WebChromeClient.CustomViewCallback callback) {
                this.onShowCustomView(view, callback);
            }

            //새창 띄우기 여부
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                return false;
            }

            //경고창 띄우기
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("\n" + message + "\n")
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        result.confirm();
                                    }
                                }).create().show();
                return true;
            }

            //컴펌 띄우기
            @Override
            public boolean onJsConfirm(WebView view, String url, String message,
                                       final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("\n" + message + "\n")
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        result.confirm();
                                    }
                                })
                        .setNegativeButton("CANCEL",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        result.cancel();
                                    }
                                }).create().show();
                return true;
            }

            //현재 위치 정보 사용여부 묻기
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // Should implement this function.
                final String myOrigin = origin;
                final GeolocationPermissions.Callback myCallback = callback;
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Request message");
                builder.setMessage("Allow current location?");
                builder.setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        myCallback.invoke(myOrigin, true, false);
                    }
                });
                builder.setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        myCallback.invoke(myOrigin, false, false);
                    }

                });
                AlertDialog alert = builder.create();
                alert.show();
            }
            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                openFileChooser(uploadMsg, "");
            }

            // For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {

            }

            // For Android 4.1+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                openFileChooser(uploadMsg, acceptType);
            }


            // For Android 5.0+\
            // SDK 21 이상부터 웹뷰에서 파일 첨부를 해주는 기능입니다.
            @RequiresApi(api = Build.VERSION_CODES.M)
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                    return false;

            }

        };
    }

    WebViewClient client;
    {
        client = new WebViewClient() {
            //페이지 로딩중일 때 (마시멜로) 6.0 이후에는 쓰지 않음
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                Log.d("url",url);

                return false;
            }
            //페이지 로딩이 다 끝났을 때
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

            }
            //페이지 오류가 났을 때 6.0 이후에는 쓰이지 않음
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

            }
        };
    }


    //로그인 로그아웃
    class WebJavascriptEvent{


        @JavascriptInterface
        public void ActivityFinish(){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }
        @RequiresApi(api = Build.VERSION_CODES.M)
        @JavascriptInterface
        public void AppReset(){
            finishAffinity();
            Intent intent = new Intent(MainActivity.this,MainActivity.class);
            intent.putExtra("s_id",Common.getMyDeviceId(MainActivity.this));
            startActivity(intent);
            System.exit(0);
        }

    }
    //위치 잡기
    private void LocationFind(){
        LocationPosition.act=MainActivity.this;
        LocationPosition.setPosition(MainActivity.this);
        if(LocationPosition.lng==0.0){
            LocationPosition.setPosition(MainActivity.this);
        }
    }
    //
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
    //광고스케줄러 만들기
    private void setAlram(String adDate){
        Intent recieverIntent = new Intent(MainActivity.this,AlaramRecevier.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this,0,recieverIntent,0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date datetime = null;
        try {
            datetime = dateFormat.parse(adDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(datetime);

            alarmManager.set(AlarmManager.RTC,calendar.getTimeInMillis(),pendingIntent);
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}