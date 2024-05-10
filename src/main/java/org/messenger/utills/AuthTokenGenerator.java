package org.messenger.utills;

import org.messenger.data.entities.AuthToken;
import org.messenger.data.entities.User;
import org.messenger.data.implementations.AuthTokenDAO;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

public class AuthTokenGenerator {
    public static String getToken(User user, String ip) {
        Date date = new Date();
        AuthTokenDAO authTokenDAO = new AuthTokenDAO();
        AuthToken authToken = authTokenDAO.getByIP(user, ip);

        if (authToken != null) {
            if (authToken.getExpireTime().getTime() < date.getTime()) {
                    authTokenDAO.delete(authToken);
                    return null;
            } else {
                return authToken.getToken();
            }
        } else {
            return null;
        }
    }
    public static String getNewToken(User user, String ip){
        try{
            Date date = new Date();
            AuthTokenDAO authTokenDAO = new AuthTokenDAO();
            AuthToken authToken ;
            String data = user.getId() + user.getUserName() + user.getPassword() + date.getTime();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for(byte b : hash){
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DAY_OF_MONTH,30);
            authToken = new AuthToken();
            authToken.setIp(ip);
            authToken.setUserid(user.getId());
            authToken.setExpireTime(new Timestamp(calendar.getTime().getTime()));
            authToken.setToken(hexString.toString());
            authTokenDAO.create(authToken);

        return hexString.toString();
    } catch (
    NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
    }
    }

}
