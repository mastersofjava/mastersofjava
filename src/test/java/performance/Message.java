package performance;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public class Message {

        public List<Source> sources;
        public List<String> tests;
        public String assignmentName;
        public String uuid;
        public String timeLeft;
        public Long arrivalTime;

        @AllArgsConstructor
        public static class Source {
            public String uuid;
            public String content;
        }
}
