package org.activityinfo.ui.full.client.dispatch.remote.cache;

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

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.activityinfo.legacy.shared.command.*;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.model.SchemaDTO;
import org.activityinfo.ui.full.client.dispatch.CommandCache;
import org.activityinfo.ui.full.client.dispatch.DispatchEventSource;
import org.activityinfo.ui.full.client.dispatch.DispatchListener;

import java.util.Set;

/**
 * Caches the user's schema in-memory for the duration of the session.
 * <p/>
 * TODO: we need to peridiodically check the server for updates. Do we do this
 * here or in a separate class?
 *
 * @author Alex Bertram
 */
public class SchemaCache implements CommandCache<GetSchema>, DispatchListener {

    private SchemaDTO schema = null;

    private Set<String> schemaEntityTypes = Sets.newHashSet();

    @Inject
    public SchemaCache(DispatchEventSource source) {

        source.registerProxy(GetSchema.class, this);
        source.registerListener(GetSchema.class, this);
        source.registerListener(UpdateEntity.class, this);
        source.registerListener(CreateEntity.class, this);
        source.registerListener(AddPartner.class, this);
        source.registerListener(RemovePartner.class, this);
        source.registerListener(RequestChange.class, this);

        schemaEntityTypes.add("UserDatabase");
        schemaEntityTypes.add("Activity");
        schemaEntityTypes.add("Indicator");
        schemaEntityTypes.add("AttributeGroup");
        schemaEntityTypes.add("AttributeDimension");
        schemaEntityTypes.add("Partner");
        schemaEntityTypes.add("Project");
        schemaEntityTypes.add("LockedPeriod");

    }

    @Override
    public CacheResult maybeExecute(GetSchema command) {
        if (schema == null) {
            return CacheResult.couldNotExecute();
        } else {
            return new CacheResult(schema);
        }
    }

    @Override
    public void beforeDispatched(Command command) {
        if (command instanceof UpdateEntity
                && isSchemaEntity(((UpdateEntity) command).getEntityName())) {
            schema = null;
        } else if (command instanceof CreateEntity
                && isSchemaEntity(((CreateEntity) command).getEntityName())) {
            schema = null;
        } else if (command instanceof AddPartner ||
                command instanceof RemovePartner) {
            schema = null;
        } else if (command instanceof RequestChange &&
                isSchemaEntity(((RequestChange) command).getEntityType())) {
        }
    }

    private boolean isSchemaEntity(String entityName) {
        return schemaEntityTypes.contains(entityName);
    }

    @Override
    public void onSuccess(Command command, CommandResult result) {
        if (command instanceof GetSchema) {
            cache((SchemaDTO) result);
        } else if (schema != null) {
            if (command instanceof AddPartner) {
            }
        }
    }

    /**
     * Caches the schema in-memory following a successful GetSchema call.
     * Subclasses can override this to provide a more permanent cache.
     *
     * @param schema The schema to cache
     */
    protected void cache(SchemaDTO schema) {
        this.schema = schema;
    }

    @Override
    public void onFailure(Command command, Throwable caught) {

    }

    @Override
    public void clear() {
        schema = null;
    }
}
