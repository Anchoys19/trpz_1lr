package dm.core;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public class FilteredTaskIterable implements Iterable<DownloadTask> {
    private final TaskIterable base;
    private final Predicate<DownloadTask> predicate;

    public FilteredTaskIterable(TaskIterable base, Predicate<DownloadTask> predicate) {
        this.base = base;
        this.predicate = predicate;
    }

    @Override
    public Iterator<DownloadTask> iterator() {
        Iterator<DownloadTask> it = base.iterator();
        return new Iterator<>() {
            private DownloadTask next;

            @Override
            public boolean hasNext() {
                while (next == null && it.hasNext()) {
                    DownloadTask cand = it.next();
                    if (predicate.test(cand)) {
                        next = cand;
                        break;
                    }
                }
                return next != null;
            }

            @Override
            public DownloadTask next() {
                if (!hasNext()) throw new NoSuchElementException();
                DownloadTask out = next;
                next = null;
                return out;
            }
        };
    }
}
