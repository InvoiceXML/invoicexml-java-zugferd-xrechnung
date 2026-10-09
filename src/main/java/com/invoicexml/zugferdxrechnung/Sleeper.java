package com.invoicexml.zugferdxrechnung;

import java.time.Duration;

/** Waits between retries. Replaceable in tests. */
@FunctionalInterface
interface Sleeper {

    Sleeper SYSTEM = d -> Thread.sleep(d.toMillis());

    void sleep(Duration duration) throws InterruptedException;
}
