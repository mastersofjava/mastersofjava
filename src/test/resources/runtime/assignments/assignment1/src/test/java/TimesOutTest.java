import static org.hamcrest.CoreMatchers.equalTo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;


public class TimesOutTest {

	@Test
	public void timout() throws Exception {
		Thread.sleep(TimeUnit.SECONDS.toMillis(60));
	}
}
