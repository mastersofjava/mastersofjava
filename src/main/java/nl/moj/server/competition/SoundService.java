package nl.moj.server.competition;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import nl.moj.server.DirectoriesConfiguration;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class SoundService {

	private static final String TIC_TAC = "slowtictaclong.wav";
	private static final String GONG = "gong.wav";

	private DirectoriesConfiguration directoriesConfiguration;

	public SoundService(DirectoriesConfiguration directoriesConfiguration) {
		this.directoriesConfiguration = directoriesConfiguration;
	}

	private void playSound(String wav) {
		Path gong = Paths.get(directoriesConfiguration.getBaseDirectory(),directoriesConfiguration.getSoundDirectory(), wav).toAbsolutePath();
		Media m = new Media(gong.toUri().toString());
		MediaPlayer player = new MediaPlayer(m);
		player.play();
	}

	public void playGong() {
		playSound(GONG);
	}

	public void playTicTac() {
		playSound(TIC_TAC);
	}
}
