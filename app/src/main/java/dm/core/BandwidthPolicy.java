package dm.core;

import java.util.concurrent.atomic.AtomicLong;

public class BandwidthPolicy {
    private final AtomicLong limitBps = new AtomicLong(0); // 0 = unlimited
    public void setLimit(long bps) { limitBps.set(Math.max(0, bps)); }
    public long getLimit() { return limitBps.get(); }
}
