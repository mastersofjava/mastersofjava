package nl.moj.server.competition;

import org.junit.Test;

public class AssignmentRepositoryServiceTest {

	
	@Test
	public void test() {
		
		
		AssignmentRepositoryService repo = new AssignmentRepositoryService();
		repo.cloneRemoteGitRepository();
		
	}
}
