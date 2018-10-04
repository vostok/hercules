package ru.kontur.vostok.hercules.util.fsm;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class FsmTest {

    private enum S implements State {
        A,
        B,
        C,
        ;
    }

    @Test
    public void shouldWaitForFirstStateChange() throws Exception {
        Fsm<S> fsm = new Fsm<>(S.C);

        Thread thread = new Thread(() -> {
            sleep(10);

            fsm.makeTransition(Collections.singletonMap(S.C, S.B));
            fsm.makeTransition(Collections.singletonMap(S.B, S.A));
        });

        thread.start();

        assertEquals(S.B, fsm.waitStateChange());

        thread.join();
    }

    @Test
    public void shouldWaitForSpecificState() throws Exception {
        Fsm<S> fsm = new Fsm<>(S.C);

        Thread thread = new Thread(() -> {
            sleep(10);

            fsm.makeTransition(Collections.singletonMap(S.C, S.B));
            fsm.makeTransition(Collections.singletonMap(S.B, S.C));
            fsm.makeTransition(Collections.singletonMap(S.C, S.B));
            fsm.makeTransition(Collections.singletonMap(S.B, S.A));
        });

        thread.start();

        assertEquals(S.A, fsm.waitForState(S.A));

        thread.join();
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
