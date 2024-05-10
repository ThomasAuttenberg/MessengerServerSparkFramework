package org.messenger.data.entities;

import org.messenger.data.annotations.DataBaseField;
import org.messenger.data.annotations.DataBaseTable;
import org.messenger.data.interfaces.DBSerializable;

@DataBaseTable(name = "users")
public class User implements DBSerializable{

    @DataBaseField(isPrimaryKey = true, isSequence = true)
    private long user_id;
    @DataBaseField
    private String username;
    @DataBaseField
    private String password;

    private String token = null;

    public long getId(){return user_id;}
    public void setUsername(String username){this.username = username;}
    public void setPassword(String password){this.password = password;}
    public void setToken(String token){this.token = token;}
    public String getUserName(){return username;}
    public String getPassword(){return password;}
    public String getToken(){return token;}

    @Override
    public String toString() {
        return "username: "+username+" | password: "+password;
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof User otherUser)
            return username.equals(otherUser.username);
        else
            return false;
    }
}
