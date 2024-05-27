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
package nl.moj.worker.util;

import static java.lang.Math.min;

import org.zeroturnaround.exec.stream.LogOutputStream;

import nl.moj.common.config.properties.Limits;

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

    public LengthLimitedOutputCatcher(Limits.OutputLimits limits) {
        this.maxSize = limits.getMaxChars();
        this.maxLines = limits.getMaxFeedbackLines();
        this.maxLineLenght = limits.getMaxLineLen();
        this.lineTruncatedMessage = limits.getLineTruncatedMessage();
        this.outputTruncMessage = limits.getOutputTruncMessage();
    }

    @Override
    protected void processLine(String line) {
        if (lineCount < maxLines) {
            final int maxAppendFromBufferSize = min(line.length(), maxSize - buffer.length() + 1);
            final int maxAppendFromLineLimit = min(maxAppendFromBufferSize, maxLineLenght);
            if (maxAppendFromLineLimit > 0) {
                final boolean isLineTruncated = maxAppendFromLineLimit < line.length();
                if (isLineTruncated) {
                    buffer.append(line, 0, maxAppendFromLineLimit).append(lineTruncatedMessage);
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
