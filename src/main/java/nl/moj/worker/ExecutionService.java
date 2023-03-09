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
package nl.moj.worker;

import java.util.concurrent.Executor;

import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.common.assignment.descriptor.ExecutionModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExecutionService {

    private final Executor sequential;
    private final Executor parallel;

    public ExecutionService(@Qualifier("sequential") Executor sequential, @Qualifier("parallel") Executor parallel) {
        this.sequential = sequential;
        this.parallel = parallel;
    }

    public Executor getExecutor(AssignmentDescriptor ad) {
        if (ad.getExecutionModel() == ExecutionModel.SEQUENTIAL) {
            return sequential;
        }
        return parallel;
    }
}
