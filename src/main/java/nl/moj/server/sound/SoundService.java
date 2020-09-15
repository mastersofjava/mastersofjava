/*
   Copyright 2020 First Eight BV (The Netherlands)
 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
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
            try {
                clip = AudioSystem.getClip();
            } catch( IllegalArgumentException e ) {
                log.warn("No audio-system detected, sound playing disabled.");
                clip = null;
            }
        }

        private void play(long seconds) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
            if( clip != null  ) {
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
}
