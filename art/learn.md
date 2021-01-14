### webrtc android 学习笔记
##### OutCall 主动呼叫时序图
@startuml 
participant "CallSingleActivity" 
participant "SkyEngineKit" 
participant "CallSession" 
participant "VoipEvent" 
participant "SocketManager" 
participant "DWebSocket" 
participant "Peer" 
participant "VideoFragment"

CallSingleActivity -> SkyEngineKit: startOutCall
SkyEngineKit -> CallSession: 1. init CallSession\n 2. createHome
CallSession -> VoipEvent: new Thread(createRoom)
VoipEvent -> SocketManager: createRoom
SocketManager -> DWebSocket: 1. createRoom\n 2. req event:__create
DWebSocket -> DWebSocket: 1. ack event:__peers\n 2. handlePeers
DWebSocket -> SocketManager: onPeers //enter room
SocketManager -> CallSession: onJoinHome
CallSession -> VoipEvent: 1. isComming=false\n 2. sendInvite
CallSession -> VideoFragment: didCreateLocalVideoTrack
VoipEvent -> SocketManager: sendInvite
SocketManager -> DWebSocket: 1. req event:__invite\n 2. sendInvite
DWebSocket -> DWebSocket: 1. ack event:__new_peer\n 2. handleNewPeer
DWebSocket -> SocketManager: onNewPeer
SocketManager -> CallSession: newPeer
CallSession -> Peer: 1. new Peer\n pc.addStream(localStream)
CallSession -> VideoFragment: didChangeState(connected)
DWebSocket -> DWebSocket: 1. ack event:__offer\n 2. handleOffer
DWebSocket -> SocketManager: onOffer
SocketManager -> CallSession: onReceiverOffer(userid, sdp)
CallSession -> Peer: 1. setOffer(false)\n 2. pc.setRemoteDescription\n 3. createAnswer
Peer -> Peer: 1. OnCreateSuccess callback\n 2. new Thread(pc.setLocalDescription)\n 3. onSetSuccess callback
Peer -> VoipEvent: sendAnswer
VoipEvent -> SocketManager: sendAnswer
SocketManager -> DWebSocket: 1. req event:__answer\n 2. sendAnswer
Peer -> Peer: 1. PeerConnection.Observer interface\n 2. onIceCandidate callback
Peer -> VoipEvent: sendIceCandidate
VoipEvent -> SocketManager: sendIceCandidate
SocketManager -> DWebSocket: 1. sendIceCandidate\n 2. req event:__ice_candidate
DWebSocket -> DWebSocket: 1. ack event:__ice_candidate\n 2. handleIceCandidate
DWebSocket -> SocketManager: onIceCandidate
SocketManager -> CallSession: onRemoteIceCandidate
CallSession -> Peer: 1. new IceCandidate\n 2. addRemoteIceCandidate\n 3. pc.addIceCandidate
Peer -> Peer: PeerConnection.Observeron callback AddStream
Peer-> CallSession: fragment callback
CallSession -> VideoFragment: didReceiveRemoteVideoTrack

@enduml

