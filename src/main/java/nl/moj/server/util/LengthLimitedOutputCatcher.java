package nl.moj.server.util;

import nl.moj.server.config.properties.MojServerProperties;
import org.zeroturnaround.exec.stream.LogOutputStream;

import static java.lang.Math.min;

/**
 * Support class to capture a limited shard of potentially huge output. The
 * output is limited to a maximum number of lines, a maximum number of chars per
 * line, and a total maximum number of characters.
 *
 * @author hartmut
 */
public class LengthLimitedOutputCatcher extends LogOutputStream {
	private final StringBuilder buffer = new StringBuilder();
	private final int maxSize;
	private final int maxLines;
	private final int maxLineLenght;
	private final String lineTruncatedMessage;
	private final String outputTruncMessage;
	private int lineCount = 0;

	public LengthLimitedOutputCatcher(MojServerProperties mojServerProperties) {
		this.maxSize = mojServerProperties.getLimits().getUnitTestOutput().getMaxChars();
		this.maxLines = mojServerProperties.getLimits().getUnitTestOutput().getMaxFeedbackLines();
		this.maxLineLenght = mojServerProperties.getLimits().getUnitTestOutput().getMaxLineLen();
		this.lineTruncatedMessage = mojServerProperties.getLimits().getUnitTestOutput().getLineTruncatedMessage();
		this.outputTruncMessage = mojServerProperties.getLimits().getUnitTestOutput().getOutputTruncMessage();
	}

	@Override
	protected void processLine(String line) {
		if (lineCount < maxLines) {
			final int maxAppendFromBufferSize = min(line.length(), maxSize - buffer.length() + 1);
			final int maxAppendFromLineLimit = min(maxAppendFromBufferSize, maxLineLenght);
			if (maxAppendFromLineLimit > 0) {
				final boolean isLineTruncated = maxAppendFromLineLimit < line.length();
				if (isLineTruncated) {
					buffer.append(line.substring(0, maxAppendFromLineLimit)).append(lineTruncatedMessage);
				} else {
					buffer.append(line);
				}
				buffer.append('\n');
			}
		} else if (lineCount == maxLines) {
			buffer.append(outputTruncMessage);
		}
		lineCount++;
	}

	public StringBuilder getBuffer() {
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
