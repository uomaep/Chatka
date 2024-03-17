package com.uomaep.chat.service;

import akka.actor.ActorSystem;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;

public class HealthyService extends WebService {
    private final int servicePort = 8002;
    private final ActorSystem system;

    public HealthyService(ActorSystem system) {
        this.system = system;
    }

    // createRoute 메서드: HTTP 요청을 처리하기 위한 경로(route)를 정의하고 이를 Flow로 변환하여 반환
    private Route createRoute() {
        // Route를 정의. 단일 경로 "/"에 대해 간단한 웰컴 메시지를 응답
        return new AllDirectives() {
            public Route createRoute() {
                return concat(
                        pathEndOrSingleSlash(() ->
                                complete("Welcome to Chatka") // 루트 경로("/")에 접근했을 때 "Welcome to Chatka" 메시지로 응답
                        )
                );
            }
        }.createRoute();
    }

    @Override
    public void start() {
        // 서비스 시작 로직
        System.out.println("Healthy Service starting ...");
        // HTTP 서버를 시작하고, createRoute()에서 생성된 Flow를 사용하여 요청을 처리
        serviceBind(this.getClass().getSimpleName(), createRoute().flow(system), servicePort, system);
    }

    @Override
    public void stop() {
        // 서비스 정지 로직
        serviceUnbind(this.getClass().getSimpleName());
    }
}
