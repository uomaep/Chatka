package com.uomaep.chat.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.*;
import akka.http.javadsl.model.headers.RawHeader;
import com.uomaep.chat.config.Environment.HttpClientGet;
import com.uomaep.chat.config.Environment.HttpClientPost;
import com.uomaep.chat.config.Environment.HttpClientResponseFailure;
import com.uomaep.chat.config.Environment.HttpClientResponseSuccess;

import java.util.concurrent.CompletionStage;

public class HttpClient extends AbstractActor {
    private final ActorSystem system;

    public HttpClient(ActorSystem system) {
        this.system = system;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(HttpClientGet.class, o -> {
                    get(o.event, o.path, sender());
                })
                .match(HttpClientPost.class, o -> {
                    post(o.event, o.path, o.token, o.jsonBody, sender());
                })
                .match(HttpClientResponseSuccess.class, o -> {
                })
                .match(HttpClientResponseFailure.class, o -> {

                })
                .build();

    }

    private void get(String event, String path, ActorRef recipient) {
        CompletionStage<HttpResponse> respFuture = Http.get(system)
                .singleRequest(HttpRequest.create(path).withMethod(HttpMethods.GET));

        pipeToSelf(event, respFuture, recipient);
    }

    private void post(String event, String path, String token, String jsonBody, ActorRef recipient) {
        final HttpEntity.Strict objEntity = HttpEntities.create(ContentTypes.APPLICATION_JSON, jsonBody);
        final HttpRequest req = HttpRequest.POST(path)
                .withEntity(objEntity)
                .addHeader(RawHeader.create("Authorization", "Token" + token));

        final CompletionStage<HttpResponse> respFuture = Http.get(system).singleRequest(req);

        pipeToSelf(event, respFuture, recipient);
    }

    private void pipeToSelf(String event, CompletionStage<HttpResponse> future, ActorRef recipient) {
        future.whenComplete((resp, failure) -> {
            if(failure != null) {
                self().tell(new HttpClientResponseFailure(event, failure.getMessage(), recipient), self());
            } else {
                self().tell(new HttpClientResponseSuccess(event, resp, recipient), self());
            }
        });
    }
}
