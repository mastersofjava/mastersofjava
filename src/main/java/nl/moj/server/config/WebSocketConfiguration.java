/*
   Copyright 2020 First Eight BV (The Netherlands)
 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketConfiguration {

    @Configuration
    @EnableWebSocketMessageBroker
    public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

        @Override
        public void configureMessageBroker(MessageBrokerRegistry config) {
            config.enableSimpleBroker("/topic", "/queue");
            config.setApplicationDestinationPrefixes("/app");
            config.setUserDestinationPrefix("/user");
        }

        @Override
        public void registerStompEndpoints(StompEndpointRegistry registry) {
            registry.addEndpoint("/ws/competition/websocket");
            registry.addEndpoint("/submit/websocket");
            registry.addEndpoint("/control/websocket");
            registry.addEndpoint("/rankings/websocket");
            registry.addEndpoint("/feedback/websocket");
        }
    }

    @Configuration
    public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

        protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
            messages.simpDestMatchers("/*").authenticated();
        }

        @Override
        protected boolean sameOriginDisabled() {
            // disable CSRF for websockets for now...
            return true;
        }
    }
}
