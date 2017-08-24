package nl.moj.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class SourceMessageDeserializer extends JsonDeserializer<SourceMessage> {
    @Override
    public SourceMessage deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String team = node.get("team").textValue();
        Map<String,String> sources = new HashMap<>();
        if (node.get("source").isArray()) {
        	ArrayNode sourceArray = (ArrayNode)node.get("source");
        	for (int i =0; i < sourceArray.size(); i++) {
        		JsonNode sourceElement = sourceArray.get(i);
        		sources.put(sourceElement.get("filename").textValue(), sourceElement.get("content").textValue());
        	}
        }
        return new SourceMessage(team,sources);
    }
}
