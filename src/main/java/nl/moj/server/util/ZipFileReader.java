package nl.moj.server.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.util.Assert;


/**
 * unzip archive into outputfilter
 */
@Slf4j
public class ZipFileReader {

	public static void unZipIt(String zipFile) {
		Assert.isTrue(zipFile.contains(".zip"),"not a zip file");
		String outputFolder = zipFile.replace(".zip", "");
		outputFolder = new File(zipFile).getParentFile().getPath();
		String targetFolder = new File(zipFile.replace(".zip","")).getParentFile().getPath();
		unZipIt(zipFile, outputFolder,targetFolder);
	}
	public static void unZipIt(String zipFile, String outputFolder, String targetFolder) {
		log.info("target " + targetFolder + " outputFolder " + outputFolder);
		byte[] buffer = new byte[1024];
		
		try {
			
			
			// create output directory is not exists
			File folder = new File(outputFolder);
			if (!folder.exists()) {
				folder.mkdir();
			}

			// get the zip file content
			ZipInputStream zis = new ZipInputStream(
					new FileInputStream(zipFile));
			// get the zipped file list entry
			ZipEntry ze = zis.getNextEntry();

			while (ze != null) {

				String fileName = ze.getName();
				File newFile = new File(targetFolder + File.separator
						+ fileName);

				System.out.println("file unzip : " + newFile.getAbsoluteFile()
						+ " " + ze.isDirectory());

				// create all non exists folders
				// else you will hit FileNotFoundException for compressed folder
				if (ze.isDirectory()) {
					newFile.mkdirs();

				} else {
					new File(newFile.getParent()).mkdirs();
					FileOutputStream fos = new FileOutputStream(newFile);

					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}

					fos.close();

				}
				ze = zis.getNextEntry();

			}

			zis.closeEntry();
			zis.close();

			log.info("Done");

		} catch (IOException ex) {
			log.error("could no unzip",ex);
		}
	}

}
