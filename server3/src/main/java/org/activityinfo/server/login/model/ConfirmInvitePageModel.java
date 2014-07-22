package org.activityinfo.server.login.model;

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

import org.activityinfo.server.database.hibernate.entity.User;

public class ConfirmInvitePageModel extends PageModel {
    private User user;
    private boolean formIncomplete;

    public ConfirmInvitePageModel(User user) {
        this.setUser(user);
    }

    public static ConfirmInvitePageModel incompleteForm(User user) {
        ConfirmInvitePageModel model = new ConfirmInvitePageModel(user);
        model.setFormIncomplete(true);
        return model;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isFormIncomplete() {
        return formIncomplete;
    }

    public void setFormIncomplete(boolean formIncomplete) {
        this.formIncomplete = formIncomplete;
    }
}