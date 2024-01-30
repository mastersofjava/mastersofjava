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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Score {

    private Long initialScore = 0L;
    private Long submitBonus = 0L;
    private Long resubmitPenalty = 0L;
    private Long testPenalty = 0L;
    private Long testBonus = 0L;
    private String explanation;

    public Long getTotalScore() {
        Long score = initialScore + submitBonus + testBonus - resubmitPenalty - testPenalty;
        if (score < 0) {
            return 0L;
        }
        return score;
    }

    public Long getTotalBonus() {
        return submitBonus + testBonus;
    }

    public Long getTotalPenalty() {
        return resubmitPenalty + testPenalty;
    }

    public void addExplanation(String explanation) {
        if (this.explanation == null) {
            this.explanation = "";
        }
        this.explanation = this.explanation + "<li>" + explanation + "</li>";
    }

    public String getExplanation() {
        return "<ul>" + explanation + "</ul>";
    }
}
