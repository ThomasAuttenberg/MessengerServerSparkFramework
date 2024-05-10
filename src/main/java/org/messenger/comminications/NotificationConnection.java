package org.messenger.comminications;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.messenger.data.entities.AuthToken;
import org.messenger.data.entities.Subscription;
import org.messenger.data.implementations.AuthTokenDAO;
import org.messenger.data.implementations.MessageDAO;
import org.messenger.data.implementations.SubscriptionsDAO;
import org.messenger.data.implementations.UsersDAO;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@WebSocket
public class NotificationConnection {

    static private final AuthTokenDAO authTokenDao = new AuthTokenDAO();
    static private final SubscriptionsDAO subsDao = new SubscriptionsDAO();
    static private final UsersDAO usersDao = new UsersDAO();
    static private final HashMap<Session,Long> uniqueWatchingThread = new HashMap<>();
    static private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<Session>> listeners = new ConcurrentHashMap<>();
    static private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<Session>> userIdToSessions = new ConcurrentHashMap<>();
    static private final ConcurrentHashMap<Session,Long> sessionToUserId = new ConcurrentHashMap<>();
    static private final ConcurrentHashMap<Long,ConcurrentLinkedQueue<Long>> userListeningThreads = new ConcurrentHashMap<>();

    //private Session currentSession;
    //private Long userId;
    //private final LinkedList<Long> listeningThreads = new LinkedList<>();

    @OnWebSocketConnect
    public void onConnect(Session session){
        System.out.println("Notification connection established on "+session.getRemote().getInetSocketAddress());
    }
    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason){
        System.out.println("Websocket on "+session.getRemote().getInetSocketAddress()+" closed because of "+reason);
        for(Long thread : userListeningThreads.get(sessionToUserId.get(session))){
            var sessionsOnThread = listeners.get(thread);
            sessionsOnThread.remove(session);
            if(sessionsOnThread.isEmpty()){
                listeners.remove(sessionsOnThread);
            }
        }
        Long unqWatchingThread = uniqueWatchingThread.get(session);
        if(unqWatchingThread != null){
            listeners.get(unqWatchingThread).remove(session);
            uniqueWatchingThread.remove(session);
        }
        Long userId = sessionToUserId.get(session);
        userIdToSessions.get(userId).remove(session);
        sessionToUserId.remove(session);
        if(userIdToSessions.get(userId).isEmpty()) userIdToSessions.remove(userId);

    }
    @OnWebSocketError
    public void onError(Session session, Throwable error){
        //onClose(1006,"Abnormal session closure");
        session.close();
    }
    @OnWebSocketMessage
    public void onMessage(Session session, String message){

        if(message.matches("^view:.*")){
            String id = message.substring(5);
            Long thread = Long.parseLong(id);
            if(listeners.get(thread) == null) listeners.put(thread, new ConcurrentLinkedQueue<>());
            listeners.get(thread).add(session);
            uniqueWatchingThread.put(session,thread);
            System.out.println(session.getRemote().getInetSocketAddress() + " set view on "+thread);
            return;
        }
        if(message.matches("unview")){
            listeners.get(uniqueWatchingThread.get(session)).remove(session);
            uniqueWatchingThread.remove(session);
            System.out.println(session.getRemote().getInetSocketAddress() + " unview ");
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

        Long userId = token.getUserid();

        LinkedList<Subscription> subs = subsDao.getUserSubscriptions(userId);
        var userSessions = userIdToSessions.get(userId);
        if (userSessions == null) userIdToSessions.put(userId, new ConcurrentLinkedQueue<>());
        userIdToSessions.get(userId).add(session);
        sessionToUserId.put(session,userId);

        boolean userListeningThreadsNeedToBeSetted = userListeningThreads.get(userId) == null;
        if(userListeningThreadsNeedToBeSetted) userListeningThreads.put(userId, new ConcurrentLinkedQueue<>());

        for (Subscription sub : subs) {
            Long parentMessageId = sub.getParentMessageId();
            var perThreadListeners = listeners.get(parentMessageId);
            if (perThreadListeners == null) listeners.put(parentMessageId, new ConcurrentLinkedQueue<>());
            listeners.get(parentMessageId).add(session);
            if(userListeningThreadsNeedToBeSetted) userListeningThreads.get(userId).add(sub.getParentMessageId());
        }
    }

    public static void notifyInThread(Long threadId){

        MessageDAO messageDAO = new MessageDAO();
        String threadPath = messageDAO.getByMessageId(threadId).getExtendedPath();
        String[] path = threadPath.split("\\.");
        HashSet<Session> ignoringSessions = new HashSet<>();
        for(int i = path.length-1; i>=0; i--) {

            long notyfingThread = Long.parseLong(path[i]);
            HashSet<Session> sessionsWithNoSubscribe = new HashSet<>();
            if (listeners.get(notyfingThread) != null)
                for (Session session : listeners.get(notyfingThread)) {
                    try {
                        if(!ignoringSessions.contains(session)){
                            if(session.isOpen()) {
                                if(uniqueWatchingThread.containsKey(session)){
                                    if(uniqueWatchingThread.get(session).equals(notyfingThread))
                                        if(sessionsWithNoSubscribe.contains(session))
                                            continue;
                                        else
                                            sessionsWithNoSubscribe.add(session);
                                }
                                session.getRemote().sendString(threadId.toString());
                                ignoringSessions.add(session);
                                System.out.println("NOTIFIED"+session.getRemote().getInetSocketAddress());
                            }else{
                                System.out.println("GET STRANGE SESSION ON "+session.getRemote().getInetSocketAddress());
                                listeners.get(notyfingThread).remove(session);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

            SubscriptionsDAO subscriptionsDAO = new SubscriptionsDAO();
            LinkedList<Subscription> subscriptions = subscriptionsDAO.getSubscriptionsToThread(notyfingThread);
            for(Subscription sub : subscriptions){
                if( !userIdToSessions.containsKey(sub.getUserId()) ){
                    LinkedList<Subscription> userSubscriptions = subscriptionsDAO.getUserSubscriptions(sub.getUserId());
                    Subscription subToCheck = new Subscription();
                    subToCheck.setUserId(sub.getUserId());
                    subToCheck.setParentMessageId(threadId);
                    if(!userSubscriptions.contains(subToCheck)){
                        Subscription userNotification = new Subscription();
                        userNotification.setUserId(sub.getUserId());
                        userNotification.setParentMessageId(threadId);
                        userNotification.setLastReadTime(0L);
                        userNotification.setNotification();
                        subscriptionsDAO.create(userNotification);
                    }
                }
            }
        }

    }
    public static void addUserListener(Long userId, Long newSub){
        for(Session session : userIdToSessions.get(userId)) {
            if (session == null) return;
            var listenersPerThread = listeners.get(newSub);
            if (listenersPerThread == null)
                listeners.put(newSub, new ConcurrentLinkedQueue<>());
            listeners.get(newSub).add(session);
            userListeningThreads.get(userId).add(newSub);
        }
    }
    public static void removeUserListener(Long userId, Long removingSub){
        for(Session session : userIdToSessions.get(userId)) {
            var listenersPerThread = listeners.get(removingSub);
            if (listenersPerThread == null) return;
            listenersPerThread.remove(session);
            if(listenersPerThread.isEmpty()) listeners.remove(removingSub);
            userListeningThreads.get(userId).remove(removingSub);
        }
    }

}
