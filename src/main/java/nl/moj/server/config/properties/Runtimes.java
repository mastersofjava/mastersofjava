package nl.moj.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix="moj.server.runtimes")
public class Runtimes {

	private Test test = new Test();
	private Compile compile = new Compile();
	private Server server = new Server();

	@Data
	@ConfigurationProperties
	public static final class Test {
		private long timeout = 5;
		private int threads = 3;
	}

	@Data
	@ConfigurationProperties
	public static final class Compile {
		private long timeout = 5;
		private int threads = 3;
	}

	@Data
	@ConfigurationProperties
	public static final class Server {
		private int threads = 10;
	}
}
