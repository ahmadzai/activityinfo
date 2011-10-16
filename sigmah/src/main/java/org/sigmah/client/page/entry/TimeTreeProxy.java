package org.sigmah.client.page.entry;

import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.sigmah.client.dispatch.Dispatcher;
import org.sigmah.client.page.entry.SiteTreeGridPageState.TreeType;
import org.sigmah.shared.command.GetAdminEntities;
import org.sigmah.shared.command.GetSites;
import org.sigmah.shared.command.SitesPerAdminEntity;
import org.sigmah.shared.command.SitesPerAdminEntity.SitesPerAdminEntityResult;
import org.sigmah.shared.command.SitesPerAdminEntity.SitesPerAdminEntityResult.AmountPerAdminEntity;
import org.sigmah.shared.command.SitesPerTime;
import org.sigmah.shared.command.SitesPerTime.SitesPerTimeResult;
import org.sigmah.shared.command.SitesPerTime.SitesPerTimeResult.MonthResult;
import org.sigmah.shared.command.SitesPerTime.SitesPerTimeResult.YearResult;
import org.sigmah.shared.command.result.SiteResult;
import org.sigmah.shared.dao.Filter;
import org.sigmah.shared.dto.AdminEntityDTO;
import org.sigmah.shared.dto.SiteDTO;
import org.sigmah.shared.report.model.DimensionType;

import com.bedatadriven.rebar.time.calendar.LocalDate;
import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.DataProxy;
import com.extjs.gxt.ui.client.data.DataReader;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.SortInfo;
import com.google.common.collect.Lists;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class TimeTreeProxy implements DataProxy<List<ModelData>> {
		private Dispatcher service;
		private SiteTreeGridPageState place;
		private Filter filter;
		private SitesPerTimeResult result;
		private SitesPerAdminEntityResult sitesPerAdminEntityResult;
		private final static int treshold = 25; // maximum sites for a parent 

		public TimeTreeProxy(Dispatcher service) {
			super();
			this.service = service;
		}

		@Override
		public void load(DataReader<List<ModelData>> reader, Object parent,
				final AsyncCallback<List<ModelData>> callback) {
			if (place.getTreeType() == TreeType.GEO) {
				getDataForGeo(parent, callback);
			}
			if (place.getTreeType() == TreeType.TIME) {
				getDataForTime(parent, callback);
			}
		}
		
		private void setResult(SitesPerTimeResult result) {
			this.result=result;
		}
		
		private void getDataForTime(Object parent, final AsyncCallback<List<ModelData>> callback) {
			// Show the years as root
			if (parent == null) {
				showYears(callback);
			}
			
			// Show months as child for years
			if (parent instanceof YearViewModel) {
				showMonths(parent, callback);
			}
			
			// Show sites as children for month
			if (parent instanceof MonthViewModel) {
				showMonthChildren(parent, callback);
			}
		}
		
		private void getDataForGeo(Object parent, final AsyncCallback<List<ModelData>> callback) {
			// No AdminEntities available yet, grab them first 
			if (parent == null) {
				grabAdminEntities(callback);
			}
			
			// User clicked "Show more AdminEntities
			if (parent instanceof ShowSitesViewModel) {
				showAllAdminEntityChildren(parent, callback);
			}
			
			// User drills down on an AdminEntity
			if (parent instanceof AdminViewModel) {
				grabAdminEntityChildren(parent, callback);
			}
		}

		private void showMonths(Object parent,
				final AsyncCallback<List<ModelData>> callback) {
			YearViewModel year = (YearViewModel) parent;
			callback.onSuccess(createMonths(year.getYear()));
		}

		private void showYears(final AsyncCallback<List<ModelData>> callback) {
			List<ModelData> yearsAndProvinces = Lists.newArrayList();
			service.execute(new SitesPerTime(place.getActivityId()), null, new AsyncCallback<SitesPerTimeResult>() {
				@Override
				public void onFailure(Throwable caught) {
					callback.onFailure(caught);
				}
				@Override
				public void onSuccess(SitesPerTimeResult result) {
					setResult(result);
					callback.onSuccess(createYears(result.getYears()));
				}
			});
		}

		/** Grabs children of target month */
		private void showMonthChildren(Object parent, final AsyncCallback<List<ModelData>> callback) {
			MonthViewModel month = (MonthViewModel) parent;
			GetSites getSites = new GetSites();
			filter.setMinDate(new Date(month.getYear(), month.getMonth() + 1, 1));
			filter.setMaxDate(new Date(month.getYear(), month.getMonth() + 1, 28));
			getSites.setFilter(filter);
			getSites.setSortInfo(new SortInfo("date2", SortDir.DESC));
			service.execute(getSites, null, new AsyncCallback<SiteResult>() {
				@Override
				public void onFailure(Throwable caught) {
					callback.onFailure(caught);
				}

				@Override
				public void onSuccess(SiteResult result) {
					List<ModelData> sites = Lists.newArrayList();
					for (SiteDTO site : result.getData()) {
						sites.add(site);
						site.set("name", DateTimeFormat.getFormat("dd, EEEE").format(fromLocal(site.getDate2())));
						//site.set("name", "");
					}
					callback.onSuccess(sites);
				}
			});
		}
		
		private Date fromLocal(LocalDate localDate) {
			return new Date(localDate.getYear(), localDate.getMonthOfYear(), localDate.getDayOfMonth());
		}

		/** Grabs children of target AdminEntity */
		private void grabAdminEntityChildren(Object parent,final AsyncCallback<List<ModelData>> callback) {
			AdminViewModel adminEntityViewModelParent = (AdminViewModel) parent;
			AdminEntityDTO adminEntityParent = adminEntityViewModelParent.getAdminEntity();
			
			final List<ModelData> adminlevels = Lists.newArrayList();
			for (Entry<Integer, AmountPerAdminEntity> entry : sitesPerAdminEntityResult.getAdminEntitiesById().entrySet()) {
				if (entry.getValue().getAdminEntity().getParentId() != null && entry.getValue().getAdminEntity().getParentId() == adminEntityParent.getId()) {
					adminlevels.add(new AdminViewModel(entry.getValue().getAdminEntity(), entry.getValue().getAmountSites()));
				}
			}
			if (adminEntityViewModelParent.getAmountSites() < treshold) {
				getSitesByAdminEntity(callback, adminEntityParent.getId(), adminlevels);
			} else {
				ModelData showMore = new ShowSitesViewModel(adminEntityViewModelParent.getAmountSites(), adminEntityParent);
				adminlevels.add(showMore);
				callback.onSuccess(adminlevels);
			}
		}

		/** Grabs a list of AdminEntities with amount of sites contained by them */
		private void grabAdminEntities(final AsyncCallback<List<ModelData>> callback) {
			GetAdminEntities getAdminEntities = new GetAdminEntities();
			getAdminEntities.setFilter(filter);
			service.execute(new SitesPerAdminEntity(place.getActivityId()), null, new AsyncCallback<SitesPerAdminEntityResult>() {
				@Override
				public void onFailure(Throwable caught) {
					callback.onFailure(caught);
				}
				@Override
				public void onSuccess(SitesPerAdminEntityResult result) {
					TimeTreeProxy.this.sitesPerAdminEntityResult = result;
					List<ModelData> adminlevels = Lists.newArrayList();
					for (Entry<Integer, AmountPerAdminEntity> entry : result.getAdminEntitiesById().entrySet()) {
						if (entry.getValue().getAdminEntity().getParentId() == null) {
							adminlevels.add(new AdminViewModel(entry.getValue().getAdminEntity(), entry.getValue().getAmountSites()));
						}
					}
					callback.onSuccess(adminlevels);
				}
			});
		}

		/** Displays _all_ sites of target AdminEntity in a drilldown parent */
		private void showAllAdminEntityChildren(Object parent, final AsyncCallback<List<ModelData>> callback) {
			ShowSitesViewModel showSites = (ShowSitesViewModel) parent;
			final List<ModelData> adminlevels = Lists.newArrayList();
			getSitesByAdminEntity(callback, showSites.getAdminEntityId(), adminlevels);
		}

		/** Grabs all sites beloning to a target AdminEntity */
		private void getSitesByAdminEntity(
				final AsyncCallback<List<ModelData>> callback,
				Integer adminEntityParentId,
				final List<ModelData> adminlevels) {
			
			GetSites getSites = new GetSites();
			Filter filter = new Filter();
			filter.addRestriction(DimensionType.AdminLevel, adminEntityParentId);
			filter.addRestriction(DimensionType.Activity, place.getActivityId());
			getSites.setFilter(filter);
			
			service.execute(getSites, null, new AsyncCallback<SiteResult>() {
				@Override
				public void onFailure(Throwable caught) {
					callback.onFailure(caught);
				}
				@Override
				public void onSuccess(SiteResult result) {
					for (SiteDTO site : result.getData()) {
						adminlevels.add(site);
					}
					callback.onSuccess(adminlevels);
				}
			});
		}

		private List<ModelData> createMonths(int year) {
			List<ModelData> months = Lists.newArrayList();
			YearResult yearResult = byYear(result.getYears(), year);
			for (MonthResult month : yearResult.getMonths()) {
				months.add(new MonthViewModel()
					.setName(DateTimeFormat.getFormat("MMMM yyyy").format(new Date(year -1900, month.getMonth() -1, 1)) + " " + "(" + month.getAmountSites() + ")")
					.setYear(year)
					.setMonth(month.getMonth() - 1));
			}
			return months;
		}
		
		private YearResult byYear(List<YearResult> years, int year) {
			for (YearResult yearResult : years) {
				if (year == yearResult.getYear()) {
					return yearResult;
				}
			}
			return null;
		}

		private List<ModelData> createYears(List<YearResult> yearResults) {
			List<ModelData> years= Lists.newArrayList();
			
			for (YearResult year : yearResults) {
				years.add(new YearViewModel()
					.setName(Integer.toString(year.getYear()))
					.setYear(year.getYear()));
			}
			return years;
		}
		
		public SiteTreeGridPageState getPlace() {
			return place;
		}
		public void setPlace(SiteTreeGridPageState place) {
			this.place = place;
		}

		public void setFilter(Filter filter) {
			this.filter=filter;
		}

		/** True when year, month, admin of "show all adminlevels" is selected */
		public boolean hasChildren(ModelData parent) {
			return (parent instanceof YearViewModel   || 
				parent instanceof MonthViewModel      ||
				parent instanceof AdminViewModel      ||
				parent instanceof ShowSitesViewModel);
		}
	}