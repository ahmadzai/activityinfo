package org.activityinfo.core.client;
/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.common.annotations.GwtCompatible;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.promise.Promise;

import java.util.ArrayList;
import java.util.List;

/**
 * GWT compatible promise matchers.
 *
 * @author yuriyz on 03/26/2015.
 */
@GwtCompatible
public class GwtPromiseMatchers {

    private GwtPromiseMatchers() {
    }

    // copy-paste because "org.hamcrest" in PromiseMatchers is not in emulation library
    public static <T> T assertResolves(Promise<T> promise) {
        final List<T> results = new ArrayList<>();
        promise.then(new AsyncCallback<T>() {
            @Override
            public void onFailure(Throwable caught) {
                throw new RuntimeException(caught);
            }

            @Override
            public void onSuccess(T result) {
                // no problems
                results.add(result);
            }
        });
        if (results.size() > 1) {
            throw new RuntimeException("Callback called " + results.size() + " times, expected exactly one callback.");
        }
        if (results.isEmpty()) {
            throw new RuntimeException("Callback not called, expected exactly one callback.");
        }
        return results.get(0);
    }
}
