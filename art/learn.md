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

CallSingleActivity -> SkyEngineKit: 1. init(stun, turn urls)\n2. startOutCall
SkyEngineKit -> CallSession: 1. init CallSession\n2. createHome
CallSession -> VoipEvent: new Thread(createRoom)
VoipEvent -> SocketManager: createRoom
SocketManager -> DWebSocket: 1. createRoom\n2. req event:__create
DWebSocket -> DWebSocket: 1. ack event:__peers\n2. handlePeers
DWebSocket -> SocketManager: onPeers //enter room
SocketManager -> CallSession: onJoinHome
CallSession -> VoipEvent: 1. isComming=false\n2. sendInvite
CallSession -> VideoFragment: didCreateLocalVideoTrack
VoipEvent -> SocketManager: sendInvite
SocketManager -> DWebSocket: 1. req event:__invite\n2. sendInvite
DWebSocket -> DWebSocket: 1. ack event:__new_peer\n2. handleNewPeer
DWebSocket -> SocketManager: onNewPeer
SocketManager -> CallSession: newPeer
CallSession -> Peer: 1. new Peer\n2. pc.addStream(localStream)
CallSession -> VideoFragment: didChangeState(connected)
DWebSocket -> DWebSocket: 1. ack event:__offer\n2. handleOffer
DWebSocket -> SocketManager: onOffer
SocketManager -> CallSession: onReceiverOffer(userid, sdp)
CallSession -> Peer: 1. setOffer(false)\n2. pc.setRemoteDescription\n3. createAnswer
Peer -> Peer: 1. onCreateSuccess callback\n2. new Thread(pc.setLocalDescription)\n3. onSetSuccess callback
Peer -> VoipEvent: sendAnswer
VoipEvent -> SocketManager: sendAnswer
SocketManager -> DWebSocket: 1. req event:__answer\n2. sendAnswer
Peer -> Peer: 1. PeerConnection.Observer interface\n2. onIceCandidate callback
Peer -> VoipEvent: sendIceCandidate
VoipEvent -> SocketManager: sendIceCandidate
SocketManager -> DWebSocket: 1. sendIceCandidate\n2. req event:__ice_candidate
DWebSocket -> DWebSocket: 1. ack event:__ice_candidate\n2. handleIceCandidate
DWebSocket -> SocketManager: onIceCandidate
SocketManager -> CallSession: onRemoteIceCandidate
CallSession -> Peer: 1. new IceCandidate\n2. addRemoteIceCandidate\n3. pc.addIceCandidate
Peer -> Peer: PeerConnection.Observer callback AddStream
Peer-> CallSession: fragment callback
CallSession -> VideoFragment: didReceiveRemoteVideoTrack

@enduml


##### OutCall 被动呼叫时序图
@startuml 
participant "CallSingleActivity" 
participant "SkyEngineKit" 
participant "CallSession" 
participant "VoipEvent" 
participant "SocketManager" 
participant "DWebSocket" 
participant "Peer" 
participant "VideoFragment"
participant "VoipReceiver"

DWebSocket -> DWebSocket: 1. ack __invite\n2. handleInvite
DWebSocket -> SocketManager: onInvite(room, audioOnly, inviteId, userList)
SocketManager -> VoipReceiver: intent broadcast
VoipReceiver -> SkyEngineKit: 1. init(stun, turn urls)\n2. startInCall
VoipReceiver -> CallSingleActivity: 1. startActivity\n2. init VideoFragment
SkyEngineKit -> CallSession: shouldStartRing
CallSession -> VoipEvent: shouldStartRing
SkyEngineKit -> CallSession: sendRingBack
CallSession -> VoipEvent: sendRingBack
VoipEvent -> SocketManager: sendRingBack
SocketManager -> DWebSocket: 1. sendRing\n2. req event:__ring
DWebSocket -> DWebSocket: 1. ack event:__ring\n2. handleRing
DWebSocket -> SocketManager: onRing
SocketManager -> CallSession: onRingBack
CallSession -> VoipEvent: shouldStartRing
DWebSocket -> DWebSocket: 1. ack event:__peers (enter room)\n2. handlePeers
DWebSocket -> SocketManager: onPeers
SocketManager -> CallSession: onJoinHome
CallSession -> Peer: 1. new Peer\n2. setOffer(true)\n3. pc.addStream(localStram)\n4. pc.createOffer
Peer -> Peer: 1. SdpObserver callback onCreateSuccess\n2. new Thread(pc.setLocalDescription)\n3. SdpObserver callback onSetSuccess
Peer -> VoipEvent: sendOffer
VoipEvent -> SocketManager: sendOffer
SocketManager -> DWebSocket: 1. sendOffer\n2. req event:__offer\n
DWebSocket -> DWebSocket: 1. ack event:__answer\n2. handleAnswer
DWebSocket -> SocketManager: onAnswer
SocketManager -> CallSession: onReceiverAnswer
CallSession -> Peer: pc.setRemoteDescription
Peer -> Peer: SdpObserver callback onSetSuccess\n2. remoteDSP ready/drainCandidates
Peer -> Peer: 1. PeerConnection.Observer interface\n2. onIceCandidate callback
Peer -> VoipEvent: sendIceCandidate
VoipEvent -> SocketManager: sendIceCandidate
SocketManager -> DWebSocket: 1. sendIceCandidate\n2. req event:__ice_candidate
DWebSocket -> DWebSocket: 1. ack event:__ice_candidate\n2. handleIceCandidate
DWebSocket -> SocketManager: onIceCandidate
SocketManager -> CallSession: onRemoteIceCandidate
CallSession -> Peer: 1. new IceCandidate\n2. addRemoteIceCandidate\n3. pc.addIceCandidate
Peer -> Peer: PeerConnection.Observer callback AddStream
Peer-> CallSession: fragment callback
CallSession -> VideoFragment: didReceiveRemoteVideoTrack

@enduml