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
package nl.moj.server.runtime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.*;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import org.apache.commons.io.IOUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

@Slf4j
public class JavaAssignmentFileResolver {

    public List<AssignmentFile> resolve(AssignmentDescriptor ad) {
        List<AssignmentFile> originalAssignmentFiles = new ArrayList<>();

        // get all sources
        Sources sources = ad.getAssignmentFiles().getSources();
        sources.getEditable().forEach(p -> {
            originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), sources.getBase(), p, AssignmentFileType.EDIT, false));
        });
        sources.getReadonly().forEach(p -> {
            originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), sources.getBase(), p, AssignmentFileType.READONLY, true));
        });
        sources.getHidden().forEach(p -> {
            originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), sources.getBase(), p, AssignmentFileType.HIDDEN, true));
        });

        // get all resources
        Resources resources = ad.getAssignmentFiles().getResources();
        resources.getFiles().forEach(p -> {
            originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), resources.getBase(), p, AssignmentFileType.RESOURCE, true));
        });

        // get all test sources
        TestSources testSources = ad.getAssignmentFiles().getTestSources();
        testSources.getTests().forEach(p -> {
            originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), testSources.getBase(), p, AssignmentFileType.TEST, true));
        });
        testSources.getHiddenTests().forEach(p -> {
            originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), testSources.getBase(), p, AssignmentFileType.HIDDEN_TEST, true));
        });
        testSources.getInvisibleTests().forEach(p -> {
            originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), testSources.getBase(), p, AssignmentFileType.INVISIBLE_TEST, true));
        });

        // get all test resources
        TestResources testResources = ad.getAssignmentFiles().getTestResources();
        testResources.getFiles().forEach(p -> {
            originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), testResources.getBase(), p, AssignmentFileType.TEST_RESOURCE, true));
        });
        testResources.getHiddenFiles().forEach(p -> {
            originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), testResources.getBase(), p, AssignmentFileType.HIDDEN_TEST_RESOURCE, true));
        });
        testResources.getInvisibleFiles().forEach(p -> {
            originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), testResources.getBase(), p, AssignmentFileType.INVISIBLE_TEST_RESOURCE, true));
        });

        // get all solution files
        ad.getAssignmentFiles().getSolution().forEach(p -> {
            originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), null, p, AssignmentFileType.SOLUTION, true));
        });
        // get the assignment
        originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), null, ad.getAssignmentFiles()
                .getAssignment(), AssignmentFileType.TASK, true));

        return originalAssignmentFiles;
    }
    public AssignmentFile convertToAssignmentFile(String assignment, Path assignmentBase, Path prefix, Path file, AssignmentFileType type, boolean readOnly) {
        return convertToAssignmentFile(assignment, assignmentBase, prefix, file, type, readOnly, UUID.randomUUID());
    }

    public AssignmentFile convertToAssignmentFile(String assignment, Path assignmentBase, Path prefix, Path file, AssignmentFileType type, boolean readOnly, UUID uuid) {
        Path ap = assignmentBase;
        Path bp = assignmentBase;
        if (prefix != null) {
            bp = ap.resolve(prefix);
        }
        ap = bp.resolve(file);
        return AssignmentFile.builder()
                .assignment(assignment)
                .content(readPathContent(ap))
                .absoluteFile(ap)
                .base(bp)
                .file(file)
                .name(getName(file))
                .shortName(getShortName(file))
                .fileType(type)
                .readOnly(readOnly)
                .uuid(uuid)
                .mediaType(resolveMediaType(ap))
                .build();
    }

    private MediaType resolveMediaType(Path file) {
        try {
            TikaConfig config = TikaConfig.getDefaultConfig();
            Detector detector = config.getDetector();

            TikaInputStream stream = TikaInputStream.get(file);

            Metadata metadata = new Metadata();
            metadata.add(Metadata.RESOURCE_NAME_KEY, file.getFileName().toString());
            return detector.detect(stream, metadata);
        } catch (Exception e) {
            log.warn("Unable to determine MediaType for {}, assuming text/plain.", e);
            return MediaType.TEXT_PLAIN;
        }
    }

    private String getShortName(Path file) {
        return file.getFileName().toString().substring(0, file.getFileName().toString().indexOf("."));
    }

    private String getName(Path file) {
        return file.toString().substring(0, file.toString().indexOf(".")).replace(File.separatorChar, '.');
    }

    private byte[] readPathContent(Path p) {
        try {
            return IOUtils.toByteArray(Files.newInputStream(p, StandardOpenOption.READ));
        } catch (IOException e) {
            throw new RuntimeException("Unable to read assignment file " + p, e);
        }
    }
}
