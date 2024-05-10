package org.messenger.data.implementations;

import org.messenger.data.DAO;
import org.messenger.data.entities.Subscription;

import java.sql.SQLException;
import java.util.LinkedList;

public class SubscriptionsDAO extends DAO<Subscription> {

    public SubscriptionsDAO() {
        super(Subscription.class);
    }

    public LinkedList<Subscription> getUserSubscriptions(long userId){
        try {
            return super.getByField(Subscription.class.getDeclaredField("user_id"),userId);
        } catch (IllegalAccessException | SQLException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public LinkedList<Subscription> getSubscriptionsToThread(long threadId){
        try {
            return super.getByField(Subscription.class.getDeclaredField("parentMessage_id"),threadId);
        } catch (IllegalAccessException | SQLException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void create(Subscription entity){
        try {
            super.create(entity);
        } catch (IllegalAccessException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(Subscription entity){
        try {
            super.update(entity);
        }catch (IllegalAccessException | SQLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(Subscription entity){
        try {
            super.delete(entity);
        } catch (SQLException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
