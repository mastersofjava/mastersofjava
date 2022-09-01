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
package nl.moj.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nl.moj.server.util.CollectionUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CollectionUtilTest {

    @Test
    public void test0Items() {
        List<List<String>> items = CollectionUtil.partition(Collections.emptyList(), 3);
        assertThat(items.size()).isEqualTo(3);
        assertThat(items.get(0).size()).isEqualTo(0);
        assertThat(items.get(1).size()).isEqualTo(0);
        assertThat(items.get(2).size()).isEqualTo(0);
    }

    @Test
    public void test1Items() {
        List<List<String>> items = CollectionUtil.partition(Collections.singletonList("test"), 3);
        assertThat(items.size()).isEqualTo(3);
        assertThat(items.get(0).size()).isEqualTo(1);
        assertThat(items.get(1).size()).isEqualTo(0);
        assertThat(items.get(2).size()).isEqualTo(0);
    }

    @Test
    public void test2Items() {
        List<List<String>> items = CollectionUtil.partition(Arrays.asList("test1", "test2"), 3);
        assertThat(items.size()).isEqualTo(3);
        assertThat(items.get(0).size()).isEqualTo(1);
        assertThat(items.get(1).size()).isEqualTo(1);
        assertThat(items.get(2).size()).isEqualTo(0);
    }

    @Test
    public void test3Items() {
        List<List<String>> items = CollectionUtil.partition(Arrays.asList("test1", "test2", "test3"), 3);
        assertThat(items.size()).isEqualTo(3);
        assertThat(items.get(0).size()).isEqualTo(1);
        assertThat(items.get(1).size()).isEqualTo(1);
        assertThat(items.get(2).size()).isEqualTo(1);
    }

    @Test
    public void test4Items() {
        List<List<String>> items = CollectionUtil.partition(Arrays.asList("test1", "test2", "test3", "test4"), 3);
        assertThat(items.size()).isEqualTo(3);
        assertThat(items.get(0).size()).isEqualTo(2);
        assertThat(items.get(1).size()).isEqualTo(1);
        assertThat(items.get(2).size()).isEqualTo(1);
    }

    @Test
    public void test5Items() {
        List<List<String>> items = CollectionUtil.partition(Arrays.asList("test1", "test2", "test3", "test4", "test5"), 3);
        assertThat(items.size()).isEqualTo(3);
        assertThat(items.get(0).size()).isEqualTo(2);
        assertThat(items.get(1).size()).isEqualTo(2);
        assertThat(items.get(2).size()).isEqualTo(1);
    }

    @Test
    public void test6Items() {
        List<List<String>> items = CollectionUtil.partition(Arrays.asList("test1", "test2", "test3", "test4", "test5", "test6"), 3);
        assertThat(items.size()).isEqualTo(3);
        assertThat(items.get(0).size()).isEqualTo(2);
        assertThat(items.get(1).size()).isEqualTo(2);
        assertThat(items.get(2).size()).isEqualTo(2);
    }
}
