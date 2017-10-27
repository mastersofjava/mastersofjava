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
		while( lsParts.size() < partitions ) {
			lsParts.add(Collections.emptyList());
		}
		return lsParts;
	}
}
