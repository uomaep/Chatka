package com.uomaep.chat.constants;

import akka.actor.ActorRef;
import akka.actor.Props;

public class EventConstants {

    // ChatSupervisor
    public static class CreateChatRoom {
        public final int roomId;
        public CreateChatRoom(int roomId) {this.roomId = roomId;}
    }

    public static class RemoveChatRoom {
        public final int roomId;
        public RemoveChatRoom(int roomId) {this.roomId = roomId;}
    }

    public static class RegisterUser {
        public final int roomId;
        public final ActorRef userActor;
        public RegisterUser(int roomId, ActorRef userActor) {
            this.roomId = roomId;
            this.userActor = userActor;
        }
    }

    public static class JoinRoom {
        public final ActorRef chatRoomActor;
        public JoinRoom(ActorRef chatRoomActor) {this.chatRoomActor = chatRoomActor;}
    }

    public static class RegProps {
        public final Props props;
        public final String name;
        public RegProps(Props props, String name) {
            this.props = props;
            this.name = name;
        }
    }

    // ChatRoomActor
    public static class ChatMessage {
        public final String message;
        public ChatMessage(String message){this.message = message;}
    }

    public static class UnsubscribeChannel {
        public final String channel;
        public UnsubscribeChannel(String channel) {this.channel = channel;}
    }

}
