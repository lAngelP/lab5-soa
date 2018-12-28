package soa.eip;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import soa.websocket.TwitterWebSocketHandler;

import java.util.logging.Logger;


@Component
public class WebSocketProcessor implements Processor {

    private final TwitterWebSocketHandler handler;

    private static final Logger LOGGER = Logger.getLogger("WebSocketProcessor");


    @Autowired
    public WebSocketProcessor(TwitterWebSocketHandler handler) {
        this.handler = handler;
        handler.setWebSocketProcessor(this);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String msg = exchange.getMessage().getBody().toString();
        LOGGER.info("Handle message: " + msg);
        handler.handleMessage(msg);
    }
}
