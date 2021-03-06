package com.gmail.fomichov.m.youtubeanalytics.request;

import com.alibaba.fastjson.JSON;
import com.gmail.fomichov.m.youtubeanalytics.MainActivity;
import com.gmail.fomichov.m.youtubeanalytics.json.json_playlist.Playlist;
import com.gmail.fomichov.m.youtubeanalytics.json.json_video.VideoList;
import com.gmail.fomichov.m.youtubeanalytics.utils.MyLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CommentsRequest {
    private final String HTTP_URL_PARSE_PLAYLIST = "https://www.googleapis.com/youtube/v3/playlistItems";
    private final String HTTP_URL_PARSE_VIDEO = "https://www.googleapis.com/youtube/v3/videos";
    private String idPlayList;
    private static Semaphore semaphore = new Semaphore(50);

    public CommentsRequest() {
    }

    public CommentsRequest(String idPlayList) {
        this.idPlayList = idPlayList;
    }

    // получаем обьект плейлист
    public Playlist getListIdVideo(final String nextPageToken) throws ExecutionException, InterruptedException {
        FutureTask<String> futureTask = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String json = null;
                HttpUrl.Builder urlBuilder = HttpUrl.parse(HTTP_URL_PARSE_PLAYLIST).newBuilder();
                urlBuilder.addQueryParameter("part", "contentDetails");
                urlBuilder.addQueryParameter("playlistId", idPlayList);
                urlBuilder.addQueryParameter("maxResults", "50");
                urlBuilder.addQueryParameter("pageToken", nextPageToken);
                urlBuilder.addQueryParameter("key", MainActivity.KEY_YOUTUBE_API);
                Request request = new Request.Builder()
                        .url(urlBuilder.build().toString())
                        .build();
                OkHttpClient client = new OkHttpClient();
                Response response = null;
                try {
                    response = client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    json = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return json;
            }
        });
        new Thread(futureTask).start();
        return JSON.parseObject(futureTask.get(), Playlist.class);
    }

    // получам лист обьектов видеоАйди c количеством комментариев
    public List<VideoList> getVideoListId(final List<String> videoIdArray) throws ExecutionException, InterruptedException {
        List<VideoList> videoLists = new ArrayList<>();
        List<FutureTask> taskList = new ArrayList<>();
        for (int i = 0; i < videoIdArray.size(); i++) {
            final int finalI = i;
            final FutureTask<String> futureTask = new FutureTask<String>(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String json = null;
                    HttpUrl.Builder urlBuilder = HttpUrl.parse(HTTP_URL_PARSE_VIDEO).newBuilder();
                    urlBuilder.addQueryParameter("part", "statistics");
                    urlBuilder.addQueryParameter("id", videoIdArray.get(finalI));
                    urlBuilder.addQueryParameter("key", MainActivity.KEY_YOUTUBE_API);
                    Request request = new Request.Builder()
                            .url(urlBuilder.build().toString())
                            .build();
                    OkHttpClient client = new OkHttpClient();
                    Response response = null;
                    try {
                        response = client.newCall(request).execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        json = response.body().string();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return json;
                }
            });
            taskList.add(futureTask);
            new Thread(futureTask).start();
        }
        for (FutureTask value : taskList) {
            videoLists.add(JSON.parseObject((String) value.get(), VideoList.class));
        }
        return videoLists;
    }
}

