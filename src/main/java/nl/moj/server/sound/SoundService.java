package nl.moj.server.sound;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.config.properties.MojServerProperties;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SoundService {

    private final MojServerProperties mojServerProperties;

    public void playGong() {
        play(Sound.GONG, 0);
    }

    /**
     * Play the sound for specified seconds
     *
     * @param sound
     * @param seconds
     */
    public void play(Sound sound, long seconds) {
        try {
            if (mojServerProperties.getRuntime().isPlaySounds()) {
                SoundPlayer player = new SoundPlayer(sound);
                player.play(seconds);
            }
        } catch (Exception e) {
            log.error("Unable to play sound: {}", sound, e);
        }
    }

    private class SoundPlayer {

        private Path song;
        private Clip clip;

        private SoundPlayer(Sound sound) throws LineUnavailableException {

            this.song = mojServerProperties.getDirectories().getBaseDirectory()
                    .resolve(mojServerProperties.getDirectories().getSoundDirectory())
                    .resolve(sound.filename()).toAbsolutePath();
            clip = AudioSystem.getClip();
        }

        private void play(long seconds) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
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
