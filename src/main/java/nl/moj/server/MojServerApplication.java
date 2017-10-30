package nl.moj.server;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javafx.stage.Stage;
@SpringBootApplication
public class MojServerApplication extends AbstractJavaFxApplicationSupport {

	@Autowired
	private DirectoriesConfiguration directories;

	public static void main(String[] args) {
		launchApp(MojServerApplication.class, args);
	}

	public MojServerApplication() {
		// TODO Auto-generated constructor stub
	}

	@PostConstruct
	public void initialize() {
		if (!FileUtils.getFile(directories.getBaseDirectory()).exists()) {
			FileUtils.getFile(directories.getBaseDirectory()).mkdir();
		}
		if (!FileUtils.getFile(directories.getBaseDirectory(), directories.getTeamDirectory()).exists()) {
			FileUtils.getFile(directories.getBaseDirectory(), directories.getTeamDirectory()).mkdir();
		}
		if (!FileUtils.getFile(directories.getBaseDirectory(), directories.getAssignmentDirectory()).exists()) {
			FileUtils.getFile(directories.getBaseDirectory(), directories.getAssignmentDirectory()).mkdir();
		}
		if (!FileUtils.getFile(directories.getBaseDirectory(), directories.getLibDirectory()).exists()) {
			FileUtils.getFile(directories.getBaseDirectory(), directories.getLibDirectory()).mkdir();
		}
	}

	@Override
	public void start(Stage arg0) throws Exception {
		// Bootstrap Spring context here.
	}

}