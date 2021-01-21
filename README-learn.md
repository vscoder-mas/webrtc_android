#### 学习笔记   

+ 用户id: 后台以socketid做为用户id  
+ 发送offer/answer与发送iceCandidate是并行的  
+ 接收Offer
  ```
  先接收到Offer，解析offer的sdp, 将sdp设置到 setRemoteDescription
  回调顺序为:
  1. onSetSuccess 在此回调中调用 createAnswer，回调 onCreateSuccess
  2. onCreateSuccess 回调中设置 setLocalDescription
  3. onSetSuccess 在此回调中调用 sendAnswer (PeerConnection.SignalingState.STABLE)
  ```
+ 发送Offer
  ```
  先发送Offer，及调用createOffer
  回调顺序：
  1. onCreateSuccess 在此回调中设置 setLocalDescription
  2. onSetSuccess 在此回调中调用 sendOffer
  3. onReceiverAnswer -> setRemoteDescription -> onSetSuccess (PeerConnection.SignalingState.STABLE)
  ```

