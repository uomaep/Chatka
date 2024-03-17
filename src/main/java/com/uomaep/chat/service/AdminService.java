package com.uomaep.chat.service;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.server.Directives;
import akka.http.javadsl.server.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminService extends WebService {
    private final Logger logger = LoggerFactory.getLogger("DEBUG");
    private final ActorSystem system;
    private final HealthyService healthyService;
    private ActorRef chatSupervisor;
    private final int servicePort = 8001; // 관리자 서버 포트 번호 설정

    public AdminService(ActorSystem system, HealthyService healthyService) {
        this.system = system;
        this.healthyService = healthyService;
    }

    public AdminService(ActorSystem system, HealthyService healthyService, ActorRef chatSupervisor) {
        this.system = system;
        this.healthyService = healthyService;
        this.chatSupervisor = chatSupervisor;
    }

    // HTTP 요청을 처리하기 위한 경로 설정
    public Route createRoute() {
        return Directives.concat(
                // "health" 경로 접두사로 시작하는 모든 요청 처리
                Directives.pathPrefix("health", () ->
                        Directives.concat(
                                // "up" 경로로 요청이 오면, healthyService를 시작하고 "200 OK"로 응답
                                Directives.path("up", () -> {
                                    healthyService.start();
                                    return httpRespJson("200 OK");
                                }),
                                // "down" 경로로 요청이 오면, healthyService를 중지하고 "200 OK"로 응답
                                Directives.path("down", () -> {
                                    healthyService.stop();
                                    return httpRespJson("200 OK");
                                }),
                                // "view" 경로로 요청이 오면, chatSupervisor 상태 확인 후 결과 응답
                                Directives.path("view", () -> {
                                    String result;
                                    if(chatSupervisor != null) {
                                        chatSupervisor.tell("akka://chatka/user/*", ActorRef.noSender());
                                        chatSupervisor.tell("akka://chatka/user/cs/*", ActorRef.noSender());
                                        result = "200 OK";
                                    } else {
                                        result = "ChatSupervisor ActorRef is NULL";
                                    }
                                    return httpRespJson(result);
                                })
                        ))
        );
    }

    public void setChatSupervisor(ActorRef chatSupervisor) {
        this.chatSupervisor = chatSupervisor;
    }

    // JSON 형식의 HTTP 응답 생성
    private Route httpRespJson(String body) {
        return Directives.complete(HttpEntities.create(ContentTypes.APPLICATION_JSON, body + "\r\n"));
    }

    // 서비스 시작 로직, HTTP 서버 바인딩
    @Override
    public void start() {
        System.out.println("Admin Server starting ...");
        Http.get(system).newServerAt("0.0.0.0", servicePort).bind(createRoute());
    }

    // 서비스 중지 로직, HTTP 서버 언바인딩
    @Override
    public void stop() {
        serviceUnbind(this.getClass().getSimpleName());
    }
}
