package org.messenger.data.entities;

import org.messenger.data.annotations.DataBaseField;
import org.messenger.data.annotations.DataBaseTable;
import org.messenger.data.interfaces.DBSerializable;

import java.sql.Timestamp;

@DataBaseTable(name = "subscriptions")
public class Subscription implements DBSerializable {
    @DataBaseField(isSequence = true, isPrimaryKey = true)
    long id;
    @DataBaseField
    long user_id;
    @DataBaseField
    long parentMessage_id;
    @DataBaseField
    Timestamp lastReadTime;
    @DataBaseField
    boolean isNotification = false;

    public long getParentMessageId() {
        return parentMessage_id;
    }

    public long getUserId() {
        return user_id;
    }

    public long getId() {
        return id;
    }

    public void setParentMessageId(long parentMessage_id) {
        this.parentMessage_id = parentMessage_id;
    }

    public void setUserId(long user_id) {
        this.user_id = user_id;
    }
    public void setLastReadTime(long time){
        lastReadTime = new Timestamp(time);
    }

    public Timestamp getLastReadTime() {
        return lastReadTime;
    }
    public boolean isNotification(){return isNotification;}
    public void setNotification(){isNotification = true;}

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Subscription other){
            return other.getUserId() == this.getUserId() && other.getParentMessageId() == this.getParentMessageId();
        }
        return false;
    }
}
