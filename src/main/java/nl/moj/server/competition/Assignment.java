package nl.moj.server.competition;

import java.util.ArrayList;
import java.util.List;

public class Assignment {

	private String name;
	
	private List<String> filenames = new ArrayList<>();

	public Assignment(String name, List<String> filenames) {
		super();
		this.name = name;
		this.filenames = filenames;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getFilenames() {
		return filenames;
	}

	public void setFilenames(List<String> filenames) {
		this.filenames = filenames;
	}
	
	public void addFilename(String filename) {
		filenames.add(filename);
	}
}
