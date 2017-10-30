package nl.moj.server;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import org.codehaus.plexus.util.FileUtils;
import org.springframework.stereotype.Service;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

@Service
public class SoundService {

	public void playStartGong() {
		File gong = FileUtils
				.getFile("/home/mhayen/Workspaces/workspace-moj/server/src/main/resources/sounds/gong.wav");
		Media m = new Media(gong.toURI().toString());
		MediaPlayer player = new MediaPlayer(m);
		player.play();
	}

	public void playTicTac1Sound() {
		CompletableFuture.runAsync(() -> {
			// 10 seconden
			File gong = FileUtils
					.getFile("/home/mhayen/Workspaces/workspace-moj/server/src/main/resources/sounds/tictac1.wav");
			Media m = new Media(gong.toURI().toString());
			MediaPlayer player = new MediaPlayer(m);
			player.setCycleCount(6);
			player.play();
		});
	}

	public void playTicTac2Sound() {
		CompletableFuture.runAsync(() -> {
			// 23 seconden
			File gong = FileUtils
					.getFile("/home/mhayen/Workspaces/workspace-moj/server/src/main/resources/sounds/tictac2.wav");
			Media m = new Media(gong.toURI().toString());
			MediaPlayer player = new MediaPlayer(m);
			player.setCycleCount(6);
			player.play();
		});
	}

	public void playSlowTicTac2Sound() {
		CompletableFuture.runAsync(() -> {
			// 1,5 seconden
			File gong = FileUtils
					.getFile("/home/mhayen/Workspaces/workspace-moj/server/src/main/resources/sounds/tictac2.wav");
			Media m = new Media(gong.toURI().toString());
			MediaPlayer player = new MediaPlayer(m);
			player.setCycleCount(40);
			player.play();
		});
	}
}
