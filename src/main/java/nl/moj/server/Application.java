package nl.moj.server;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	
	private static final Logger log = LoggerFactory.getLogger(Application.class);

	@Value("${moj.server.assignmentDirectory}")
	public String assignmentDirectory;

	@Value("${moj.server.teamDirectory}")
	private String teamDirectory;

	@Value("${moj.server.libDirectory}")
	private String libDirectory;

	@Value("${moj.server.basedir}")
	private String basedir;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@PostConstruct
	public void init() {
		if (!FileUtils.getFile(basedir, teamDirectory).exists()) {
			FileUtils.getFile(basedir, teamDirectory).mkdir();
		}
		if (!FileUtils.getFile(basedir, assignmentDirectory).exists()) {
			FileUtils.getFile(basedir, assignmentDirectory).mkdir();
		}
		if (!FileUtils.getFile(basedir, libDirectory).exists()) {
			FileUtils.getFile(basedir, libDirectory).mkdir();
		}

	}

}