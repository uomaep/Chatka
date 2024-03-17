package com.uomaep.chat.actor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.model.RemoteAddress;
import com.uomaep.chat.ChatRooms;
import com.uomaep.chat.constants.EventConstants.*;

import java.time.Duration;

public class UserActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final int chatRoomID;
    private final ActorRef chatSupervisor;
    private final ActorRef httpClientActor;
    private final RemoteAddress ip;
    private ActorRef chatRoom = null;
    private int joinRetries = 0;

    public static Props props(int chatRoomID, ActorRef chatSuper, ActorRef httpClientActor, RemoteAddress ip) {
        return Props.create(UserActor.class, () -> new UserActor(chatRoomID, chatSuper, httpClientActor, ip));
    }

    public UserActor(int chatRoomID, ActorRef chatSupervisor, ActorRef httpClientActor, RemoteAddress ip) {
        this.chatRoomID = chatRoomID;
        this.chatSupervisor = chatSupervisor;
        this.httpClientActor = httpClientActor;
        this.ip = ip;
    }

    @Override
    public void preStart() {
        synchronized (this) {
            chatRoom = ChatRooms.chatRooms.getOrDefault(chatRoomID, null);
        }
        if (chatRoom == null) {
            chatSupervisor.tell(new RegisterUser(chatRoomID, self()), self());
            log.info("[#{}] gets a ChatRoomActorRef for UserActor", chatRoomID);
        }
    }

    @Override
    public void preRestart(Throwable reason, scala.Option<Object> message) throws Exception {
        super.preRestart(reason, message);
        preStart();
    }

    @Override
    public void postRestart(Throwable reason) throws Exception {
        super.postRestart(reason);
        log.info("[-{}] User actor has restarted.", chatRoomID);
    }

    @Override
    public void postStop() throws Exception {
        if (chatRoom != null) {
            chatRoom.tell(new Leave(), self());
        } else {
            log.info("[#{}] UserActor couldn't get chatroom!", chatRoomID);
        }

        log.info("[#{}] UserActor({}) is stopped!", chatRoomID, null);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ReceiveTimeout.class, timeout -> {
                    log.info("I've been idle for over 3 hours. Terminating myself");
                    getContext().stop(self());
                })
                .match(JoinRoom.class, joinRoom -> {
                    chatRoom = joinRoom.chatRoomActor;
                    log.info("[#{}] Set ChatRoom : {}", chatRoomID, chatRoomID);
                })
                .match(Connected.class, connected -> {
                    if (chatRoom == null) {
                        if (joinRetries < 1000) {
                            joinRetries++;
                            self().tell(new Connected(connected.outgoing), self()); // Retry
                        } else {
                            log.debug("[#{}] It looks like chatRoom is null ... just drinking poison myself.", chatRoomID);
                            self().tell(PoisonPill.getInstance(), self());
                        }
                    } else {
                        getContext().become(connected(connected.outgoing));
                        log.debug("[#{}] Become->State: User Connected", chatRoomID);
                    }
                })
                .match(InMessage.class, message -> {
                    getContext().system().scheduler().scheduleOnce(Duration.ofMillis(1), () -> {
                        self().tell(new InMessage(message.text), self());
                    }, getContext().getSystem().dispatcher());
                })
                .match(Left.class, left -> {
                    getContext().become(leftHandle(left.outgoing));
                })
                .build();
    }

    private Receive connected(ActorRef outgoing) {
        if(chatRoom == null) {
            log.debug("User Actor ChatRoom Actor is null");
        }
        chatRoom.tell(new Join(), self());
        log.debug("[-{}] Join at ChatRoom({})", chatRoomID, chatRoomID);

        return receiveBuilder()
                .match(ReceiveTimeout.class, timeout -> {
                    context().stop(self());
                })
                .match(InMessage.class, msg -> {
                    if(chatRoom == null) {
                        log.error("User Actor -> InMessage -> ChatRoom actor is null");
                    } else {
                        chatRoom.tell(new ChatMessage(msg.text), self());
                    }
                })
                .match(ChatMessage.class, msg -> {
                    outgoing.tell(new OutMessage(msg.message), self());
                })
                .build();
    }

    private Receive leftHandle(ActorRef outgoing) {
        chatRoom.tell(new Leave(), self());
        log.info("[-{}] leaves from ChatRoom({})", chatRoomID, chatRoomID);

        return receiveBuilder()
                .matchAny(o -> {
                    chatRoom.tell(new ChatMessage("User has left."), self());
                }).build();
    }

}
