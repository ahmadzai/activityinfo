package org.activityinfo.server.command.handler;

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

import com.google.inject.Inject;
import org.activityinfo.legacy.shared.command.CreateLockedPeriod;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.command.result.CreateResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.legacy.shared.model.LockedPeriodDTO;
import org.activityinfo.server.database.hibernate.entity.*;

import javax.persistence.EntityManager;
import java.util.Date;

public class CreateLockedPeriodHandler implements
        CommandHandler<CreateLockedPeriod> {
    private EntityManager em;

    @Inject
    public CreateLockedPeriodHandler(EntityManager em) {
        this.em = em;
    }

    @Override
    public CommandResult execute(CreateLockedPeriod cmd, User user)
            throws CommandException {

        Activity activity = null;
        UserDatabase database = null;
        Project project = null;

        LockedPeriod lockedPeriod = new LockedPeriod();
        LockedPeriodDTO lockedPeriodDTO = cmd.getLockedPeriod();
        lockedPeriod.setFromDate(lockedPeriodDTO.getFromDate()
                .atMidnightInMyTimezone());
        lockedPeriod.setToDate(lockedPeriodDTO.getToDate()
                .atMidnightInMyTimezone());
        lockedPeriod.setName(lockedPeriodDTO.getName());
        lockedPeriod.setEnabled(lockedPeriodDTO.isEnabled());

        int databaseId;
        if (cmd.getUserDatabseId() != 0) {
            database = em.find(UserDatabase.class, cmd.getUserDatabseId());
            lockedPeriod.setUserDatabase(database);
            databaseId = database.getId();
        } else if (cmd.getProjectId() != 0) {
            project = em.find(Project.class, cmd.getProjectId());
            lockedPeriod.setProject(project);
            databaseId = project.getUserDatabase().getId();
        } else if (cmd.getActivityId() != 0) {
            activity = em.find(Activity.class, cmd.getActivityId());
            lockedPeriod.setActivity(activity);
            databaseId = activity.getDatabase().getId();
        } else {
            throw new CommandException(
                    "One of the following must be provdied: userDatabaseId, projectId, activityId");
        }

        UserDatabase db = em.find(UserDatabase.class, databaseId);

        em.persist(lockedPeriod);

        db.setLastSchemaUpdate(new Date());
        em.persist(db);

        if (database != null) {
            database.getLockedPeriods().add(lockedPeriod);
        }
        if (project != null) {
            project.getLockedPeriods().add(lockedPeriod);
        }
        if (activity != null) {
            activity.getLockedPeriods().add(lockedPeriod);
        }

        return new CreateResult(lockedPeriod.getId());
    }

}
