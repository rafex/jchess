package dev.rafex.jchess.transport.http;

import com.fasterxml.jackson.databind.JsonNode;
import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.HttpError;
import dev.rafex.ether.http.jetty12.JettyApiErrorResponses;
import dev.rafex.ether.http.jetty12.JettyApiResponses;
import dev.rafex.ether.http.jetty12.JettyHttpExchange;
import dev.rafex.ether.json.JsonCodec;
import org.eclipse.jetty.server.Request;

final class HttpSupport {
    private final JsonCodec jsonCodec;
    private final JettyApiResponses responses;
    private final JettyApiErrorResponses errors;

    HttpSupport(JsonCodec jsonCodec) {
        this.jsonCodec = jsonCodec;
        this.responses = new JettyApiResponses(jsonCodec);
        this.errors = new JettyApiErrorResponses(jsonCodec);
    }

    JettyHttpExchange asJetty(HttpExchange exchange) {
        return (JettyHttpExchange) exchange;
    }

    JsonNode readJson(HttpExchange exchange) {
        return jsonCodec.readTree(Request.asInputStream(asJetty(exchange).request()));
    }

    void ok(HttpExchange exchange, Object body) {
        JettyHttpExchange jetty = asJetty(exchange);
        responses.ok(jetty.response(), jetty.callback(), body);
    }

    void created(HttpExchange exchange, Object body) {
        JettyHttpExchange jetty = asJetty(exchange);
        responses.created(jetty.response(), jetty.callback(), body);
    }

    void error(HttpExchange exchange, int status, String code, String message) {
        JettyHttpExchange jetty = asJetty(exchange);
        errors.error(jetty.response(), jetty.callback(), status, code, message);
    }

    void error(HttpExchange exchange, HttpError error) {
        JettyHttpExchange jetty = asJetty(exchange);
        errors.error(jetty.response(), jetty.callback(), error);
    }
}
