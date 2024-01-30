package nl.moj.server.util;

import java.util.function.Supplier;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

@Service
public class TransactionHelper {

    @Transactional
    public <T> T required(Supplier<T> supplier) {
        return supplier.get();
    }

    @Transactional
    public void required(Runnable runnable) {
        runnable.run();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public <T> T requiresNew(Supplier<T> supplier) {
        return supplier.get();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void requiresNew(Runnable runnable) {
        runnable.run();
    }

}
