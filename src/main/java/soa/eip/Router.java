package soa.eip;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.twitter.search.TwitterSearchComponent;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.StringJoiner;
import java.util.logging.Logger;

@Component
public class Router extends RouteBuilder {

  private final CamelContext context;

  @Value("${twitter.consumerKey}")
  private String consumerKey;

  @Value("${twitter.consumerSecret}")
  private String consumerSecret;

  @Value("${twitter.accessToken}")
  private String accessToken;

  @Value("${twitter.accessTokenSecret}")
  private String accessTokenSecret;

  @Value("${websocket.delay:6000}")
  private int delay;

  @Value("${websocket.port:9090}")
  private int port;

  private RouteDefinition route = null;

  private static final Logger LOGGER = Logger.getLogger("Router");

  @Autowired
  public Router(CamelContext context) {
    this.context = context;
  }

  @Override
  public void configure() {
    from("direct:search")
            .to("twitter://search?consumerKey={{twitter.consumerKey}}&"
                    + "consumerSecret={{twitter.consumerSecret}}&"
                    + "accessToken={{twitter.accessToken}}&"
                    + "accessTokenSecret={{twitter.accessTokenSecret}}");

    // setup Twitter component
    TwitterSearchComponent tc = getContext().getComponent("twitter-search", TwitterSearchComponent.class);
    tc.setAccessToken(accessToken);
    tc.setAccessTokenSecret(accessTokenSecret);
    tc.setConsumerKey(consumerKey);
    tc.setConsumerSecret(consumerSecret);
  }

  public void reconfigure(Collection<String> terms, WebSocketProcessor webSocketProcessor){
    StringJoiner joiner = new StringJoiner(" ");
    terms.forEach(joiner::add);
    LOGGER.info("Router msg: " + joiner.toString());
    // poll twitter search for new tweets
    if(route != null) {
      try {
        context.stopRoute(route.getId());
        context.removeRoute(route.getId());
      } catch (Exception e) {
        e.printStackTrace();
      }
      LOGGER.info("Removing old route " + route.getId());
    }
    String fullMsg = joiner.toString();
    if(!fullMsg.isEmpty()) {
      route = fromF("twitter-search://%s?delay=%s", joiner.toString(), delay)
              .marshal().json(JsonLibrary.Jackson)
              .convertBodyTo(String.class)
              .process(webSocketProcessor);
      try {
        context.addRouteDefinition(route);
        context.startRoute(route.getId());
        LOGGER.info("Route started!");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }else{
      route = null;
    }
  }

}
