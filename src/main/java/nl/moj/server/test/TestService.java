package nl.moj.server.test;

import static java.lang.Math.min;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;

import nl.moj.server.FeedbackController;
import nl.moj.server.competition.Competition;
import nl.moj.server.compile.CompileResult;
import nl.moj.server.files.AssignmentFile;

@Service
public class TestService {
    private static final String TRUNCATED = "...{truncated}";
    private static final String TERMINATED = "...{terminated: test time expired}";
    private static final Pattern JUNIT_PREFIX_P = Pattern.compile( "^(JUnit version 4.12)?\\s*\\.?", Pattern.MULTILINE|Pattern.CASE_INSENSITIVE|Pattern.DOTALL );


    @Value("${moj.server.limits.unitTestOutput.maxChars}")
    private int MAX_FEEDBACK_SIZE;

    @Value("${moj.server.limits.unitTestOutput.maxLines}")
    private int MAX_FEEDBACK_LINES;

    @Value("${moj.server.limits.unitTestOutput.maxLineLen}")
    private int MAX_FEEDBACK_LINES_LENGTH;

	private static final Logger log = LoggerFactory.getLogger(TestService.class);


	@Autowired
	@Qualifier("testing")
	private Executor testing;

	@Value("${moj.server.timeout}")
	private int timeout;

	@Value("${moj.server.teamDirectory}")
	private String teamDirectory;

	@Value("${moj.server.libDirectory}")
	private String libDirectory;

	@Value("${moj.server.basedir}")
	private String basedir;

	@Value("${moj.server.javaExecutable}")
	private String javaExecutable;

	@Autowired
	private Competition competition;

	@Autowired
	private FeedbackController feedback;


	public CompletableFuture<List<TestResult>> testAll(CompileResult compileResult) {
		return CompletableFuture.supplyAsync(new Supplier<List<TestResult>>() {
			@Override
			public List<TestResult> get() {
				if (compileResult.isSuccessful()) {
					List<TestResult> result = new ArrayList<>();
					List<String> tests = compileResult.getTests();
					List<AssignmentFile> testFiles = competition.getCurrentAssignment().getTestFiles().stream()
							.filter(f -> tests.contains(f.getName())).collect(Collectors.toList());
					for (AssignmentFile assignmentFile : testFiles) {
						TestResult tr = unittest(assignmentFile, compileResult);
						tr.setSubmit(false);
						feedback.sendTestFeedbackMessage(tr, false);
					}
					return result;
				} else {
					return new ArrayList<>();
				}
			}
		}, testing);
	}

	public CompletableFuture<TestResult> testSubmit(CompileResult compileResult) {
		return CompletableFuture.supplyAsync(new Supplier<TestResult>() {
			@Override
			public TestResult get() {
				competition.getCurrentAssignment().addFinishedTeam(compileResult.getUser());
				if (compileResult.isSuccessful()) {
					TestResult tr = null;
					List<AssignmentFile> testFiles = competition.getCurrentAssignment().getSubmitFiles();
					// there should be only 1;
					tr = unittest(testFiles.get(0), compileResult);
					tr.setSubmit(true);
					feedback.sendTestFeedbackMessage(tr, true);
					return tr;
				}
				return null;
			}
		}, testing);

	}

	private TestResult unittest(AssignmentFile file, CompileResult compileResult) {

	    log.info("running unittest: {}", file.getName());
	    File teamdir = FileUtils.getFile(basedir, teamDirectory, compileResult.getUser());
	    File policy = FileUtils.getFile(basedir, libDirectory, "securityPolicyForUnitTests.policy");
	    if (!policy.exists()) {
	        throw new RuntimeException("security policy file not found");
	    }

	    try {
	        boolean isRunTerminated = false;
	        int exitvalue=0;
	            final LengthLimitedOutputCatcher jUnitOutput = new LengthLimitedOutputCatcher(
	                    MAX_FEEDBACK_LINES,
	                    MAX_FEEDBACK_SIZE,
	                    MAX_FEEDBACK_LINES_LENGTH);
	            final LengthLimitedOutputCatcher jUnitError = new LengthLimitedOutputCatcher(
	                    MAX_FEEDBACK_LINES,
	                    MAX_FEEDBACK_SIZE,
	                    MAX_FEEDBACK_LINES_LENGTH);
	            try {
	            exitvalue = new ProcessExecutor().command(javaExecutable,
	                    "-cp", makeClasspath(compileResult.getUser()),
	                    "-Djava.security.manager",
	                    "-Djava.security.policy=" + policy.getAbsolutePath(),
	                    "org.junit.runner.JUnitCore", file.getName()
	                    )
	                    .directory( teamdir )
	                    .timeout( 2+2, TimeUnit.SECONDS )
	                    .redirectOutput( jUnitOutput )
	                    .redirectError( jUnitError )
	                    .execute()
	                    .getExitValue();
	        }
	        catch (TimeoutException e) {
	            // process is automatically destroyed
	            log.debug("Unit test for {} timed out and got killed", compileResult.getUser());
	            isRunTerminated = true;
	        }
	        catch (SecurityException se) {
	            log.error(se.getMessage(), se);
	        }
	        log.debug("exitValue {}", exitvalue);
	        if (isRunTerminated) {
	            jUnitOutput.getOutput().append('\n').append(TERMINATED);
	        }


	        final boolean success;
	        final String result;
            if (jUnitOutput.length() > 0) {
	            stripJUnitPrefix( jUnitOutput.getOutput() );
	            // if we still have some output left and exitvalue = 0
	            if (jUnitOutput.length() > 0 && exitvalue == 0) {
	                success = true;
	                // result = filteroutput(output);
	            } else {
	                success = false;
	            }
	            result = jUnitOutput.toString();
	        } else {
	            log.debug( jUnitOutput.toString() );
	            result = jUnitError.toString();
	            success = (exitvalue == 0);
	        }

	        log.debug("success {}", success);
	        log.info("finished unittest: {}", file.getName());
	        return new TestResult(result, compileResult.getUser(), success, file.getName());
	    } catch (Exception e) {
	        log.error(e.getMessage(), e);
	    }
	    return null;
	}


    private void stripJUnitPrefix(StringBuilder result) {
        final Matcher matcher = JUNIT_PREFIX_P.matcher(result);
        if (matcher.find()) {
            log.debug("stipped '{}'", matcher.group());
            result.delete(0, matcher.end());
        } else {
            log.debug("stripped nothing of '{}'", result.subSequence(0, 50));
        }
    }

	private String makeClasspath(String user) {
		final List<File> classPath = new ArrayList<>();
		classPath.add(FileUtils.getFile(basedir, teamDirectory, user));
		classPath.add(FileUtils.getFile(basedir, libDirectory, "junit-4.12.jar"));
		classPath.add(FileUtils.getFile(basedir, libDirectory, "hamcrest-all-1.3.jar"));
		for (File file : classPath) {
			if (!file.exists()) {
				System.out.println("not found: " + file.getAbsolutePath());
			}
		}
		StringBuilder sb = new StringBuilder();
		for (File file : classPath) {
			sb.append(file.getAbsolutePath());
			sb.append(System.getProperty("path.separator"));
		}
		return sb.toString();
	}


	/**
     * Support class to capture a limited shard of potentially huge output.
     * The output is limited to a maximum number of lines, a maximum number of chars per line,
     * and a total maximum number of characters.
     *
     * @author hartmut
     */
    private final static class LengthLimitedOutputCatcher extends LogOutputStream {
        private final StringBuilder buffer;
        private final int maxSize;
        private final int maxLines;
        private final int maxLineLenght;
        private int lineCount=0;

        private LengthLimitedOutputCatcher(int maxLines, int maxSize, int maxLineLenght) {
            this.buffer = new StringBuilder();
            this.maxSize = maxSize;
            this.maxLines = maxLines;
            this.maxLineLenght = maxLineLenght;
        }

        @Override
        protected void processLine(String line) {
            if (lineCount < maxLines) {
                final int maxAppendFromBufferSize = min( line.length(), maxSize-buffer.length()+1 );
                final int maxAppendFromLineLimit = min( maxAppendFromBufferSize, maxLineLenght );
                if (maxAppendFromLineLimit>0) {
                    final boolean isLineTruncated = maxAppendFromLineLimit < line.length();
                    if (isLineTruncated) {
                        buffer.append(line.substring(0, maxAppendFromLineLimit-TRUNCATED.length())).append(TRUNCATED);
                    } else {
                        buffer.append( line );
                    }
                    buffer.append('\n');
                }
            } else if (lineCount == maxLines) {
                buffer.append(TRUNCATED);
            }
            lineCount++;
        }

        public StringBuilder getOutput() {
            return buffer;
        }

        @Override
        public String toString() {
            return buffer.toString();
        }

        public int length() {
            return buffer.length();
        }
    }


}
