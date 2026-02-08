package org.kiwi.console.kiwi;

import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.TestUtil;
import org.kiwi.console.util.Utils;

@Slf4j
public class ExchangeClientTest extends TestCase {

    private ExchangeClient exchangeClient;

    public static final String ID = "01f69d8bba0700";

    @Override
    protected void setUp() {
        exchangeClient = TestUtil.createKiwiFeignClient(ExchangeClient.class);
    }

    public void test() {
        var exch = exchangeClient.get(ID);
        var task = exch.getTasks().getFirst();
        var attempts = task.getAttempts().size();
        task.addAttempt(Attempt.create());
        exchangeClient.update(ID, exch);

        log.debug("Task ID: {}", task.getId());

        var exch1 = exchangeClient.get(ID);

        log.debug("{}", Utils.toPrettyJSONString(exch1));
        var task1 = Utils.find(exch1.getTasks(), t -> t.getId().equals(task.getId()));
        assertNotNull(task1);
        assertEquals(attempts + 1, task1.getAttempts().size());
    }

}
