package org.activityinfo.test.capacity.action;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Sampling {
    
    
    public static <T> T chooseOne(List<T> items) {
        int index = ThreadLocalRandom.current().nextInt(0, items.size() - 1);
        return items.get(index);
    }
}
