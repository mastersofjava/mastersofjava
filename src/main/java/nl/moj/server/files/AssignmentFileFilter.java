package nl.moj.server.files;

import java.io.File;
import java.io.IOException;

import org.springframework.integration.file.filters.AbstractFileListFilter;

/**
 * filters out eclipse settings and maven target folders
 * 
 * @author mhayen
 *
 */
public class AssignmentFileFilter {

	boolean accept(File file) {
		try {
			if (file.getCanonicalPath().contains(".settings")) {
				return false;
			}
			if (file.getCanonicalPath().contains(".git")) {
				return false;
			}
			if (file.getCanonicalPath().contains("target")) {
				return false;
			}
			if (file.getCanonicalPath().contains("README.md")) {
				return false;
			}
		} catch (IOException e) {
			// ignore
		}
		return true;
	}

}
