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
package nl.moj.server.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollectionUtil {

    public static <T> List<List<T>> partition(final List<T> ls, final int partitions) {
        final List<List<T>> lsParts = new ArrayList<>();
        final int iChunkSize = ls.size() / partitions;
        int iLeftOver = ls.size() % partitions;
        int iTake;

        for (int i = 0, iT = ls.size(); i < iT; i += iTake) {
            if (iLeftOver > 0) {
                iLeftOver--;
                iTake = iChunkSize + 1;
            } else {
                iTake = iChunkSize;
            }

            lsParts.add(new ArrayList<T>(ls.subList(i, Math.min(iT, i + iTake))));
        }
        while (lsParts.size() < partitions) {
            lsParts.add(Collections.emptyList());
        }
        return lsParts;
    }
}
