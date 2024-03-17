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

    public static class Join {}
    public static class Leave {}
    public static class Close {}

    public static class ChatMessageToUsers {
        public final String message;
        public ChatMessageToUsers(String message) { this.message = message;}
    }

    public static class UnsubscribeMessage {
        public final String channel;
        public UnsubscribeMessage(String channel) {this.channel = channel;}
    }

    public static class Terminated {
        public final ActorRef user;
        public Terminated(ActorRef user) {this.user = user;}
    }

    // UserActor
    public static class Connected {
        public final ActorRef outgoing;

        public Connected(ActorRef outgoing) {
            this.outgoing = outgoing;
        }
    }

    public static class InMessage {
        public final String text;
        public InMessage(String text) {this.text = text;}
    }

    public static class OutMessage {
        public final String text;
        public OutMessage(String text) {this.text = text;}
    }

    public static class CustomResponse {
        public final String event;
        public final int rcode;
        public final String data;

        public CustomResponse(String event, int rcode, String data) {
            this.event = event;
            this.rcode = rcode;
            this.data = data;
        }
    }

    public static class Left {
        public final ActorRef outgoing;

        public Left(ActorRef outgoing) {this.outgoing = outgoing;}
    }
}
