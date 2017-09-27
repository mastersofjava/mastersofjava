package nl.moj.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Test {

	private String team;
	private String assignment; 
	private String testname;
	private Integer success;
	private Integer failure;
}
