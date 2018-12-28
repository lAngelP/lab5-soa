package soa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import soa.websocket.TwitterWebSocketHandler;

@org.springframework.context.annotation.Configuration
@EnableWebSocket
public class Configuration implements WebSocketConfigurer {

    private final TwitterWebSocketHandler handler;

    @Autowired
    public Configuration(TwitterWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/stream")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

}
