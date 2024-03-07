package bgu.spl.net.impl.tftp;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionHandler;
import java.util.concurrent.ConcurrentHashMap;

//or brodcast or for only one client
//maps for id, move by iterator for the ones you want to send to
//when brodcast, syncronized, because there are tpc and server thread

public class ConnectionsImpl<T> implements Connections<T> {
    private ConcurrentHashMap<Integer, ConnectionHandler<T>> connectionsMap;

    public ConnectionsImpl(){
        connectionsMap = new ConcurrentHashMap<>();
    }
    
    public boolean connect(int connectionId, ConnectionHandler<T> handler){
        if(connectionsMap.containsKey(connectionId))
            return false;
        else{
            connectionsMap.put(connectionId, handler);
            return true;
        }
    }

    public boolean send(int connectionId, T msg){
        connectionsMap.get(connectionId).send(msg);
        return true;
    }

    public void disconnect(int connectionId){
        if(connectionsMap.containsKey(connectionId))
            connectionsMap.remove(connectionId);
    }


}


   