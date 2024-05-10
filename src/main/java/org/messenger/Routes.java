package org.messenger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.messenger.comminications.StaticReplies;
import org.messenger.comminications.NotificationConnection;
import org.messenger.data.entities.AuthToken;
import org.messenger.data.entities.Message;
import org.messenger.data.entities.Subscription;
import org.messenger.data.entities.User;
import org.messenger.data.implementations.AuthTokenDAO;
import org.messenger.data.implementations.MessageDAO;
import org.messenger.data.implementations.SubscriptionsDAO;
import org.messenger.data.implementations.UsersDAO;
import org.messenger.utills.AuthTokenGenerator;

import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static spark.Spark.*;

public class Routes {
    static void init(){

        webSocket("/listen", NotificationConnection.class);


        post("/authorization",(req,res)-> {
            JSONParser jsonParser = new JSONParser();
            System.out.println(req.ip());
            JSONObject request = (JSONObject) jsonParser.parse(req.body());
            String authType = (String) request.get("authType");
            JSONObject reply = new JSONObject();
            if (authType.equals("password")) {
                String username = (String) request.get("username");
                UsersDAO usersDAO = new UsersDAO();
                User user = usersDAO.getByName(username);
                String token = user == null ? null : AuthTokenGenerator.getToken(user, req.ip());
                String password = (String) request.get("password");

                if (user == null || !(user.getPassword().equals(password))) {
                    reply.put("status", "failure");
                    reply.put("desc", "Username or password is incorrect");
                    return reply;
                }

                reply.put("status", "OK");
                reply.put("desc", "Authorization!");
                if (token == null) token = AuthTokenGenerator.getNewToken(user, req.ip());
                reply.put("token", token);
                user.setToken(token);
                var session = req.session(true);
                session.attribute("token",token);
            }
            if (authType.equals("authToken")) {
                AuthTokenDAO authTokenDAO = new AuthTokenDAO();
                String receivedToken = (String) request.get("token");
                AuthToken authToken = authTokenDAO.getByToken(receivedToken);
                if (authToken != null && receivedToken.equals(authToken.getToken())) {
                    UsersDAO usersDAO = new UsersDAO();
                    User user = usersDAO.getById(authToken.getUserid());
                    reply = StaticReplies.get(StaticReplies.Reply.AUTHSUCCESS);
                    user.setToken(receivedToken);
                    req.session(true).attribute("token",receivedToken);
                } else {
                    reply = StaticReplies.get(StaticReplies.Reply.INVALID_TOKEN);
                }
            }
            return reply;
        });

        get("/getthread/:threadId",(req,res)->{
            Long threadId = Long.parseLong(req.params(":threadId"));
            JSONObject reply = new JSONObject();
            reply.put("status","OK");
            UsersDAO usersDAO = new UsersDAO();
            if(threadId != null){
                MessageDAO messageDAO = new MessageDAO();
                Message parentMessage = messageDAO.getByMessageId(threadId);
                if(parentMessage != null) {
                    JSONObject parentMessageJSON = messageToJSON(parentMessage);
                    reply.put("parentMessage", parentMessageJSON);
                    LinkedList<Message> messages = messageDAO.getByParentMessageIdPaginate(threadId, 0L, MessageDAO.PagingMode.nextMessages,999999999);
                    JSONArray jsonArray = new JSONArray();
                    for (Message message : messages) {
                        JSONObject messageJSON = messageToJSON(message);
                        jsonArray.add(messageJSON);
                    }
                    reply.put("messages", jsonArray);
                }
            }
            return reply;
        });
        get("getlastmessage/:threadId",(req,res)->{
            Long threadId = Long.parseLong(req.params("threadId"));
            MessageDAO messageDAO = new MessageDAO();
            LinkedList<Message> messages = messageDAO.getByParentMessageIdPaginate(threadId, 999999999999999L, MessageDAO.PagingMode.prevMessages,1);
            if(messages.isEmpty()) {
                messages = new LinkedList<>();
                Message firstMessage = messageDAO.getByMessageId(threadId);
                if(firstMessage != null)
                    messages.add(messageDAO.getByMessageId(threadId));
            }
            JSONObject reply;
            if(messages.isEmpty()){
                reply = new JSONObject();
                reply = StaticReplies.get(StaticReplies.Reply.THREAD_NOT_EXISTS);
            }else {
                Message message = messages.getFirst();
                reply = messageToJSON(message);
                reply.put("status", "OK");
            }
            return reply;
        });
        get("getfirstmessage/:threadId",(req,res)->{
            Long threadId = Long.parseLong(req.params("threadId"));
            MessageDAO messageDAO = new MessageDAO();
            LinkedList<Message> messages = messageDAO.getByParentMessageIdPaginate(threadId, 0, MessageDAO.PagingMode.nextMessages,1);
            if(messages.isEmpty()) {
                messages = new LinkedList<>();
                Message firstMessage = messageDAO.getByMessageId(threadId);
                if(firstMessage != null)
                    messages.add(messageDAO.getByMessageId(threadId));
            }
            JSONObject reply;
            if(messages.isEmpty()){
                reply = new JSONObject();
                reply = StaticReplies.get(StaticReplies.Reply.THREAD_NOT_EXISTS);
            }else {
                Message message = messages.getFirst();
                reply = messageToJSON(message);
                reply.put("status", "OK");
            }
            return reply;
        });

        get("getsubscriptions",(req,res)->{
            JSONObject reply = new JSONObject();
            if(req.headers("token") != null){
                reply.put("status","OK");
                SubscriptionsDAO subscriptionsDAO = new SubscriptionsDAO();
                MessageDAO messageDAO = new MessageDAO();
                AuthTokenDAO authTokenDAO = new AuthTokenDAO();
                AuthToken token = authTokenDAO.getByToken(req.headers("token"));
                LinkedList<Subscription> subscriptions = subscriptionsDAO.getUserSubscriptions(token.getUserid());
                JSONArray subscriptionsArray = new JSONArray();
                for(Subscription sub : subscriptions){
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("topic",sub.getParentMessageId());
                    jsonObject.put("lastReadTime",sub.getLastReadTime().getTime());
                    jsonObject.put("firstMessage",messageDAO.getByMessageId(sub.getParentMessageId()).getContent());
                    jsonObject.put("isNotification",sub.isNotification());
                    subscriptionsArray.add(jsonObject);
                }
                reply.put("subscriptions",subscriptionsArray);
            }else{
                reply = StaticReplies.get(StaticReplies.Reply.NOT_AUTHORIZED);
            }
            return reply;
        });

        post("/subscribe/:threadId", (req,res)-> {
            JSONObject reply = new JSONObject();
            if(req.headers("token") != null) {
                Long threadId = Long.parseLong(req.params("threadId"));
                SubscriptionsDAO subscriptionsDAO = new SubscriptionsDAO();
                AuthTokenDAO authTokenDAO = new AuthTokenDAO();
                AuthToken token = authTokenDAO.getByToken(req.session().attribute("token"));
                LinkedList<Subscription> subscriptions = subscriptionsDAO.getUserSubscriptions(token.getUserid());
                boolean exists = false;
                for (Subscription sub : subscriptions) {
                    if (sub.getParentMessageId() == threadId) {
                        reply = StaticReplies.get(StaticReplies.Reply.ALREADY_SUBSCRIBED);
                        exists = true;
                    }
                }
                if (!exists) {
                    if (threadId == null) {
                        reply = StaticReplies.get(StaticReplies.Reply.THREAD_NOT_EXISTS);
                    } else {
                        MessageDAO messageDAO = new MessageDAO();
                        if(messageDAO.getByMessageId(threadId) == null){
                            reply = StaticReplies.get(StaticReplies.Reply.THREAD_NOT_EXISTS);
                        }else {
                            Subscription subscription = new Subscription();
                            subscription.setLastReadTime(new Date().getTime());
                            subscription.setParentMessageId(threadId);
                            Long userId = token.getUserid();
                            subscription.setUserId(userId);
                            subscriptionsDAO.create(subscription);
                            reply = StaticReplies.get(StaticReplies.Reply.SUBSCRIBTION_SUCCESS);
                            //TODO: Notification manager
                            NotificationConnection.addUserListener(userId,threadId);
                            //NotificationManager.updateSubscriptions(connection.getUser());
                        }
                    }
                }

            }else{
                reply = StaticReplies.get(StaticReplies.Reply.NOT_AUTHORIZED);
            }
            return reply;
        });

        post("/unsubscribe/:threadId",(req,res)->{
            JSONObject reply = new JSONObject();
            if(req.headers("token") != null) {
                Long threadId;
                try {
                    threadId = Long.parseLong(req.params("threadId"));
                }catch (NumberFormatException ex){
                    reply.put("status","failure");
                    reply.put("desc","can't parse threadId from request");
                    return reply;
                }
                AuthTokenDAO authTokenDAO = new AuthTokenDAO();
                AuthToken token = authTokenDAO.getByToken(req.session().attribute("token"));
                Long userId = token.getUserid();
                SubscriptionsDAO subscriptionsDAO = new SubscriptionsDAO();
                LinkedList<Subscription> subscriptions = subscriptionsDAO.getUserSubscriptions(userId);
                boolean exists = false;
                for (Subscription sub : subscriptions) {
                    if (sub.getParentMessageId() == threadId) {
                        subscriptionsDAO.delete(sub);
                        reply.put("status", "OK");
                        reply.put("desc", "unsubscribed of" + threadId);
                        exists = true;
                        //TODO: Notification manager
                        NotificationConnection.removeUserListener(userId,threadId);
                        //NotificationManager.updateSubscriptions(connection.getUser());
                    }
                }
                if (!exists) {
                    reply.put("status", "failure");
                    reply.put("desc", "no such subscription");
                } else {
                    reply = StaticReplies.get(StaticReplies.Reply.THREAD_NOT_EXISTS);
                }
            }else{
                reply = StaticReplies.get(StaticReplies.Reply.NOT_AUTHORIZED);
            }
            return reply;
        });

        post("/sendmessage/:threadId", (req,res)->{
            JSONParser jsonParser = new JSONParser();
            System.out.println(req.ip());
            JSONObject dataPacket = (JSONObject) jsonParser.parse(req.body());
            JSONObject reply = new JSONObject();
            if(req.headers("token") != null) {
                Long threadId = Long.parseLong(req.params("threadId"));
                System.out.println(threadId);
                MessageDAO messageDAO = new MessageDAO();
                Message threadMessage = messageDAO.getByMessageId(threadId);
                if(threadMessage != null){
                    Message message = new Message();
                    AuthTokenDAO authTokenDAO = new AuthTokenDAO();
                    AuthToken token = authTokenDAO.getByToken(req.headers("token"));
                    message.setAuthorId(token.getUserid());
                    message.setContent((String)dataPacket.get("content"));
                    message.setDatetime(new Timestamp(new Date().getTime()));
                    message.setParentMessageId(threadId);
                    messageDAO.create(message);
                    //JSONObject notification = new JSONObject();
                    //notification.put("threadId",threadId);
                    //notification.put("firstMessage",messageDAO.getByParentMessageIdPaginate(threadId, 0L, MessageDAO.PagingMode.nextMessages,1).getFirst().getContent());
                    //Todo: notification!!!
                    NotificationConnection.notifyInThread(threadId);
                    //NotificationManager.notifyInThread(threadId,notification);
                    reply = StaticReplies.get(StaticReplies.Reply.MESSAGE_SENT);
                }else{
                    reply = StaticReplies.get(StaticReplies.Reply.THREAD_NOT_EXISTS);
                }
            }else{
                reply = StaticReplies.get(StaticReplies.Reply.NOT_AUTHORIZED);
            }
            return reply;
        });
        post("/read/:threadId",(req,res)->{
            JSONObject reply = new JSONObject();
            AuthTokenDAO authTokenDAO = new AuthTokenDAO();
            AuthToken token = authTokenDAO.getByToken(req.headers("token"));
            if(token != null){
                Long threadId = null;
                try {
                    threadId = Long.parseLong(req.params("threadId"));
                }catch (NumberFormatException ex){

                }
                if(threadId != null) {
                    SubscriptionsDAO subscriptionsDAO = new SubscriptionsDAO();
                    LinkedList<Subscription> subscriptions = subscriptionsDAO.getUserSubscriptions(token.getUserid());
                    boolean beenRead = false;
                    for (Subscription sub : subscriptions) {
                        if (sub.getParentMessageId() == threadId) {
                            if(sub.isNotification()) {
                                subscriptionsDAO.delete(sub);
                            }else {
                                sub.setLastReadTime(new Date().getTime());
                                subscriptionsDAO.update(sub);
                            }
                            beenRead = true;
                            break;
                        }
                    }
                    if (beenRead) {
                        reply.put("status", "OK");
                        reply.put("desc", "Topic has been read");
                    } else {
                        reply.put("status", "failure");
                        reply.put("desc", "No such topic in subscriptions");
                    }
                }else{
                    reply = StaticReplies.get(StaticReplies.Reply.THREAD_NOT_EXISTS);
                }
            }else{
                reply = StaticReplies.get(StaticReplies.Reply.NOT_AUTHORIZED);
            }
            return reply;
        });

        get("/token",(req,res)->{
            System.out.println("meow");
           return req.headers("token");
        });

        get("/{path}",(req,res) -> {
            if(req.params().equals("listen")) return null;
            System.out.println(StaticReplies.get(StaticReplies.Reply.INCORRECT_REQUEST));
            return StaticReplies.get(StaticReplies.Reply.INCORRECT_REQUEST).toJSONString();

        });


    }

    private static JSONObject messageToJSON(Message message){
        UsersDAO usersDAO = new UsersDAO();
        JSONObject messageJSON = new JSONObject();
        messageJSON.put("id",message.getMessageId());
        Long parentMessageId = message.getParentMessageId();
        messageJSON.put("parentMessage",parentMessageId == null ? -1L : parentMessageId);
        messageJSON.put("author", usersDAO.getById(message.getAuthorId()).getUserName());
        messageJSON.put("content",message.getContent());
        messageJSON.put("dateTime",message.getDatetime().getTime());
        messageJSON.put("quotes",message.getQuotes());
        return messageJSON;
    }
}
