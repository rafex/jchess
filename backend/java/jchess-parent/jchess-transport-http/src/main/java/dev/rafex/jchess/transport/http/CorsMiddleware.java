package dev.rafex.jchess.transport.http;

import dev.rafex.ether.http.jetty12.JettyMiddleware;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.util.Set;

public final class CorsMiddleware implements JettyMiddleware {
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "https://jchess.rafex.dev"
    );

    @Override
    public Handler wrap(Handler next) {
        return new Handler.Wrapper(next) {
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws Exception {
                String origin = request.getHeaders().get("Origin");
                if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
                    response.getHeaders().put("Access-Control-Allow-Origin", origin);
                    response.getHeaders().put("Vary", "Origin");
                    response.getHeaders().put("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, Origin");
                    response.getHeaders().put("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
                }

                if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                    response.setStatus(204);
                    callback.succeeded();
                    return true;
                }

                return super.handle(request, response, callback);
            }
        };
    }
}
