Messenger with a tree-like messages relation stucture (message can be reply for other message)
<br>Client: https://github.com/ThomasAuttenberg/MessengerClient

DataBase:
<pre>
CREATE TABLE Users (
    user_id bigserial PRIMARY KEY,
    username varchar(50) UNIQUE NOT NULL,
    password varchar(50) NOT NULL
);
CREATE TABLE AuthTokens(
    user_id bigint REFERENCES Users(user_id) ON DELETE CASCADE NOT NULL,
    token varchar(64) PRIMARY KEY NOT NULL,
    ip varchar(20) NOT NULL,
    expireTime timestamp NOT NULL
);
CREATE TABLE Messages (
    message_id bigserial PRIMARY KEY,
    parentMessage_id bigint,
    content TEXT NOT NULL,
    datetime timestamp default now(),
    author_id bigint NOT NULL,
    quotes int NOT NULL DEFAULT 0,
    extendedPath text DEFAULT '',
    FOREIGN KEY (parentMessage_id) REFERENCES Messages(message_id) ON DELETE SET NULL,
    FOREIGN KEY (author_id) REFERENCES Users(user_id) ON DELETE CASCADE
);
CREATE TABLE Subscriptions(
    id bigserial PRIMARY KEY,
    user_id bigint REFERENCES Users(user_id) ON DELETE CASCADE NOT NULL,
    parentMessage_id bigint REFERENCES Messages(message_id) ON DELETE CASCADE NOT NULL,
    lastReadTime Timestamp,
    isNotification boolean DEFAULT false
);
CREATE FUNCTION messageDeletingHandler() RETURNS TRIGGER AS '
BEGIN
    WITH parent_row as
        (SELECT message_id, quotes FROM Messages
        WHERE message_id = OLD.parentMessage_id)
    UPDATE Messages SET quotes =
    CASE
        WHEN parent_row IS NOT NULL THEN
        parent_row.quotes-1
        ELSE parent_row.quotes
    END
    FROM parent_row WHERE Messages.message_id = parent_row.message_id;
    DELETE FROM Messages WHERE parentMessage_id = OLD.message_id;
    RETURN OLD;
END' LANGUAGE plpgsql;
CREATE TRIGGER messageDeletingTrigger AFTER DELETE ON Messages FOR EACH ROW EXECUTE FUNCTION messageDeletingHandler();
CREATE OR REPLACE FUNCTION messageCreatingHandler() RETURNS TRIGGER AS '
DECLARE
    parentQuotes integer;
    parentMessageId bigint;
    parentExtendedPath text;
BEGIN
    SELECT quotes,message_id,extendedPath INTO parentQuotes,parentMessageId,parentExtendedPath FROM Messages WHERE message_id = NEW.parentMessage_id;
    UPDATE Messages
        SET quotes = parentQuotes+1
    WHERE Messages.message_id = parentMessageId;
    IF parentMessageId IS NULL THEN
        UPDATE Messages SET extendedPath = CAST(NEW.message_id AS VARCHAR) WHERE message_id = NEW.message_id;
    ELSE
        UPDATE Messages SET extendedPath = parentExtendedPath || ''.'' || CAST(NEW.message_id AS VARCHAR) WHERE message_id = NEW.message_id;
    end if;
    RETURN NEW;
END;' LANGUAGE plpgsql;
CREATE TRIGGER messagesCreatingTrigger AFTER INSERT ON Messages FOR EACH ROW EXECUTE FUNCTION messageCreatingHandler();
</pre>
