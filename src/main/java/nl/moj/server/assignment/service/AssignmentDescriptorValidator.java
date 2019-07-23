package nl.moj.server.assignment.service;

import java.nio.file.Path;
import java.util.List;

import nl.moj.server.assignment.descriptor.*;
import nl.moj.server.assignment.model.AssignmentDescriptorValidationResult;
import org.springframework.stereotype.Service;

@Service
public class AssignmentDescriptorValidator {

    public AssignmentDescriptorValidationResult validate(AssignmentDescriptor descriptor) {
        AssignmentDescriptorValidationResult result = new AssignmentDescriptorValidationResult(descriptor.getName(), descriptor);

        validateScoringRules(result, descriptor.getScoringRules());
        validateAssignmentFiles(result, descriptor);
        validateSimpleProperties(result, descriptor);

        return result;
    }

    private void validateSimpleProperties(AssignmentDescriptorValidationResult result, AssignmentDescriptor descriptor) {
        if (descriptor.getDuration() == null) {
            result.addValidationMessage("No duration specified.");
        }
        if (descriptor.getName() == null) {
            result.addValidationMessage("No name specified.");
        }
        if (descriptor.getDisplayName() == null) {
            result.addValidationMessage("No display name specified.");
        }
        if (descriptor.getImage() != null) {
            validateFile(result, "Image file %s not found.", null, descriptor.getImage());
        }
        if (descriptor.getSponsorImage() != null) {
            validateFile(result, "Sponsor image file %s not found.", null, descriptor.getSponsorImage());
        }
    }

    private void validateAssignmentFiles(AssignmentDescriptorValidationResult result, AssignmentDescriptor descriptor) {
        AssignmentFiles files = descriptor.getAssignmentFiles();
        if (files != null) {
            validateSolutionFiles(result, files.getSolution());
            validateSourceFiles(result, files.getSources());
            validateResourceFiles(result, files.getResources());
            validateTestSourceFiles(result, files.getTestSources());
            validateTestResourceFiles(result, files.getTestResources());
            validateAssignmentFile(result, files.getAssignment());

        } else {
            result.addValidationMessage("No assignment files specified.");
        }
    }

    private void validateSolutionFiles(AssignmentDescriptorValidationResult result, List<Path> files) {
        if (files == null || files.isEmpty()) {
            result.addValidationMessage("No solution specified.");
        } else {
            files.forEach(f -> {
                validateFile(result, "Solution file %s not found.", null, f);
            });
        }
    }

    private void validateSourceFiles(AssignmentDescriptorValidationResult result, Sources sources) {
        if (sources == null) {
            result.addValidationMessage("No sources specified.");
        } else {
            if (sources.getEditable() == null || sources.getEditable().isEmpty()) {
                result.addValidationMessage("No editable files specified, should have at least one.");
            } else {
                sources.getEditable()
                        .forEach(f -> validateFile(result, "Source file %s not found.", sources.getBase(), f));
            }
            if (sources.getReadonly() != null && !sources.getReadonly().isEmpty()) {
                sources.getReadonly()
                        .forEach(f -> validateFile(result, "Source file %s not found.", sources.getBase(), f));
            }
        }
    }

    private void validateResourceFiles(AssignmentDescriptorValidationResult result, Resources resources) {
        if (resources != null) {

            if (resources.getFiles() != null && !resources.getFiles().isEmpty()) {
                resources.getFiles()
                        .forEach(f -> validateFile(result, "Resource file %s not found.", resources.getBase(), f));
            }
        }
    }

    private void validateTestSourceFiles(AssignmentDescriptorValidationResult result, TestSources sources) {
        if (sources == null) {
            result.addValidationMessage("No test sources specified.");
        } else {
            if (sources.getTests() == null || sources.getTests().isEmpty()) {
                result.addValidationMessage("No test files specified, should have at least one.");
            } else {
                sources.getTests()
                        .forEach(f -> validateFile(result, "TestCase source file %s not found.", sources.getBase(), f));
            }
            if (sources.getHiddenTests() != null && !sources.getHiddenTests().isEmpty()) {
                sources.getHiddenTests()
                        .forEach(f -> validateFile(result, "Hidden test source file %s not found.", sources.getBase(), f));
            }
        }
    }

    private void validateTestResourceFiles(AssignmentDescriptorValidationResult result, TestResources sources) {
        if (sources == null) {
            result.addValidationMessage("No test sources specified.");
        } else {
            if (sources.getFiles() != null && !sources.getFiles().isEmpty()) {
                sources.getFiles()
                        .forEach(f -> validateFile(result, "TestCase resource file %s not found.", sources.getBase(), f));
            }
            if (sources.getHiddenFiles() != null && !sources.getHiddenFiles().isEmpty()) {
                sources.getHiddenFiles()
                        .forEach(f -> validateFile(result, "Hidden test resource file %s not found.", sources.getBase(), f));
            }
        }
    }

    private void validateAssignmentFile(AssignmentDescriptorValidationResult result, Path file) {
        if (file == null) {
            result.addValidationMessage("No assignment instruction specified.");
        } else {
            validateFile(result, "Assignment instruction file %s not found.", null, file);
        }
    }

    private void validateScoringRules(AssignmentDescriptorValidationResult result, ScoringRules scoringRules) {
        if (scoringRules == null) {
            result.addValidationMessage("No scoring rules specified.");
        } else {
            if (scoringRules.getMaximumResubmits() != null && scoringRules.getMaximumResubmits() < 0) {
                result.addValidationMessage("Maximum resubmits must be >= 0.");
            }
            validatePercentageValue(result, "Resubmit penalty", scoringRules.getResubmitPenalty());
            validatePercentageValue(result, "TestCase penalty", scoringRules.getTestPenalty());
        }
    }

    private void validatePercentageValue(AssignmentDescriptorValidationResult result, String valueName, String percentage) {
        if (percentage != null) {
            String value = percentage;
            if (percentage.endsWith("%")) {
                value = percentage.substring(0, value.length() - 1);
            }
            try {
                long v = Long.valueOf(value.replace("%", ""));
                if (v < 0 || v > 100) {
                    result.addValidationMessage(String.format("%s must be >= 0 and <= 100.", valueName));
                }
            } catch (NumberFormatException nfe) {
                result.addValidationMessage(String.format("%s invalid, must be a number (e.g. 25) or a percentage string (e.g. 25%%).", valueName));
            }
        }
    }

    private void validateFile(AssignmentDescriptorValidationResult result, String errorPattern, Path prefix, Path f) {
        Path fp = resolve(result.getAssignmentDescriptor().getDirectory(), prefix, f);
        if (!fp.toFile().exists()) {
            result.addValidationMessage(String.format(errorPattern, fp.toString()));
        }
    }

    private Path resolve(Path base, Path prefix, Path p) {
        Path result = base;
        if (prefix != null) {
            result = result.resolve(prefix);
        }
        return result.resolve(p);
    }
}
