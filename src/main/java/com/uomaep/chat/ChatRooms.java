package com.uomaep.chat;

import akka.actor.ActorRef;

import java.util.HashMap;
import java.util.Map;

public class ChatRooms {
    public static Map<Integer, ActorRef> chatRooms = new HashMap<>();
    public static ActorRef httpClient = null;
}
