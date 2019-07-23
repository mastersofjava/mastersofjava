package nl.moj.server.submit.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import nl.moj.server.submit.model.SourceMessage;

public class SourceMessageDeserializer extends JsonDeserializer<SourceMessage> {

    public SourceMessageDeserializer() {
    }

    @Override
    public SourceMessage deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        Map<String, String> sources = new HashMap<>();
        if (node.get("sources") != null && node.get("sources").isArray()) {
            ArrayNode sourceArray = (ArrayNode) node.get("sources");
            for (int i = 0; i < sourceArray.size(); i++) {
                JsonNode sourceElement = sourceArray.get(i);
                sources.put(sourceElement.get("uuid").textValue(), sourceElement.get("content").textValue());
            }
        }
        List<String> tests = new ArrayList<>();
        if (node.get("tests") != null && node.get("tests").isArray()) {
            ArrayNode jsonTests = (ArrayNode) node.get("tests");
            jsonTests.forEach(t -> tests.add(t.asText()));
        }
        return new SourceMessage(sources, tests);
    }
}
