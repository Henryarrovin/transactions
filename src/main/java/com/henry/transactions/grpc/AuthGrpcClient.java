package com.henry.transactions.grpc;

import com.google.protobuf.Empty;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

@Component
@Slf4j
public class AuthGrpcClient {

    // Import the auth-service proto stub
    @GrpcClient("auth-service")
    private io.grpc.Channel authChannel;

    @Value("${app.auth.canonical-secret}")
    private String canonicalSecret;

    @Value("${app.auth.service-name}")
    private String serviceName;

    @Value("${app.auth.timeout-seconds:5}")
    private int timeoutSeconds;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public ValidateResult validateToken(String token) {
        try {
            String date = ZonedDateTime.now(ZoneOffset.UTC).format(DATE_FORMAT);
            String sig = sign("POST", "/api/v1/auth/validate", date);

            var stub = io.grpc.stub.AbstractBlockingStub.newStub(
                    AuthServiceBlockingStub::new, authChannel);

            return callValidateToken(token, date, sig);

        } catch (Exception e) {
            log.error("Auth gRPC call failed", e);
            return ValidateResult.invalid("Auth service unavailable: " + e.getMessage());
        }
    }

    private ValidateResult callValidateToken(String token, String date, String sig) {
        try {
            var methodDescriptor =
                    io.grpc.MethodDescriptor.<Empty, Empty>newBuilder()
                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                            .setFullMethodName("auth.v1.AuthService/ValidateToken")
                            .setRequestMarshaller(
                                    io.grpc.protobuf.ProtoUtils.marshaller(Empty.getDefaultInstance()))
                            .setResponseMarshaller(
                                    io.grpc.protobuf.ProtoUtils.marshaller(Empty.getDefaultInstance()))
                            .build();

            log.warn("Direct proto not on classpath — token validation via HTTP fallback");
            return validateViaHttp(token);

        } catch (Exception e) {
            log.error("ValidateToken call failed", e);
            return ValidateResult.invalid(e.getMessage());
        }
    }


    private ValidateResult validateViaHttp(String token) {
        try {
            var client = java.net.http.HttpClient.newHttpClient();
            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://auth-service:8080/api/v1/auth/me"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                    .build();

            var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse response
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var node = mapper.readTree(response.body());

                String userId = node.path("userId").asText("");
                String email = node.path("email").asText("");
                var rolesNode = node.path("roles");
                java.util.List<String> roles = new java.util.ArrayList<>();
                if (rolesNode.isArray()) {
                    rolesNode.forEach(r -> roles.add(r.asText()));
                }

                return ValidateResult.valid(userId, email, roles);
            } else {
                return ValidateResult.invalid("Token validation failed: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("HTTP token validation failed", e);
            return ValidateResult.invalid("Token validation error: " + e.getMessage());
        }
    }

    private String sign(String method, String path, String date) {
        try {
            String canonicalString = String.join("\n", method.toUpperCase(), path, date, serviceName);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(canonicalSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(canonicalString.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign canonical request", e);
        }
    }

    // Stub placeholder

    private static class AuthServiceBlockingStub
            extends io.grpc.stub.AbstractBlockingStub<AuthServiceBlockingStub> {
        protected AuthServiceBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }
        @Override
        protected AuthServiceBlockingStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new AuthServiceBlockingStub(channel, callOptions);
        }
    }

    // Result DTO

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ValidateResult {
        private boolean valid;
        private String userId;
        private String email;
        private java.util.List<String> roles;
        private String error;

        public static ValidateResult valid(String userId, String email, java.util.List<String> roles) {
            ValidateResult r = new ValidateResult();
            r.valid = true;
            r.userId = userId;
            r.email = email;
            r.roles = roles;
            return r;
        }

        public static ValidateResult invalid(String error) {
            ValidateResult r = new ValidateResult();
            r.valid = false;
            r.error = error;
            return r;
        }
    }

}

