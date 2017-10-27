package nl.moj.server;

import nl.moj.server.util.CollectionUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
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
