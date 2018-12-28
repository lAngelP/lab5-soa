package soa.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import soa.eip.Router;
import soa.eip.WebSocketProcessor;
import twitter4j.JSONArray;
import twitter4j.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
public class TwitterWebSocketHandler extends TextWebSocketHandler {

    private final Router router;
    private WebSocketProcessor webSocketProcessor;

    private Map<WebSocketSession, List<String>> clients = new HashMap<>();
    private Map<String, Integer> words = new HashMap<>();

    private static final Logger LOGGER = Logger.getLogger("WebSockets");

    @Autowired
    public TwitterWebSocketHandler(Router router) {
        this.router = router;
    }

    public void setWebSocketProcessor(WebSocketProcessor webSocketProcessor) {
        this.webSocketProcessor = webSocketProcessor;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        LOGGER.info("Server Connected ... " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        cleanUpSession(session);
        LOGGER.info(String.format("Session %s closed because of %s", session.getId(), status.getReason()));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        cleanUpSession(session);
        LOGGER.log(Level.SEVERE,
                String.format("Session %s closed because of %s", session.getId(), exception.getClass().getName()),
                exception);
    }

    private void handleWords(String msg, WebSocketSession session, List<String> words){
        words.forEach(t -> {
            if(msg.contains(t)){
                try {
                    session.sendMessage(new TextMessage(msg));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void handleMessage(String msg){
        clients.forEach((session, words) -> handleWords(msg, session, words));
    }

    private void cleanUpSession(WebSocketSession session){
        List<String> topics = clients.getOrDefault(session, new ArrayList<>());
        topics.forEach(t -> words.put(t, words.getOrDefault(t, 0)-1));
        words.values().removeIf(t -> t <= 0);
        clients.remove(session);
        router.reconfigure(words.keySet(), webSocketProcessor);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        LOGGER.info("Server Message ... " + session.getId() + " - " + message.getPayload());
        String[] values = message.getPayload().split(":", 2);
        LOGGER.info("MSG[0]=" + values[0]);
        LOGGER.info("MSG[1]=" + values[1]);
        //Message format is: SUBSCRIBE:topic or END:
        if ("SUBSCRIBE".equals(values[0].toUpperCase())) {
            String msg = values[1];
            words.put(msg, words.getOrDefault(msg, 0) + 1);

            List<String> wordList = clients.getOrDefault(session, new ArrayList<>());
            wordList.add(msg);
            clients.put(session, wordList);

            router.reconfigure(words.keySet(), webSocketProcessor);
            JSONObject mainObject = new JSONObject();
            mainObject.put("msg", "SUBSCRIBED");
            mainObject.put("term", msg);
            mainObject.put("terms", new JSONArray(clients.values().stream().flatMap(List::stream).collect(Collectors.toList())));

            session.sendMessage(new TextMessage(mainObject.toString()));
            LOGGER.info("User " + session.getId() + " has subbed to " + msg);
        } else if("END".equals(values[0])){
            cleanUpSession(session);

            session.close(new CloseStatus(CloseStatus.NORMAL.getCode(), "Alright then, goodbye!"));
            LOGGER.info("User " + session.getId() + " has unsubbed from everything.");
        }
    }

}
