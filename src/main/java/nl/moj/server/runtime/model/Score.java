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
package nl.moj.server.runtime.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder(toBuilder = true)
@Getter
@ToString
public class Score {

    @Builder.Default
    private final Long initialScore = 0L;
    @Builder.Default
    private final Long submitBonus = 0L;
    @Builder.Default
    private final Long resubmitPenalty = 0L;
    @Builder.Default
    private final Long testPenalty = 0L;
    @Builder.Default
    private final Long testBonus = 0L;

    public Long getTotalScore() {
        Long score = initialScore + submitBonus + testBonus - resubmitPenalty - testPenalty;
        if (score < 0) {
            return 0L;
        }
        return score;
    }

    public Long getTotalBonus() { return submitBonus + testBonus; }

    public Long getTotalPenalty() {
        return resubmitPenalty + testPenalty;
    }
}
