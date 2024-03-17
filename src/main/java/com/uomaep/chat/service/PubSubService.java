package com.uomaep.chat.service;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.uomaep.chat.config.Environment;
import com.uomaep.chat.constants.EventConstants.ChatMessageToUsers;
import com.uomaep.chat.constants.EventConstants.Close;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.HashMap;
import java.util.Map;

public class PubSubService extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private Jedis pubClient = null;
    private Jedis subClient = null;
    private final Map<String, InternalJedisPubSub> channelSubscribers = new HashMap<>();

    private class InternalJedisPubSub extends JedisPubSub {
        private ActorRef chatRoomActor;

        public InternalJedisPubSub(ActorRef chatRoomActor) {
            this.chatRoomActor = chatRoomActor;
        }

        @Override
        public void onMessage(String channel, String message) {
            chatRoomActor.tell(new ChatMessageToUsers(message), ActorRef.noSender());
        }

        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            log.info("{} PubSubService subscribed / subscribing to {}", channel, subscribedChannels);
        }

        @Override
        public void onUnsubscribe(String channel, int subscribedChannels) {
            log.info("{} PubSubService unsubscribed (remain: {}) & disconnected.", channel, subscribedChannels);
            disconnection(chatRoomActor);
        }
    }

    public void subscribe(ActorRef chatRoomActor, String channel, String ip, int port) {
        if(subClient == null) {
            subClient = new Jedis(ip, port);
        }
        InternalJedisPubSub subscriber = new InternalJedisPubSub(chatRoomActor);
        channelSubscribers.put(channel, subscriber);

        new Thread(() -> subClient.subscribe(subscriber, channel)).start();

    }

    public void unSubscribe(ActorRef chatRoomActor, String channel) {
        InternalJedisPubSub subscriber = channelSubscribers.get(channel);
        if(subscriber != null) {
            subscriber.unsubscribe(channel);
            channelSubscribers.remove(channel);
        }
    }

    public void publish(String channel, String message) {
        if(pubClient == null) {
            pubClient = new Jedis(Environment.getRedisIP(), Integer.parseInt(Environment.getRedisPort()));
        }
        pubClient.publish(channel, message);
    }

    public void disconnection(ActorRef chatRoomActor) {
        pubClient.disconnect();
        subClient.disconnect();
        chatRoomActor.tell(new Close(), ActorRef.noSender());
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder().build();
    };
}
