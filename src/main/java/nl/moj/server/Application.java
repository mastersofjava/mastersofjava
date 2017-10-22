package nl.moj.server;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	@Value("${moj.server.assignmentDirectory}")
	public String assignmentDirectory;

	@Value("${moj.server.teamDirectory}")
	private String teamDirectory;

	@Value("${moj.server.basedir}")
	private String basedir;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    
    @PostConstruct
    public void init() {
    	
    }
    
}