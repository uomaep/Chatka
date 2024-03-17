package com.uomaep.chat.service;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.javadsl.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

public abstract class WebService {
    protected final Logger logger = LoggerFactory.getLogger("DEBUG");

    // 서버 바인딩 상태를 관리하기 위함
    protected CompletionStage<ServerBinding> bindingFuture = null;

    /**
     * 서비스를 시작하고 HTTP 요청을 처리하기 위해 서버를 바인딩하는 메서드.
     * @param serviceName 서비스 이름. 로깅에 사용됩니다.
     * @param bindRoute HTTP 요청과 응답을 처리하는 Flow입니다.
     * @param bindPort 서버가 바인딩될 포트 번호.
     * @param system ActorSystem. Akka 인프라의 중심입니다.
     */
    public void serviceBind(String serviceName, Flow<HttpRequest, HttpResponse, ?> bindRoute, int bindPort, ActorSystem system) {
        // 서버를 "0.0.0.0" 주소와 지정된 포트에 바인딩. bindRoute를 사용하여 HTTP 요청 처리
        bindingFuture = Http.get(system)
                .newServerAt("0.0.0.0", bindPort)
                .bindFlow(bindRoute);

        // 비동기적으로 바인딩의 성공 여부를 처리
        bindingFuture.thenAccept(binding -> {
            // 성공적으로 바인딩되면, 서버가 듣고 있는 주소와 포트를 로깅
            logger.info("{} is listening on {}:{}", serviceName, binding.localAddress().getHostString(), binding.localAddress().getPort());
        }).exceptionally(e -> {
            // 바인딩 실패 시 오류 로깅
            logger.error("{} Binding failed with {}", serviceName, e.getMessage(), e);
            return null;
        });
    }

    /**
     * 리소스 해제: 서버가 더 이상 필요하지 않거나 재시작이 필요한 경우, 열려 있는 포트와 같은 리소스를 안전하게 해제할 수 있어야 함.
     * `serviceUnbind` 메서드는 서버 바인딩을 해제하고, 사용 중인 리소스를 정리하는 과정을 담당. 이는 리소스 누수를 방지하고, 시스템의 안정성을 유지하는 데 중요
     * @param serviceName 서비스 이름, 로깅에 사용됩니다.
     */
    public void serviceUnbind(String serviceName) {
        if(bindingFuture != null) {
            // bindingFuture가 null이 아니면 서버 바인딩 해제 시도
            bindingFuture.thenCompose(ServerBinding::unbind)
                    .thenAccept(unbound -> logger.info("{} listening port unbinding ...", serviceName)) // 성공 로깅
                    .exceptionally(e -> {
                        logger.error("{} Unbinding failed!", serviceName, e);
                        return null;
                    });
        } else {
            // bindingFuture가 null이라면 이미 바인딩 해제되었거나 시작되지 않았음
            logger.info("{} Unbinding Failed because it was not bound!", serviceName);
        }
    }

    public abstract void start();
    public abstract void stop();

}
