package com.henry.transactions.config;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class GrpcConfig {

    // Server interceptor — logs all gRPC calls

    @GrpcGlobalServerInterceptor
    public ServerInterceptor loggingInterceptor() {
        return new ServerInterceptor() {
            @Override
            public <Q, R> ServerCall.Listener<Q> interceptCall(
                    ServerCall<Q, R> call,
                    Metadata headers,
                    ServerCallHandler<Q, R> next) {

                String method = call.getMethodDescriptor().getFullMethodName();
                String correlationId = headers.get(
                        Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER));

                log.info("[grpc] {} correlation_id={}",
                        method, correlationId != null ? correlationId : "none");

                return next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                    @Override
                    public void close(Status status, Metadata trailers) {
                        if (!status.isOk()) {
                            log.warn("[grpc] {} failed: {} {}",
                                    method, status.getCode(), status.getDescription());
                        }
                        super.close(status, trailers);
                    }
                }, headers);
            }
        };
    }
}
