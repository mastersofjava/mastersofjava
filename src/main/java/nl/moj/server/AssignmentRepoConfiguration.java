package nl.moj.server;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "moj.server.assignments")
@Configuration
public class AssignmentRepoConfiguration {

	private List<Repo> repos;

	public AssignmentRepoConfiguration(List<Repo> repos) {
		this.repos = repos;
	}

	public AssignmentRepoConfiguration() {
	}

	public List<Repo> getRepos() {
		return repos;
	}

	public void setRepos(List<Repo> repos) {
		this.repos = repos;
	}

	public static class Repo {

		private String name;
		private String url;
		private String branch;
		
		public Repo() {
		}

		public Repo(String name, String url, String branch) {
			super();
			this.name = name;
			this.setUrl(url);
			this.setBranch(branch);
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getBranch() {
			return branch;
		}

		public void setBranch(String branch) {
			this.branch = branch;
		}

	}

}