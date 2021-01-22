package com.dds.webrtclib.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.dds.webrtclib.IViewCallback;
import com.dds.webrtclib.PeerConnectionHelper;
import com.dds.webrtclib.ProxyVideoSink;
import com.dds.webrtclib.R;
import com.dds.webrtclib.WebRTCManager;
import com.dds.webrtclib.utils.PermissionUtil;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * 单聊界面
 * 1. 一对一视频通话
 * 2. 一对一语音通话
 */
public class ChatSingleActivity extends AppCompatActivity implements View.OnClickListener {
    private SurfaceViewRenderer localViewRenderer;
    private SurfaceViewRenderer remoteViewRenderer;
    private ProxyVideoSink localProxySink;
    private ProxyVideoSink remoteProxySink;

    private WebRTCManager manager;
    private boolean videoEnable;
    private boolean isSwappedFeeds;
    private EglBase rootEglBase;

    public static void openActivity(Activity activity, boolean videoEnable) {
        Intent intent = new Intent(activity, ChatSingleActivity.class);
        intent.putExtra("videoEnable", videoEnable);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wr_activity_chat_single);
        initVar();
        initListener();
    }

    private void initVar() {
        Intent intent = getIntent();
        videoEnable = intent.getBooleanExtra("videoEnable", false);

        ChatSingleFragment chatSingleFragment = new ChatSingleFragment();
        replaceFragment(chatSingleFragment, videoEnable);
        rootEglBase = EglBase.create();
        if (videoEnable) {
            localViewRenderer = findViewById(R.id.local_view_render);
            remoteViewRenderer = findViewById(R.id.remote_view_render);
            // 本地图像初始化
            localViewRenderer.init(rootEglBase.getEglBaseContext(), null);
            localViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            localViewRenderer.setZOrderMediaOverlay(true);
            localViewRenderer.setMirror(true);
            localProxySink = new ProxyVideoSink();
            //远端图像初始化
            remoteViewRenderer.init(rootEglBase.getEglBaseContext(), null);
            remoteViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
            remoteViewRenderer.setMirror(true);
            remoteProxySink = new ProxyVideoSink();
            setSwappedFeeds(true);
            localViewRenderer.setOnClickListener(this);
        }

        startCall();
    }

    private int previewX, previewY;
    private int lastX, lastY;
    private boolean isDragged = false; //View是否被移动过
    private boolean isDrag = false; //判断是拖动还是点击

    @SuppressLint("ClickableViewAccessibility")
    private void initListener() {
        if (videoEnable) {
            // 设置小视频可以移动
            localViewRenderer.setOnTouchListener((view, motionEvent) -> {
                boolean res = false;
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        previewX = (int) motionEvent.getX();
                        previewY = (int) motionEvent.getY();
                        lastX = (int) motionEvent.getRawX();
                        lastY = (int) motionEvent.getRawY();
                        isDrag = false;
                        isDragged = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int x = (int) motionEvent.getX();
                        int y = (int) motionEvent.getY();
                        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) localViewRenderer.getLayoutParams();
                        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0); // Clears the rule, as there is no removeRule until API 17.
                        lp.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
                        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                        lp.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
                        int left = lp.leftMargin + (x - previewX);
                        int top = lp.topMargin + (y - previewY);
                        lp.leftMargin = left;
                        lp.topMargin = top;
                        view.setLayoutParams(lp);

                        //手指在屏幕上移动的距离
                        int dx = (int) motionEvent.getRawX() - lastX;
                        int dy = (int) motionEvent.getRawY() - lastY;
                        if (isDragged) {
                            //如果已经被拖动过，那么无论本次移动的距离是否为零，都判定本次事件为拖动事件
                            isDrag = true;
                        } else {
                            if (dx == 0 && dy == 0) {
                                //如果移动的距离为零，则认为控件没有被拖动过，灵敏度可以自己控制
                                isDragged = false;
                            } else {
                                isDragged = true;
                                isDrag = true;
                            }
                        }

                        lastX = (int) motionEvent.getRawX();
                        lastY = (int) motionEvent.getRawY();
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                }

                //执行onClick回调
                return isDrag;
            });
        }
    }

    private void setSwappedFeeds(boolean isSwappedFeeds) {
        this.isSwappedFeeds = isSwappedFeeds;
        localProxySink.setTarget(isSwappedFeeds ? remoteViewRenderer : localViewRenderer);
        remoteProxySink.setTarget(isSwappedFeeds ? localViewRenderer : remoteViewRenderer);
    }

    private void startCall() {
        manager = WebRTCManager.getInstance();
        manager.setCallback(new IViewCallback() {
            @Override
            public void onSetMirror4SurfaceViewRenderer(boolean mirror) {
                localViewRenderer.setMirror(mirror);
                remoteViewRenderer.setMirror(mirror);
            }

            @Override
            public void onSetLocalStream(MediaStream stream, String socketId) {
                if (stream.videoTracks.size() > 0) {
                    stream.videoTracks.get(0).addSink(localProxySink);
                }

                if (videoEnable) {
                    stream.videoTracks.get(0).setEnabled(true);
                }
            }

            @Override
            public void onAddRemoteStream(MediaStream stream, String socketId) {
                if (stream.videoTracks.size() > 0) {
                    stream.videoTracks.get(0).addSink(remoteProxySink);
                }
                if (videoEnable) {
                    stream.videoTracks.get(0).setEnabled(true);
                    runOnUiThread(() -> setSwappedFeeds(false));
                }
            }

            @Override
            public void onCloseWithId(String socketId) {
                runOnUiThread(() -> {
                    disConnect();
                    ChatSingleActivity.this.finish();
                });

            }
        });
        if (!PermissionUtil.isNeedRequestPermission(ChatSingleActivity.this)) {
            manager.joinRoom(getApplicationContext(), rootEglBase);
        }

    }

    private void replaceFragment(Fragment fragment, boolean videoEnable) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("videoEnable", videoEnable);
        fragment.setArguments(bundle);
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.wr_container, fragment)
                .commit();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
    }

    // 切换摄像头
    public void switchCamera() {
        manager.switchCamera();
    }

    // 挂断
    public void hangUp() {
        disConnect();
        this.finish();
    }

    // 静音
    public void toggleMic(boolean enable) {
        manager.toggleMute(enable);
    }

    // 扬声器
    public void toggleSpeaker(boolean enable) {
        manager.toggleSpeaker(enable);

    }

    @Override
    protected void onDestroy() {
        disConnect();
        super.onDestroy();

    }

    private void disConnect() {
        manager.exitRoom();
        if (localProxySink != null) {
            localProxySink.setTarget(null);
            localProxySink = null;
        }
        if (remoteProxySink != null) {
            remoteProxySink.setTarget(null);
            remoteProxySink = null;
        }

        if (localViewRenderer != null) {
            localViewRenderer.release();
            localViewRenderer = null;
        }
        if (remoteViewRenderer != null) {
            remoteViewRenderer.release();
            remoteViewRenderer = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            Log.i(PeerConnectionHelper.TAG, "[Permission] " + permissions[i] + " is " + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                finish();
                break;
            }
        }
        manager.joinRoom(getApplicationContext(), rootEglBase);

    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.local_view_render) {
            setSwappedFeeds(!isSwappedFeeds);
        }
    }
}
