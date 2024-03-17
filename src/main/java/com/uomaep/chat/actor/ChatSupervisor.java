package com.uomaep.chat.actor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.uomaep.chat.ChatRooms;
import com.uomaep.chat.config.Environment;
import com.uomaep.chat.constants.EventConstants.*;
import scala.concurrent.ExecutionContextExecutor;

import java.time.Duration;
import java.util.Optional;

import static akka.actor.SupervisorStrategy.resume;

public class ChatSupervisor extends AbstractActor {
    private final LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);
    private ActorSystem system;
    private ExecutionContextExecutor executionContext;

    public ChatSupervisor(ActorSystem system) {
        this.system = system;
        this.executionContext = system.dispatcher();
    }

    @Override
    public void preStart() throws Exception {
        ChatRooms.httpClient = getContext().actorOf(Props.create(HttpClient.class, system), "httpClient");
        logger.info("ChatSupervisor Starting ...");
    }

    @Override
    public void postStop() throws Exception {
        logger.info("ChatSupervisor Down!");
    }

    @Override
    public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
        logger.info("ChatSupervisor Restarting ...");
        logger.info(reason.toString());
        preStart();
    }

    @Override
    public void postRestart(Throwable reason) throws Exception {
        logger.info("ChatSupervisor Restarted.");
        logger.info(reason.toString());
    }

    private static SupervisorStrategy strategy =
            new OneForOneStrategy(10, Duration.ofMinutes(1),
                    t -> {
                        if(t instanceof Exception) {
                            return resume();
                        } else {
                            return SupervisorStrategy.escalate();
                        }
                    });

    public ActorRef getChatRoomActorRef(int number) {
        // ChatRoom Actor를 생성하거나 찾아서 리턴
        // `synchronized` 블록은 `this` 객체에 대한 동기화를 수행하며, 한 번에 하나의 스레드만 해당 블록을 실행할 수 있음
        // 따라서 여러 스레드가 공유하는 데이터에 대한 안전한 접근을 보장
        // 여러 스레드가 `ChatRooms.chatRooms` Map을 업데이트하거나 읽는 것을 방지하여 안전한 액세스가 보장
        synchronized (this) {
            return ChatRooms.chatRooms.getOrDefault(number, createNewChatRoom(number));
        }
    }

    public ActorRef createNewChatRoom(int number) {
        ActorRef chatRoom = null;
        try {
            // 새로운 ChatRoom Actor를 생성하고 리턴
            chatRoom = context().actorOf(Props.create(ChatRoomActor.class, number, Environment.getEnvType()), "chatRoomActor" + number);
            ChatRooms.chatRooms.put(number, chatRoom);
        } catch (Exception e) {
            logger.info("FIXME: Create new chat room({}) => {}", number, e);
        }

        return chatRoom;
    }

    public void removeChatRoom(int number) {
        synchronized (this) {
            ChatRooms.chatRooms.remove(number);
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CreateChatRoom.class, msg -> {
                    getChatRoomActorRef(msg.roomId);
                })
                .match(RemoveChatRoom.class, msg -> {
                    removeChatRoom(msg.roomId);
                })
                .match(RegisterUser.class, msg -> {
                    msg.userActor.tell(new JoinRoom(getChatRoomActorRef(msg.roomId)), getSelf());
                })
                .match(RegProps.class, msg -> {
                    context().actorOf(msg.props, msg.name);
                })
                .build();
    }
}

