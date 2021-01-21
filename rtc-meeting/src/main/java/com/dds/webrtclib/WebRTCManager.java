package com.dds.webrtclib;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.dds.webrtclib.bean.MediaType;
import com.dds.webrtclib.bean.MyIceServer;
import com.dds.webrtclib.ws.IConnectEventListener;
import com.dds.webrtclib.ws.ISignalingEventsListener;
import com.dds.webrtclib.ws.IWebSocketListener;
import com.dds.webrtclib.ws.JavaWebSocket;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;

import java.util.ArrayList;
import java.util.List;

/**
 * 控制信令和各种操作
 * Created by dds on 2019/4/5.
 * android_shuai@163.com
 */
public class WebRTCManager implements ISignalingEventsListener {
    private final static String TAG = "WebRTCManager";
    private String _wss;
    private MyIceServer[] _iceServers;

    private IWebSocketListener javaWebSocket;
    private PeerConnectionHelper _peerHelper;

    private String _roomId;
    private int _mediaType;
    private boolean _videoEnable;


    private IConnectEventListener connectEventListener;
    private Handler handler = new Handler(Looper.getMainLooper());

    public static WebRTCManager getInstance() {
        return Holder.wrManager;
    }

    private static class Holder {
        private static WebRTCManager wrManager = new WebRTCManager();
    }

    // init address
    public void init(String wss, MyIceServer[] iceServers, IConnectEventListener listener) {
        this._wss = wss;
        this._iceServers = iceServers;
        connectEventListener = listener;
    }

    // connect
    public void connect(int mediaType, String roomId) {
        if (javaWebSocket == null) {
            _mediaType = mediaType;
            _videoEnable = mediaType != MediaType.TYPE_AUDIO;
            _roomId = roomId;
            javaWebSocket = new JavaWebSocket(this);
            javaWebSocket.connect(_wss);
            _peerHelper = new PeerConnectionHelper(javaWebSocket, _iceServers);
        } else {
            // 正在通话中
            javaWebSocket.close();
            javaWebSocket = null;
            _peerHelper = null;
        }

    }


    public void setCallback(IViewCallback callback) {
        if (_peerHelper != null) {
            _peerHelper.setViewCallback(callback);
        }
    }

    //===================================控制功能==============================================
    public void joinRoom(Context context, EglBase eglBase) {
        if (_peerHelper != null) {
            _peerHelper.initContext(context, eglBase);
        }
        if (javaWebSocket != null) {
            javaWebSocket.joinRoom(_roomId);
        }

    }

    public void switchCamera() {
        if (_peerHelper != null) {
            _peerHelper.switchCamera();
        }
    }

    public void toggleMute(boolean enable) {
        if (_peerHelper != null) {
            _peerHelper.toggleMute(enable);
        }
    }

    public void toggleSpeaker(boolean enable) {
        if (_peerHelper != null) {
            _peerHelper.toggleSpeaker(enable);
        }
    }

    public void exitRoom() {
        if (_peerHelper != null) {
            javaWebSocket = null;
            _peerHelper.exitRoom();
        }
    }

    // ==================================信令回调===============================================
    @Override
    public void onWebSocketOpen() {
        handler.post(() -> {
            if (connectEventListener != null) {
                connectEventListener.onSuccess();
            }
        });
    }

    @Override
    public void onWebSocketOpenFailed(String msg) {
        handler.post(() -> {
            if (javaWebSocket != null && !javaWebSocket.isOpen()) {
                connectEventListener.onFailed(msg);
            } else {
                if (_peerHelper != null) {
                    _peerHelper.exitRoom();
                }
            }
        });

    }

    @Override
    public void onJoinToRoom(ArrayList<String> connections, String myId) {
        handler.post(() -> {
            if (_peerHelper != null) {
                _peerHelper.onJoinToRoom(connections, myId, _videoEnable, _mediaType);
                if (_mediaType == MediaType.TYPE_VIDEO || _mediaType == MediaType.TYPE_MEETING) {
                    toggleSpeaker(true);
                }
            }
        });

    }

    @Override
    public void onRemoteJoinToRoom(String socketId) {
        handler.post(() -> {
            if (_peerHelper != null) {
                _peerHelper.onRemoteJoinToRoom(socketId);

            }
        });

    }

    @Override
    public void onRemoteIceCandidate(String socketId, IceCandidate iceCandidate) {
        handler.post(() -> {
            if (_peerHelper != null) {
                _peerHelper.onRemoteIceCandidate(socketId, iceCandidate);
            }
        });

    }

    @Override
    public void onRemoteIceCandidateRemove(String socketId, List<IceCandidate> iceCandidates) {
        handler.post(() -> {
            if (_peerHelper != null) {
                _peerHelper.onRemoteIceCandidateRemove(socketId, iceCandidates);
            }
        });

    }

    @Override
    public void onRemoteOutRoom(String socketId) {
        handler.post(() -> {
            if (_peerHelper != null) {
                _peerHelper.onRemoteOutRoom(socketId);
            }
        });

    }

    @Override
    public void onReceiveOffer(String socketId, String sdp) {
        handler.post(() -> {
            Log.d(TAG, String.format("- onReceiveOffer: socketId:%s, sdp:%s", socketId, sdp));
            if (_peerHelper != null) {
                _peerHelper.onReceiveOffer(socketId, sdp);
            }
        });

    }

    @Override
    public void onReceiverAnswer(String socketId, String sdp) {
        handler.post(() -> {
            if (_peerHelper != null) {
                _peerHelper.onReceiverAnswer(socketId, sdp);
            }
        });
    }
}
