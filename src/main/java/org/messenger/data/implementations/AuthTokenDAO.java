package org.messenger.data.implementations;

import org.messenger.data.DAO;
import org.messenger.data.entities.AuthToken;
import org.messenger.data.entities.User;

import java.sql.SQLException;
import java.util.LinkedList;

public class AuthTokenDAO extends DAO<AuthToken> {
    public AuthTokenDAO() {
        super(AuthToken.class);
    }

    public AuthToken getByIP(User user, String ip) {
        try {
            LinkedList<AuthToken> tokens = super.getByField(AuthToken.class.getDeclaredField("user_id"), user.getId());
            AuthToken token = null;
            for (AuthToken token_ : tokens) {
                if (token_.getIp().equals(ip)) {
                    token = token_;
                }
            }
            return token;
        } catch (IllegalAccessException | SQLException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    public AuthToken getByToken(String token){
        try {
            LinkedList<AuthToken> tokens = super.getByField(AuthToken.class.getDeclaredField("token"), token);
            return tokens.isEmpty()? null : tokens.getFirst();
        } catch (IllegalAccessException | SQLException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void create(AuthToken token) {
        try {
            super.create(token);
        } catch (IllegalAccessException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(AuthToken entity) {
        try {
            super.update(entity);
        } catch (IllegalAccessException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(AuthToken entity){
        try {
            super.delete(entity);
        } catch (SQLException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
