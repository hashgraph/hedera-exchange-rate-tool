package com.hedera.services.exchange;

import com.hedera.services.exchange.ERTproc;
import org.junit.jupiter.api.Test;

public class ERTprocTestCases {

    public ERTproc ertProcess = new ERTproc("0", null, "0", "0", 0.0,
            0.0, 0.0, "0");

    @Test
    public void testMedian(){
        ertProcess.call();
    }
}
