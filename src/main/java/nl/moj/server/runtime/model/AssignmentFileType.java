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

public enum AssignmentFileType {
    TEST(true, false),
    HIDDEN_TEST(false, true),
    EDIT(true, false),
    READONLY(true, false),
    HIDDEN(false, true),
    TASK(true, false),
    SOLUTION(false, true),
    RESOURCE(true, false),
    TEST_RESOURCE(true, false),
    HIDDEN_TEST_RESOURCE(false, true),
    INVISIBLE_TEST(true, true),
    INVISIBLE_TEST_RESOURCE(true, true);

    private boolean visible;
    private boolean contentHidden;

    AssignmentFileType(boolean visible, boolean contentHidden) {
        this.visible = visible;
        this.contentHidden = contentHidden;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public boolean isContentHidden() {
        return this.contentHidden;
    }
}
