#### webrtc android 学习笔记


@startuml 
participant "CallSingleActivity" 
participant "VideoFragment"
participant "SkyEngineKit" 
participant "CallSession" 
participant "Peer" 
participant "SocketManager" 
participant "DWebSocket" 
participant "VoipEvent" 


<!-- 
rnote over "ConnectHelper"
    init socket 
endrnote 
activate "ConnectHelper" 
"ConnectHelper" -> "ConnectHelper": 1. init data\n2. init socket\n3. set conn status:connect 
deactivate "ConnectHelper" 
"ConnectHelper" -> "SendAckMonitor": add action:CONNECT/HELPER msg 
"ReceiveThread" -> "SendAckMonitor": 1. receive action:SecureConnect/VerifyClient msg\n2. del msg, action:CONNECT/HELPER 
"ReceiveThread" -> "VerifyClientProcessor": receive action:SecureConnect/VerifyClient msg 
"VerifyClientProcessor"-> "VerifyClientProcessor":1. gen ram 16bytes token (local)\n2. md5(server.token):password\n 
"VerifyClientProcessor"-> "SendThread":send action:SecureConnect/VerifyServer msg(pwd,token)
"ReceiveThread" -> "SendAckMonitor":1. receive action:SecureConnect/PublicKey msg\n2. del msg, action:SecureConnect/VerifyServer 
"ReceiveThread" -> "PublicKeyProcessor":receive action:SecureConnect/PublicKey msg
"PublicKeyProcessor" -> "PublicKeyProcessor":1. get password & equals(md5(local token))\n2. get publickey\n3. gen ram 16bytes key(blowfish key)\n4. RSA(pubkey).encrpyt(ram) 
"PublicKeyProcessor" -> "SendThread":1. send action:SecureConnect/Hello msg\n2. send action:regist msg(ip, mainctl) 
"PublicKeyProcessor"-> "ConnectHelper": set conn status:login
"ReceiveThread" -> "ReceiveThread": set conn status: from login to online  
"ReceiveThread" -> "SendAckMonitor": 1. receive action:return msg\n2. del msg, action:regist by id(timestamp)\n3. socket conn complete ! 
rnote over "ConnectHelper", "CommandProcessor" 
socket conn complete 
endrnote 
"HeartBeatThread" -> "SendThread": send action:heartbeat msg(mainctl, timestamp) 
"ReceiveThread" -> "SendAckMonitor": 1. receive action:return msg\n2. del msg action:heartbeat by id(timestamp) 
rnote over "ConnectHelper", "CommandProcessor" 
negative receive command 
endrnote 
"ReceiveThread" -> "CommandProcessor": negative receive action:command msg 
"CommandProcessor" -> "SendThread": 1. ack send action return msg(id,result) 
activate "SendAckMonitor" 
"SendAckMonitor" -> "SendAckMonitor": 1. delay 20s\n2. period 10s\n3. del timeout action:heartbeat task\n4. judge condition:cur - sendtime > 10s 
deactivate "SendAckMonitor" -->

@enduml

