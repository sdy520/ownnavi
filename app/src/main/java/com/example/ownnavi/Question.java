package com.example.ownnavi;

import android.media.MediaPlayer;

public class Question {
    /*public int ques_number;
    public MediaPlayer questionPlayer= new MediaPlayer();
    //播放问题语音

    public Question(){}
    public Question(int number){
        ques_number=number;
    }
    @Override
    public void run() {
        String filename = "/storage/emulated/0/dynasty";
        filename=filename+ques_number+".mp3";
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
                MainActivity.setgram();
            }
        });
    }*/
}
