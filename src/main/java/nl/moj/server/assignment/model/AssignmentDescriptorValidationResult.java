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
package nl.moj.server.assignment.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;

@Getter
@AllArgsConstructor
public class AssignmentDescriptorValidationResult {

    private final String assignment;
    private final AssignmentDescriptor assignmentDescriptor;
    private final List<String> validationMessages = new ArrayList<>();

    public void addValidationMessage(String message) {
        validationMessages.add(message);
    }

    public boolean isValid() {
        return assignmentDescriptor != null && validationMessages.isEmpty();
    }
}
