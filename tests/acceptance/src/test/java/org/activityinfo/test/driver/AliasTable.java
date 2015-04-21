package org.activityinfo.test.driver;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import cucumber.api.DataTable;
import cucumber.runtime.java.guice.ScenarioScoped;
import gherkin.formatter.model.DataTableRow;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Maintains a mapping between human-readable "test handles" and their unique aliases
 * that are actually used in interacting with the system. This helps isolate tests 
 * running on the same server from each other.
 * 
 * For example, two different tests might refer to "NFI Distribution", or we might run the 
 * same test on several browsers against the same server. If we actually used the name
 * "NFI Distribution", tests run against the UI would not be reliable. 
 * 
 * To avoid this, we decorate each human-readable "test handle" with a random hex string, so
 * that when we see "NFI Distribution_ADc323434" we know that is the object that we've 
 * created in this specific run of the test.
 */
@ScenarioScoped
public class AliasTable {

    private final String uniqueSuffix;

    private ConcurrentHashMap<String, Supplier<Integer>> testHandleToId = new ConcurrentHashMap<>();
    

    public AliasTable() {
        this.uniqueSuffix = "_" + Long.toHexString(ThreadLocalRandom.current().nextLong());
    }

    /**
     * Gets a randomized name for a domain object given
     * the friendly test-facing name
     */
    public String createAlias(String testHandle) {
        Preconditions.checkNotNull(testHandle, "testHandle");

        return testHandle + uniqueSuffix;
    }

    public String createAliasIfNotExists(String testHandle) {
        return createAlias(testHandle);
    }

    public int getId(String testHandle) {
        Supplier<Integer> id = testHandleToId.get(testHandle);
        if(id == null) {
            throw missingHandle("Test handle '%s' has not been bound to an id.", testHandle);
        }
        return id.get();
    }

    private IllegalStateException missingHandle(String message, Object... arguments) {
        StringBuilder s = new StringBuilder();
        s.append(String.format(message, arguments));
        s.append("\n");
        s.append("Test handles:\n");
        for (String handle : testHandleToId.keySet()) {
            s.append(String.format("  %s [%s] = %d\n", handle, createAlias(handle),
                    testHandleToId.get(handle).get()));
        }
        return new IllegalStateException(s.toString());
    }

    public int generateIdFor(String testHandle) {
        Preconditions.checkNotNull(testHandle, "testHandle");

        int id = generateId();
        bindTestHandleToId(testHandle, Suppliers.ofInstance(id));
        return id;
    }

    public int generateId() {
        return ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE - 1);
    }

    /**
     * Maps an alias to its server-generated ID
     */
    public void bindAliasToId(String alias, Supplier<Integer> id) {
        Preconditions.checkNotNull(alias, "alias");
            bindTestHandleToId(getTestHandleForAlias(alias), id);
    }

    public void bindTestHandleToIdIfAbsent(String handle, Supplier<Integer> newId) {
        Preconditions.checkNotNull(handle, "handle");
        testHandleToId.putIfAbsent(handle, newId);
    }
    
    /**
     * Maps a test handle to its server-generated ID
     */
    public void bindTestHandleToId(String handle, Supplier<Integer> newId) {
        Preconditions.checkNotNull(handle, "handle");

        Supplier<Integer> existingId = testHandleToId.putIfAbsent(handle, newId);
        
        if(existingId != null) {
            if (!Objects.equals(existingId.get(), newId.get())) {
                throw new IllegalStateException(String.format(
                        "Cannot bind test handle %s to id %d: it was previously bound to %d", handle,
                        newId.get(),
                        existingId.get()));
            }
        }
    }
    
    public void bindTestHandleToId(String handle, int newId) {
        bindTestHandleToId(handle, Suppliers.ofInstance(newId));
    }
    
    /**
     * 
     * @param alias the uniquely decorated name
     * @return the human-readable test alias
     */
    public String getTestHandleForAlias(String alias) {
        if(alias.endsWith(uniqueSuffix)) {
            return alias.substring(0, alias.length() - uniqueSuffix.length());
        } else {
            throw missingHandle("Cannot find a test handle for the alias '%s'", alias);
        }
    }

    public boolean isAlias(String alias) {
        return alias.endsWith(uniqueSuffix);
    }
    
    public String getAlias(String testHandle) {
        return createAlias(testHandle);
    }

    /**
     * Replaces strongly named objects in the given table with their 
     * human-friendly test handles
     * 
     * <p>For example, if your test has a partner called "NRC", the the
     * test driver will actually create a partner called something like
     * "NRC_5e9823d88d563417" to ensure that any existing state does not
     * interfere with the current test run.
     * 
     * <p>This method will replace "NRC_5e9823d88d563417" with "NRC" so that
     * the output can be presented back to the user.</p>
     */
    public DataTable deAlias(DataTable table) {
        List<List<String>> rows = Lists.newArrayList();
        for (DataTableRow row : table.getGherkinRows()) {
            List<String> cells = Lists.newArrayList();
            for (String cell : row.getCells()) {
                if(isAlias(cell)) {
                    cells.add(getTestHandleForAlias(cell));
                } else {
                    cells.add(cell.trim());
                }
            }
            rows.add(cells);
        }
        return DataTable.create(rows);
    }


    /**
     * @return a random 32-bit integer key
     */
    public int generateInt() {
        return ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
    }
    
    public int getOrGenerateId(String testHandle) {
        int newId = generateId();
        Supplier<Integer> existingId = testHandleToId.putIfAbsent(testHandle, Suppliers.ofInstance(newId));
        if(existingId == null) {
            return newId;
        } else  {
            return existingId.get();
        }
    }

    public Map<String, Supplier<Integer>> getTestHandleToId() {
        return Maps.newHashMap(testHandleToId);
    }
}