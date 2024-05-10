package org.messenger.comminications;

import org.json.simple.JSONObject;

import java.util.HashMap;

public class StaticReplies {

    private final static JSONObject notAuthorized;
    private final static JSONObject incorrectRequest;
    private final static JSONObject invalidToken;
    private final static JSONObject alreadySubscribed;
    private final static JSONObject threadNotExists;
    private final static JSONObject subscriptionSuccess;
    private final static JSONObject messageSent;
    private final static JSONObject authSuccess;

    static HashMap<Reply, JSONObject> replies = new HashMap<>();

    public enum Reply{
        INCORRECT_REQUEST,

        NOT_AUTHORIZED,
        INVALID_TOKEN,
        AUTHSUCCESS,

        ALREADY_SUBSCRIBED,
        THREAD_NOT_EXISTS,
        SUBSCRIBTION_SUCCESS,
        MESSAGE_SENT
    }

    public static JSONObject get(Reply reply){
        return replies.get(reply);
    }

    static{
        // Auth success
        authSuccess = new JSONObject();
        authSuccess.put("status","OK");
        authSuccess.put("desc","successful");
        // Not authorized
        notAuthorized = new JSONObject();
        notAuthorized.put("status","failure");
        notAuthorized.put("desc","not authorized");
        ////////////////////////////////////////////////////
        // INCORRECT REQUEST
        incorrectRequest = new JSONObject();
        incorrectRequest.put("status","failure");
        incorrectRequest.put("desc","no such endpoint");
        // INVALID TOKEN
        invalidToken = new JSONObject();
        invalidToken.put("status","failure");
        invalidToken.put("desc","invalid token");
        ////////////////////////////////////////////////////
        //ALREADY SUBSCRIBED
        alreadySubscribed = new JSONObject();
        alreadySubscribed.put("status","failure");
        alreadySubscribed.put("desc","already subscribed");
        //THREAD_NOT_EXISTS
        threadNotExists = new JSONObject();
        threadNotExists.put("status","failure");
        threadNotExists.put("desc","thread doesn't exists");
        //SUBSCRIPTION_SUCCESS
        subscriptionSuccess = new JSONObject();
        subscriptionSuccess.put("status","OK");
        subscriptionSuccess.put("desc","subscription has been created");
        //MESSAGE_SENT
        messageSent = new JSONObject();
        messageSent.put("status","OK");
        messageSent.put("desc","message sent successfully");



        replies.put(Reply.INCORRECT_REQUEST, incorrectRequest);
        replies.put(Reply.NOT_AUTHORIZED,notAuthorized);
        replies.put(Reply.INVALID_TOKEN,invalidToken);
        replies.put(Reply.ALREADY_SUBSCRIBED,alreadySubscribed);
        replies.put(Reply.THREAD_NOT_EXISTS,threadNotExists);
        replies.put(Reply.SUBSCRIBTION_SUCCESS,subscriptionSuccess);
        replies.put(Reply.MESSAGE_SENT,messageSent);
        replies.put(Reply.AUTHSUCCESS,authSuccess);

    }

}
