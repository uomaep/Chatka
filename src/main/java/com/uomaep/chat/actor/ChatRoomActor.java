package com.uomaep.chat.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.uomaep.chat.constants.EventConstants.*;
import scala.concurrent.duration.Duration;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ChatRoomActor extends AbstractActor {
    private final LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);
    private final int roomId;
    private final String roomName;
    private final String envType;
    private final int recvTimeout;
    private final String redisIP;
    private final int redisPort;

    private final Set<ActorRef> users = new HashSet<>();
    private int user_count = 0;
    private boolean failover = true;

    public ChatRoomActor(int roomId, String envType) {
        this.roomId = roomId;
        roomName = setRoomName(envType);
        this.envType = envType;
        this.recvTimeout = getContext().getSystem().settings().config().getInt("akka.environment." + envType + ".chatroom-receive-timeout");
        this.redisIP = getContext().getSystem().settings().config().getString("akka.environment." + envType + ".redis-ip");
        this.redisPort = getContext().getSystem().settings().config().getInt("akka.environment." + envType + ".redis-port");
        getContext().setReceiveTimeout(Duration.create(recvTimeout, TimeUnit.SECONDS));
    }

    @Override
    public void preStart() throws Exception {
        logger.info("[{}] actor has created. {}", roomId, roomName);
//        subscribe(self, roomName, redisIP, redisPort);
    }

    @Override
    public void postStop() throws Exception {
        logger.info("[ChatRoomActor-{}] Stopped. {}", roomId, roomName);
    }

    @Override
    public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
        logger.info("[ChatRoomActor-{}] Restarting ... {}", roomId, roomName);
        preStart();
    }

    @Override
    public void postRestart(Throwable reason) throws Exception {
        logger.info("[ChatRoomActor-{}] Restarted. {}", roomId, roomName);
    }

    private String setRoomName(String envType) {
        String prefix = getContext().getSystem().settings().config().getString("akka.environment.pubsub-channel.prefix");
        String postfix = getContext().getSystem().settings().config().getString("akka.environment.pubsub-channel.postfix");
        switch (envType) {
            case "service":
            case "dev":
                return prefix + self().path().name() + postfix;
            default:
                return prefix + "unknown." + self().path().name() + postfix;
        }
    }

    private String convertMsg4User(String msg) {
        return msg.replaceAll("\\p{Cntrl}", "");
    }

    private void broadcast(String msg) {
        String data = convertMsg4User(msg);
        for(ActorRef user: users) {
            user.tell(new ChatMessage(msg), self());
        }
    }

    private void updateIncrRoomUser(boolean firstJoin, ActorRef user) {
        if(firstJoin) {
            users.add(user);
            context().watch(user);
        } else {
            user_count++;
        }
    }

    private void updateDecrRoomUser(boolean isJoin, ActorRef user) {
        if(isJoin) {
            user_count--;
        }

        users.remove(user);

        if(users.isEmpty()) destroyRoom();
    }

    private void destroyRoom() {
        failover = false;
        context().parent().tell(new RemoveChatRoom(roomId), self());
        self().tell(new UnsubscribeChannel(roomName), ActorRef.noSender());
    }

    @Override
    public Receive createReceive() {
        return null;
    }
}
