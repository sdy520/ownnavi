package com.example.ownnavi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    ArrayList<Pos> posArrayList=new ArrayList<>();
    private static final String TAG = "MainActivity";
    private static final double EARTH_RADIUS = 6378.137;
    public MediaPlayer questionPlayer;
    public MediaPlayer mMediaPlayer;
    public MediaPlayer noposplayer;
    private Toast mToast;
    private TextView textView;
    // 语音唤醒对象
    private VoiceWakeuper mIvw;
    // 语音识别对象
    private SpeechRecognizer mAsr;
    // 唤醒结果内容
    private String resultString;
    // 本地语法id
    private String mLocalGrammarID=null;
    private int curThresh = 1450;
    // 本地语法文件
    private String mLocalGrammar = null;
    // 本地语法构建路径
    private String grmPath = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/msc/test";
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_LOCAL;

    //初始化音频管理器
    private AudioManager mAudioManager;
    private TextView lati;
    private TextView lngi;
    private TextView mindis;
    private TextView jindian;
    private ArrayList<Pos> posList=new ArrayList<>();
    ArrayList<Double> temps=new ArrayList<>();
    //ArrayList<Thread> threads=new ArrayList<>();
    private Pos[] poses={
            //new Pos("景点1",121.767175,39.046156,20),
            new Pos("景点1",121.767582,39.046090,20),
            new Pos("景点2",121.767543,39.042967,20),
            new Pos("景点3",121.770214,39.042969,20)
    };
    public ArrayList<Pos> Getpos(){
        for (Pos pos:poses){
            posArrayList.add(pos);
        }
        return posArrayList;
    };
    double car_longitude;
    double car_latitude;
    public String name;
    public String lastname;
    public String filename;
    public int bg_number=0;
    public int flag=1;
    //存储问题序号
    public int ques_number;
    ExecutorService exec;
    //保持问题入队列
    Queue<Integer> queue=new LinkedList<Integer>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        exec = Executors.newSingleThreadExecutor();
        lati=(TextView) findViewById(R.id.lat);
        lngi=(TextView) findViewById(R.id.lng);
        mindis=(TextView)findViewById(R.id.dist);
        jindian=(TextView)findViewById(R.id.jindian);
        textView = (TextView) findViewById(R.id.txt_show_msg);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
 /*       if (Build.VERSION.SDK_INT >= 23) {// android6 执行运行时权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }else {
                Log.e(TAG, "权限已申请");
                startLocation();
            }
        } else {
            startLocation();
        }
*/
        requestPermissions();

        //播放问题语音
        questionPlayer = new MediaPlayer();
        mMediaPlayer=new MediaPlayer();
        noposplayer=new MediaPlayer();
        //初始化音频管理器
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        startLocation();
        Getpos();

        StringBuffer param = new StringBuffer();
        param.append("appid="+getString(R.string.app_id));
        param.append(",");
        // 设置使用v5+
        param.append(SpeechConstant.ENGINE_MODE+"="+SpeechConstant.MODE_MSC);
        SpeechUtility.createUtility(this, param.toString());

        // 初始化唤醒对象
        mIvw = VoiceWakeuper.createWakeuper(this, null);
        // 初始化识别对象---唤醒+识别,用来构建语法
        //mAsr = SpeechRecognizer.createRecognizer(this, mInitListener);
        mAsr = SpeechRecognizer.createRecognizer(this, mInitListener);
        if(mAsr==null){
            Log.e(TAG,"masr is null");
        }
        // 初始化语法文件
        mLocalGrammar = readFile(this, "dynasty1.bnf", "utf-8");

        initgrammar();

        initdata();
    }

    private void startLocation() {
        //注意6.0及以上版本需要在申请完权限后调用方法
        LocationUtils.getInstance(this).setAddressCallback(new LocationUtils.AddressCallback() {
            /* @Override
            public void onGetAddress(Address address) {
               String countryName = address.getCountryName();//国家
                String adminArea = address.getAdminArea();//省
                String locality = address.getLocality();//市
                String subLocality = address.getSubLocality();//区
                String featureName = address.getFeatureName();//街道
                Log.e("定位地址", countryName + " " + adminArea + " " + locality + " " + subLocality + " " + featureName);
            }*/

            @Override
            public void onGetLocation(double lat, double lng) {
                car_latitude=lat;
                car_longitude=lng;

                function();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                lati.setText(String.valueOf(lat));
                                lngi.setText(String.valueOf(lng));
                                mindis.setText(minNum_str);
                                jindian.setText(jindian1);

                            }
                        });
                    }
                }).start();
                posList.clear();
                temps.clear();
                Log.e("定位经纬度", lat + " " + lng);

            }
        });
    }
    public static double getDistance(double longitude1, double latitude1, double longitude2, double latitude2) {
        // 纬度
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        // 经度
        double lng1 = Math.toRadians(longitude1);
        double lng2 = Math.toRadians(longitude2);
        // 纬度之差
        double a = lat1 - lat2;
        // 经度之差
        double b = lng1 - lng2;
        // 计算两点距离的公式
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(b / 2), 2)));
        // 弧长乘地球半径, 返回单位: 千米
        s =  s * EARTH_RADIUS;
        return s;
    }
    String minNum_str;
    String jindian1;

    public void function(){
        posList=GetposArrayList(car_latitude);
        for(int i=0;i<posList.size();i++){
            double distance=getDistance(car_longitude,car_latitude,posList.get(i).getLongitude(),posList.get(i).getLatitude());
            temps.add(i,distance);
            double minNum = Collections.min(temps);
            //int place=temps.indexOf(minNum);
            minNum_str=String.format("%.3f",minNum);
            Log.e("distance_min",minNum_str+"km");
            if(distance*1000<=posList.get(i).getRadius()){
                if (!mMediaPlayer.isPlaying()&&(!questionPlayer.isPlaying())) {
                    noposplayer.pause();
                    //将暂停音乐名称存在lastname
                    lastname=name;
                    flag=0;
                    filename = "/storage/emulated/0/";
                    //filename=Environment.getExternalStorageDirectory().getAbsolutePath();
                    Log.e("distance_min",filename);
                    name = posList.get(i).getName();
                    try {
                        mMediaPlayer.reset();
                        filename = filename +"/"+ name + ".mp3";
                        Log.e("filename", filename);
                        mMediaPlayer.setDataSource(filename);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                        mMediaPlayer.setVolume(1f, 1f);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                jindian1="景点内";
            }
            else{
                if((!mMediaPlayer.isPlaying())&&(!noposplayer.isPlaying())&&(!questionPlayer.isPlaying())&&flag==0) {
                    noposplayer.start();
                    name=lastname;
                    flag=1;
                    Log.e("1111111111", "111111111111111111");
                }
                if((!mMediaPlayer.isPlaying())&&(!noposplayer.isPlaying())&&(!questionPlayer.isPlaying())&&mdflag==0&&nopflag==0)
                {

                    String bgfilename = "/storage/emulated/0/bg/";
                    //String bgfilename=Environment.getExternalStorageDirectory().getAbsolutePath()+"/bg/";
                    //Log.e("distance_min",bgfilename);
                    try {
                        noposplayer.reset();
                        name="b"+String.valueOf(bg_number);
                        bgfilename = bgfilename + name + ".mp3";
                        Log.e("bgfilename", bgfilename);
                        queue.offer(bg_number);
                        noposplayer.setDataSource(bgfilename);
                        noposplayer.prepare();
                        noposplayer.start();
                        noposplayer.setVolume(1f, 1f);
                        noposplayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                //threads.add(bg_number,"thread1");
                                //new Thread(new ThreadShow()).start();

                                //ques_number=bg_number;
                                //Log.e("dyn",String.valueOf(ques_number));
                                exec.execute(new ThreadShow());
                                //exec.shutdown();
                            }
                        });
                        if(bg_number<5)
                        { bg_number++;}
                        else
                        {bg_number=1;}

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                jindian1="景点外";
            }

        }
    }

    public ArrayList<Pos> GetposArrayList(Double latitude){
        ArrayList<Pos> posLists=new ArrayList<>();
        for(int i=0;i<posArrayList.size();i++) {
            if (latitude - 0.05 <posArrayList.get(i).getLatitude() && posArrayList.get(i).getLatitude() < latitude + 0.05) {
                posLists.add(posArrayList.get(i));
            }
        }
        return posLists;

    }
    //提问时把noposplayer或者mMediaPlayer暂停
    private  int nopflag=0;
    private  int mdflag=0;

    // handler类接收数据
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                ques_number=queue.poll();
                questionPlayer.reset();
                String filename = "/storage/emulated/0/dynasty";
                filename=filename+ques_number+".mp3";
                Log.e("dy",String.valueOf(ques_number));
                try {
                    if(noposplayer.isPlaying())
                    {
                        noposplayer.pause();
                        nopflag=1;
                    }
                    if(mMediaPlayer.isPlaying())
                    {
                        mMediaPlayer.pause();
                        mdflag=1;
                    }
                    questionPlayer.setDataSource(filename); // 指定音频文件的路径/storage/emulated/0/music.mp3
                    //mediaPlayer.setLooping(true);//设置为循环播放
                    questionPlayer.prepare(); // 让MediaPlayer进入到准备状态
                    questionPlayer.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                questionPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        setgram();
                    }
                });
            }
        };
    };

    // 线程类
    class ThreadShow implements Runnable {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            //while (true) {
            try {
                Thread.sleep(12000);
                Message msg = new Message();
                msg.what = 1;
                handler.sendMessage(msg);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
           // }
        }
    }






    GrammarListener grammarListener = new GrammarListener() {
        @Override
        public void onBuildFinish(String grammarId, SpeechError error) {
            if (error == null) {
                mLocalGrammarID = grammarId;
                showTip("语法构建成功：" + grammarId);
                Log.e("gram","mLocalGrammarID");
            } else {
                showTip("语法构建失败,错误码：" + error.getErrorCode()+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }
    };

    private void initgrammar() {
        int ret = 0;
        mAsr.setParameter(SpeechConstant.PARAMS, null);
        mAsr.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
        // 设置引擎类型
        mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置语法构建路径
        mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
        // 设置本地识别使用语法id
        //mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "dynasty1");
        // 设置识别的门限值
        mAsr.setParameter(SpeechConstant.MIXED_THRESHOLD, "30");
        // 设置资源路径
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mAsr.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mAsr.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/asr.wav");
        mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
        //ret = mAsr.buildGrammar("bnf", mLocalGrammar, grammarListener);

    }
    private void initdata(){
        // 非空判断，防止因空指针使程序崩溃
        mIvw = VoiceWakeuper.getWakeuper();
        if (mIvw != null) {
            resultString = "";
            //recoString = "";
            textView.setText(resultString);

            final String resPath = ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "ivw/"+getString(R.string.app_id)+".jet");
            // 清空参数
            mIvw.setParameter(SpeechConstant.PARAMS, null);
            // 设置识别引擎
            //mIvw.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
            // 设置唤醒资源路径
            mIvw.setParameter(ResourceUtil.IVW_RES_PATH, resPath);
            /**
             * 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
             * 示例demo默认设置第一个唤醒词，建议开发者根据定制资源中唤醒词个数进行设置
             */
            mIvw.setParameter(SpeechConstant.IVW_THRESHOLD, "0:"
                    + curThresh);
            // 设置唤醒+识别模式
            //mIvw.setParameter(SpeechConstant.IVW_SST, "oneshot");
            mIvw.setParameter(SpeechConstant.IVW_SST, "wakeup");
            // 设置返回结果格式
            //mIvw.setParameter(SpeechConstant.RESULT_TYPE, "json");
            // 设置持续进行唤醒
            mIvw.setParameter(SpeechConstant.KEEP_ALIVE,"1");
//			mIvw.setParameter(SpeechConstant.IVW_SHOT_WORD, "0");

            // 设置唤醒录音保存路径，保存最近一分钟的音频
            mIvw.setParameter( SpeechConstant.IVW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath()+"/msc/ivw.wav" );
            mIvw.setParameter( SpeechConstant.AUDIO_FORMAT, "wav" );
            mIvw.startListening(mWakeuperListener);
        } else {
            showTip("唤醒未初始化");
        }

    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码："+code+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }
    };

    /**
     * 识别监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据："+data.length);
        }

        @Override
        public void onResult(final RecognizerResult result, boolean isLast) {
            if (null != result && !TextUtils.isEmpty(result.getResultString())) {
                Log.d(TAG, "recognizer result：" + result.getResultString());
                //recoString = JsonParser.parseGrammarResult(result.getResultString());
                int recoint = JsonParser.parseGrammarResultcontact(result.getResultString());
                Log.d(TAG, " "+recoint);
                if(recoint>30)
                    textView.setText("答对了");
                else {
                    textView.setText("您好，没听到你说的答案，不好意思");
                    Log.e("TAG", "recognizer result : null");
                }
                musicrestart();
            }
        }

        @Override
        public void onEndOfSpeech() {

            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            showTip("结束说话");
            //mediaPlayer.start(); // 开始播放
        }

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
        }

        @Override
        public void onError(SpeechError error) {
            textView.setText("您好，没听到你说的答案，不好意思");
            musicrestart();
            showTip("onError Code："	+ error.getErrorCode());
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null

        }

    };
    /**
     * 识别监听器。
     */
    private RecognizerListener quesRecognizerListener = new RecognizerListener() {

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据："+data.length);
        }

        @Override
        public void onResult(final RecognizerResult result, boolean isLast) {
            if (null != result && !TextUtils.isEmpty(result.getResultString())) {
                Log.d(TAG, "recognizer result：" + result.getResultString());
                //recoString = JsonParser.parseGrammarResult(result.getResultString());
                int contact = JsonParser.parseGrammarResultcontact(result.getResultString());
                int callCmd = JsonParser.parseGrammarResultcallCmd(result.getResultString());
                Log.d(TAG, " "+contact);
                Log.d(TAG, " "+callCmd);
                if(contact>30)
                    //减少音量
                    mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,AudioManager.FX_FOCUS_NAVIGATION_UP);
                if(callCmd>30)
                    //增加电量
                    mAudioManager.adjustStreamVolume (AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE,AudioManager.FX_FOCUS_NAVIGATION_UP);
                if(callCmd<=30&&contact<=30)
                    textView.setText("不好意思,没有听懂你的意思");
                musicrestart();
            }
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入

            showTip("结束说话");
            //mediaPlayer.start(); // 开始播放
        }

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
        }

        @Override
        public void onError(SpeechError error) {
            textView.setText("不好意思,没有听懂你的意思");
            musicrestart();
            showTip("onError Code："	+ error.getErrorCode());
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null

        }

    };
    //说出命令词后，启动之前播放
    private void musicrestart(){
        if((!mMediaPlayer.isPlaying())&&(!noposplayer.isPlaying())&&(!questionPlayer.isPlaying())&&mdflag==1){
            mMediaPlayer.start();
            mdflag=0;
        }
        if((!mMediaPlayer.isPlaying())&&(!noposplayer.isPlaying())&&(!questionPlayer.isPlaying())&&nopflag==1){
            noposplayer.start();
            nopflag=0;
        }
    }
    //设置判断回答正错的识别
    public void setgram(){
        String dynasty="dynasty"+ques_number;
        mLocalGrammar = readFile(MainActivity.this, dynasty+".bnf", "utf-8");
        Log.e("dyna",dynasty);
        mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, dynasty);
        mAsr.buildGrammar("bnf", mLocalGrammar, grammarListener);
        mAsr.startListening(mRecognizerListener);
    }

    //private int i=0;
    private WakeuperListener mWakeuperListener = new WakeuperListener() {

        @Override
        public void onResult(WakeuperResult result) {
            if(noposplayer.isPlaying())
            {
                noposplayer.pause();
                nopflag=1;
            }
            if(mMediaPlayer.isPlaying())
            {
                mMediaPlayer.pause();
                mdflag=1;
            }
            try {
                String text = result.getResultString();
                JSONObject object;
                object = new JSONObject(text);
                StringBuffer buffer = new StringBuffer();
                buffer.append("【RAW】 "+text);
                buffer.append("\n");
                buffer.append("【操作类型】"+ object.optString("sst"));
                buffer.append("\n");
                buffer.append("【唤醒词id】"+ object.optString("id"));
                buffer.append("\n");
                buffer.append("【得分】" + object.optString("score"));
                buffer.append("\n");
                buffer.append("【前端点】" + object.optString("bos"));
                buffer.append("\n");
                buffer.append("【尾端点】" + object.optString("eos"));
                resultString =buffer.toString();
            } catch (JSONException e) {
                resultString = "结果解析出错";
                e.printStackTrace();
            }
            //为了解决多次唤醒后，一次识别出现多次同一唤醒词
            if(mAsr!=null)
            {
                mAsr.stopListening();
            }
            textView.setText(resultString);
            //Log.e("111","mRecognizerListener");
/*
			i++;
			if(i>1&&i<=10)
			{
				String dynasty="dynasty"+i;
				mLocalGrammar = readFile(OneShotDemo.this, dynasty+".bnf", "utf-8");
				mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, dynasty);
			}
			else {
				mLocalGrammar = readFile(OneShotDemo.this, "dynasty1.bnf", "utf-8");
				mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "dynasty1");
			}

 */
            mLocalGrammar = readFile(MainActivity.this, "voiceadjust.bnf", "utf-8");
            mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "voiceadjust");
            mAsr.buildGrammar("bnf", mLocalGrammar, grammarListener);

            mAsr.startListening(quesRecognizerListener);


        }

        @Override
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
        }


        @Override
        public void onBeginOfSpeech() {
            showTip("开始说话");
        }

        @Override
        public void onEvent(int eventType, int isLast, int arg2, Bundle obj) {
            Log.d(TAG, "eventType:"+eventType+ "arg1:"+isLast + "arg2:" + arg2);
            // 识别结果
            //if (SpeechEvent.EVENT_IVW_RESULT == eventType) {
            //	RecognizerResult reslut = ((RecognizerResult)obj.get(SpeechEvent.KEY_EVENT_IVW_RESULT));
            //	recoString += JsonParser.parseGrammarResult(reslut.getResultString());
            //	textView.setText(recoString);
            //}
        }

        @Override
        public void onVolumeChanged(int volume) {
            // TODO Auto-generated method stub
        }

    };
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mMediaPlayer!=null) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer=null;
        }
        if (noposplayer!=null) {
            noposplayer.stop();
            noposplayer.reset();
            noposplayer.release();
            noposplayer=null;
        }
        if (questionPlayer!=null) {
            questionPlayer.stop();
            questionPlayer.reset();
            questionPlayer.release();
            questionPlayer=null;
        }
        mIvw = VoiceWakeuper.getWakeuper();
        if (mIvw != null) {
            mIvw.destroy();
        } else {
            showTip("唤醒未初始化");
        }

        if( null != mAsr ){
            // 退出时释放连接
            mAsr.cancel();
            mAsr.destroy();
        }
    }

    /**
     * 读取asset目录下文件。
     *
     * @return content
     */
    public static String readFile(Context mContext, String file, String code) {
        int len = 0;
        byte[] buf = null;
        String result = "";
        try {
            InputStream in = mContext.getAssets().open(file);
            len = in.available();
            buf = new byte[len];
            in.read(buf, 0, len);

            result = new String(buf, code);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }



    // 获取识别资源路径
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        // 识别通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this,
                ResourceUtil.RESOURCE_TYPE.assets, "asr/common.jet"));
        return tempBuffer.toString();
    }

    private void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }

    /*
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "onRequestPermissionsResult granted");
            startLocation();
        } else {
            Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show();
        }

    }

 */
    //请求权限
    private void requestPermissions(){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int permission = ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if(permission!= PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,new String[] {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.LOCATION_HARDWARE,Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.WRITE_SETTINGS,Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_CONTACTS,
                            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},0x0010);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //权限回调函数
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

