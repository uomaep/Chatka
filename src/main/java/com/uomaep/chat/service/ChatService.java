package com.uomaep.chat.service;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.RemoteAddress;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializer$;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.uomaep.chat.actor.ChatSupervisor;
import com.uomaep.chat.actor.UserActor;
import com.uomaep.chat.config.Environment;
import com.uomaep.chat.constants.EventConstants.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.HttpClient;

import static akka.http.javadsl.server.Directives.*;
import static akka.http.javadsl.server.PathMatchers.segment;

public class ChatService extends WebService {
  private final int servicePort = 8000;
  private final ActorSystem system;
  private final AdminService adminService;
  private ActorRef chatSupervisor;
  private HttpClient httpClient;
  private final String env;

  public Route createServiceRoute() {
    return path(segment(), chatRoomIDStr -> {
      int chatRoomID;
      try {
        chatRoomID = Integer.parseInt(chatRoomIDStr);
      } catch (NumberFormatException e) {
        return reject();
      }

      return extractClientIP(ip -> {
        return handleWebSocketMessages(newUser(chatRoomID, ip));
      });
    });
  }

  public ChatService(String env, ActorSystem system, AdminService adminService) {
    this.system = system;
    this.adminService = adminService;
    this.env = env;
  }

  public void registerNode(int port) {
    try {
      InetAddress localHost = InetAddress.getLocalHost();
      String hostAddress = localHost.getHostAddress();

      Environment.setHostName(hostAddress);
      Environment.setPort(port);

      logger.info("Server IP Address of System => {}", hostAddress);
    } catch (UnknownHostException e) {
      logger.error("Error obtaining local host address", e);
    }
  }

  // 클라이언트로부터 오는 메시지를 처리하기 위한 Sink를 정의하는 메서드
  public Sink<Message, NotUsed> inMessage(ActorRef userActor) {
    // return Flow.<Message>create().map(message -> {
    // if (message instanceof TextMessage textMessage) {
    // if (textMessage.isStrict()) {
    // logger.info("Chat server에 메시지가 들어옴: {}", textMessage.getStrictText());

    // }
    // }
    // }).to(Sink.actorRef(userActor, PoisonPill.getInstance()));
    //
    return Flow.<Message>create()
        .map(message -> {
          if (message instanceof TextMessage text) {
            if (message.isStrict()) {
              logger.info("Chat server에 메시지가 들어옴: {}", text.getStrictText());
              return new InMessage(text.getStrictText());
            }
          }
          logger.info("Chat server에 들어온 메시지가 Strict Text message가 아님: {}", message);
          return null;
        }).filter(obj -> obj != null)
            .to(Sink.actorRef(userActor, PoisonPill.getInstance()));
  }

  // 서버로부터 클라이언트로 메시지를 보내기 위한 Source를 정의하는 메서드
  public Source<Message, NotUsed> outMessage(ActorRef userActor) {
    return Source.<OutMessage>actorRef(
        256,
        OverflowStrategy.dropHead()).mapMaterializedValue(outActor -> {
          userActor.tell(new Connected(outActor), ActorRef.noSender());
          return NotUsed.getInstance();
        }).map(outMsg -> {
          logger.info("클라이언트로 메시지 보냄: {}", outMsg.text);
          return TextMessage.create(outMsg.text);
        });
  }

  // 새로운 사용자에 대한 웹소켓 연결을 처리하는 Flow를 생성하는 메서드
  public Flow<Message, Message, NotUsed> newUser(int chatRoomID, RemoteAddress ip) {
    System.out.println("new user");
    ActorRef userActor = system.actorOf(Props.create(UserActor.class, chatRoomID, chatSupervisor, httpClient, ip));

    // 사용자 액터로부터 오는 메시지를 처리하는 Sink
    Sink<Message, NotUsed> inMessage = inMessage(userActor);

    // 사용자 액터로 메시지를 보내는 Source
    Source<Message, NotUsed> outMessage = outMessage(userActor);

    // Sink와 Source를 결합하여 양방향 통신을 위한 Flow를 생성
    return Flow.fromSinkAndSource(inMessage, outMessage);
  }

  @Override
  public void start() {
    System.out.println("Chat Server Starting ...");
    Environment.setEnvType(env);

    chatSupervisor = system.actorOf(Props.create(ChatSupervisor.class, system), "cs");
    adminService.setChatSupervisor(chatSupervisor);

    registerNode(servicePort);
    Http.get(system).newServerAt("localhost", servicePort).bind(createServiceRoute());

    Environment.loadRedisConfig(system);
  }

  @Override
  public void stop() {
    serviceUnbind(this.getClass().getSimpleName());
  }
}
