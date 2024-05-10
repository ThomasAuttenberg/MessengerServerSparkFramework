package org.messenger.comminications;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.messenger.data.entities.AuthToken;
import org.messenger.data.entities.Subscription;
import org.messenger.data.entities.User;
import org.messenger.data.implementations.AuthTokenDAO;
import org.messenger.data.implementations.SubscriptionsDAO;
import org.messenger.data.implementations.UsersDAO;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@WebSocket
public class NotificationConnection {

    static private final AuthTokenDAO authTokenDao = new AuthTokenDAO();
    static private final SubscriptionsDAO subsDao = new SubscriptionsDAO();
    static private final UsersDAO usersDao = new UsersDAO();
    static private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<Session>> listeners = new ConcurrentHashMap<>();
    static private final ConcurrentHashMap<Long, Session> userIdToSession = new ConcurrentHashMap<>();

    private Session currentSession;
    private Long userId;
    private final LinkedList<Long> listeningThreads = new LinkedList<>();

    @OnWebSocketConnect
    public void onConnect(Session session){
        currentSession = session;
        System.out.println("Notification connection established on "+session.getRemote().getInetSocketAddress());
    }
    @OnWebSocketClose
    public void onClose(int statusCode, String reason){
        System.out.println("Websocket on "+currentSession.getRemote().getInetSocketAddress()+" closed because of "+reason);
        for(Long thread : listeningThreads){
            var sessionsOnThread = listeners.get(thread);
            sessionsOnThread.remove(currentSession);
                if(sessionsOnThread.isEmpty()){
                        listeners.remove(sessionsOnThread);
                }
        }
        if(userId != null) userIdToSession.remove(userId);

    }
    @OnWebSocketError
    public void onError(Session session, Throwable error){
        //onClose(1006,"Abnormal session closure");
        session.close();
    }
    @OnWebSocketMessage
    public void onMessage(Session session, String message){
        if(message.equals("alive")){
            return;
        }
        AuthToken token = authTokenDao.getByToken(message);
        if(token == null) {
            try {
                session.getRemote().sendString("Incorrect token provided");
                session.close();
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        userId = token.getUserid();
        LinkedList<Subscription> subs = subsDao.getUserSubscriptions(userId);
        userIdToSession.put(userId,session);

        for (Subscription sub:subs) {
            Long parentMessageId = sub.getParentMessageId();
            var perThreadListeners = listeners.get(parentMessageId);
            if(perThreadListeners == null) listeners.put(parentMessageId,new ConcurrentLinkedQueue<>());
            listeners.get(parentMessageId).add(session);
            listeningThreads.add(sub.getParentMessageId());
        }
    }

    public static void notifyInThread(Long threadId){
        if(listeners.get(threadId) != null)
            for(Session session : listeners.get(threadId)){
                try {
                    session.getRemote().sendString(threadId.toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
    }
    public static void addUserListener(Long userId, Long newSub){
        Session session = userIdToSession.get(userId);
        if(session == null) return;
        var listenersPerThread = listeners.get(newSub);
        if(listenersPerThread == null)
            listeners.put(newSub,new ConcurrentLinkedQueue<>());
        listeners.get(newSub).add(session);
    }
    public static void removeUserListener(Long userId, Long removingSub){
        var listenersPerThread = listeners.get(removingSub);
        if(listenersPerThread == null) return;
        listenersPerThread.remove(userIdToSession.get(userId));
    }

}
