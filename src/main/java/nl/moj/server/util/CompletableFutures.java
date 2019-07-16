package nl.moj.server.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CompletableFutures {

    private CompletableFutures() {}

    public static <T>  CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply( v ->
                futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
    }

}
