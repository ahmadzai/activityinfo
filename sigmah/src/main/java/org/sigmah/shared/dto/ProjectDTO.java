package org.sigmah.shared.dto;

import java.util.HashSet;
import java.util.Set;

import com.extjs.gxt.ui.client.data.BaseModelData;

public class ProjectDTO extends BaseModelData implements DTO {
    private Set<LockedPeriodDTO> lockedPeriods = new HashSet<LockedPeriodDTO>(0);
    private UserDatabaseDTO userDatabase;

    public ProjectDTO() {
		super();
	}

	public ProjectDTO(int id, String name) {
		super();
		
		setId(id);
		setName(name);
	} 

	public int getId() {
		return (Integer)get("id");
	}
	
	public void setId(int id) {
		set("id", id); 
	}
	
	public String getName() {
		return (String)get("name");
	}
	
	public void setName(String name) {
		set("name", name);
	}
	
	public void setDescription(String description) {
		set("description", description);
	}

	public String getDescription() {
		return (String)get("description");
	}
	
	public Set<LockedPeriodDTO> getLockedPeriods() {
//		Set<LockedPeriodDTO> allLockedPeriods = new HashSet<LockedPeriodDTO>();
//		allLockedPeriods.addAll(lockedPeriods);
//		if (getDatabase() != null && getDatabase().getLockedPeriods() != null) {
//			allLockedPeriods.addAll(getDatabase().getLockedPeriods());
//		}
		return lockedPeriods;
	}
	
	public void setLockedPeriods(Set<LockedPeriodDTO> lockedPeriods) {
		this.lockedPeriods = lockedPeriods;
	}

	public void setUserDatabase(UserDatabaseDTO database) {
		this.userDatabase = database;
	}

	public UserDatabaseDTO getUserDatabase() {
		return userDatabase;
	}
}