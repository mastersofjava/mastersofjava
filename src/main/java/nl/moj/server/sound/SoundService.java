package nl.moj.server.sound;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.DirectoriesConfiguration;
import org.springframework.stereotype.Service;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SoundService {

	private DirectoriesConfiguration directoriesConfiguration;

	public SoundService(DirectoriesConfiguration directoriesConfiguration) {
		this.directoriesConfiguration = directoriesConfiguration;
	}


	public void playGong() {
		play(Sound.GONG, 0);
	}

	public void playTicTac(int seconds) {
		play(Sound.TIC_TAC, seconds);
	}

	private void play(Sound sound, int seconds) {
		try {
			SoundPlayer player = new SoundPlayer(sound);
			player.play(seconds);
		} catch (Exception e) {
			log.error("Unable to play sound: {}", sound, e);
		}
	}

	private class SoundPlayer {

		private Path song;
		private Clip clip;

		private SoundPlayer(Sound sound) throws LineUnavailableException {

			this.song = Paths.get(directoriesConfiguration.getBaseDirectory(), directoriesConfiguration.getSoundDirectory(), sound.filename()).toAbsolutePath();
			clip = AudioSystem.getClip();
		}

		private void play(int seconds) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
			CountDownLatch l = new CountDownLatch(1);
			log.debug("Start playing {}.", song);
			clip.open(AudioSystem.getAudioInputStream(song.toFile()));
			clip.addLineListener(event -> {
				if (event.getType() == LineEvent.Type.STOP) {
					l.countDown();
				}
			});
			try {
				if (seconds > 0) {
					clip.loop(Clip.LOOP_CONTINUOUSLY);
					l.await(seconds * 1000 * 1000 + 100, TimeUnit.MICROSECONDS);
				} else {
					clip.start();
					l.await(clip.getMicrosecondLength() + 100, TimeUnit.MICROSECONDS);
				}
			} catch (InterruptedException e) {
				// ignored
			}
			log.debug("Finished playing {}.", song);
			clip.close();
		}
	}
}
