/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.business.teamcal.event;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.teamcal.TeamCalConfig;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.teamcal.externalsubscription.TeamEventExternalSubscriptionCache;
import org.projectforge.business.teamcal.service.TeamCalService;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.calendar.CalendarUtils;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class TeamEventDao extends BaseDao<TeamEventDO>
{
  public static final long MIN_DATE_1800 = -5364662400000L;

  public static final long MAX_DATE_3000 = 32535216000000L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamEventDao.class);

  private static final long ONE_DAY = 1000 * 60 * 60 * 24;

  private static final Class<?>[] ADDITIONAL_HISTORY_SEARCH_DOS = new Class[] { TeamEventAttendeeDO.class };

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "subject", "location", "calendar.id",
      "calendar.title", "note",
      "attendees" };

  @Autowired
  private TeamCalDao teamCalDao;

  @Autowired
  private TeamCalCache teamCalCache;

  @Autowired
  private TeamEventExternalSubscriptionCache teamEventExternalSubscriptionCache;

  @Autowired
  private TeamCalService teamCalService;

  public TeamEventDao()
  {
    super(TeamEventDO.class);
    userRightId = UserRightId.PLUGIN_CALENDAR_EVENT;
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * @param teamEvent
   * @param teamCalendarId If null, then team calendar will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setCalendar(final TeamEventDO teamEvent, final Integer teamCalendarId)
  {
    final TeamCalDO teamCal = teamCalDao.getOrLoad(teamCalendarId);
    teamEvent.setCalendar(teamCal);
  }

  public TeamEventDO getByUid(final String uid)
  {
    return getByUid(uid, null);
  }

  @SuppressWarnings("unchecked")
  public TeamEventDO getByUid(final String uid, final Integer teamCalId)
  {
    if (uid == null) {
      return null;
    }
    List<TeamEventDO> list;
    final Integer id = TeamCalConfig.get().extractEventId(uid);
    if (teamCalId != null) {
      if (id != null) {
        // The uid refers an own event, therefore search for the extracted id.
        list = (List<TeamEventDO>) getHibernateTemplate()
            .find("from TeamEventDO e where e.id=? and e.calendar.id=? and e.deleted=false", id, teamCalId);
      } else {
        // It's an external event:
        list = (List<TeamEventDO>) getHibernateTemplate().find(
            "from TeamEventDO e where e.externalUid=? and e.calendar.id=? and e.deleted=false", uid,
            teamCalId);
      }
    } else {
      if (id != null) {
        // The uid refers an own event, therefore search for the extracted id.
        list = (List<TeamEventDO>) getHibernateTemplate().find("from TeamEventDO e where e.id=? and e.deleted=false",
            id);
      } else {
        // It's an external event:
        list = (List<TeamEventDO>) getHibernateTemplate()
            .find("from TeamEventDO e where e.externalUid=? and e.deleted=false", uid);
      }
    }
    if (list != null && list.isEmpty() == false && list.get(0) != null) {
      return list.get(0);
    }
    return null;
  }

  /**
   * Sets midnight (UTC) of all day events.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#onSaveOrModify(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void onSaveOrModify(final TeamEventDO event)
  {
    super.onSaveOrModify(event);
    Validate.notNull(event.getCalendar());
    if (event.isAllDay() == true) {
      final Date startDate = event.getStartDate();
      if (startDate != null) {
        event.setStartDate(CalendarUtils.getUTCMidnightTimestamp(startDate));
      }
      final Date endDate = event.getEndDate();
      if (endDate != null) {
        event.setEndDate(CalendarUtils.getUTCMidnightTimestamp(endDate));
      }
    }
    // Update recurrenceUntil date (for database queries):
    final Date recurrenceUntil = ICal4JUtils.calculateRecurrenceUntil(event.getRecurrenceRule(), event.getTimeZone());
    event.setRecurrenceUntil(recurrenceUntil);
  }

  /**
   * This method also returns recurrence events outside the time period of the given filter but affecting the
   * time-period (e. g. older recurrence events without end date or end date inside or after the given time period). If
   * calculateRecurrenceEvents is true, only the recurrence events inside the given time-period are returned, if false
   * only the origin recurrence event (may-be outside the given time-period) is returned.
   *
   * @param filter
   * @param calculateRecurrenceEvents If true, recurrence events inside the given time-period are calculated.
   * @return list of team events (same as {@link #getList(BaseSearchFilter)} but with all calculated and matching
   * recurrence events (if calculateRecurrenceEvents is true). Origin events are of type {@link TeamEventDO},
   * calculated events of type {@link TeamEvent}.
   */
  public List<TeamEvent> getEventList(final TeamEventFilter filter, final boolean calculateRecurrenceEvents)
  {
    final List<TeamEvent> result = new ArrayList<TeamEvent>();
    List<TeamEventDO> list = getList(filter);
    if (CollectionUtils.isNotEmpty(list) == true) {
      for (final TeamEventDO eventDO : list) {
        if (eventDO.hasRecurrence() == true) {
          // Added later.
          continue;
        }
        result.add(eventDO);
      }
    }
    final TeamEventFilter teamEventFilter = filter.clone().setOnlyRecurrence(true);
    final QueryFilter qFilter = buildQueryFilter(teamEventFilter);
    qFilter.add(Restrictions.isNotNull("recurrenceRule"));
    list = getList(qFilter);
    list = selectUnique(list);
    // add all abo events
    final List<TeamEventDO> recurrenceEvents = teamEventExternalSubscriptionCache
        .getRecurrenceEvents(teamEventFilter);
    if (recurrenceEvents != null && recurrenceEvents.size() > 0) {
      list.addAll(recurrenceEvents);
    }
    final TimeZone timeZone = ThreadLocalUserContext.getTimeZone();
    if (list != null) {
      for (final TeamEventDO eventDO : list) {
        if (eventDO.hasRecurrence() == false) {
          log.warn("Shouldn't occur! Please contact developer.");
          // This event was handled above.
          continue;
        }
        if (calculateRecurrenceEvents == false) {
          result.add(eventDO);
          continue;
        }
        final Collection<TeamEvent> events = TeamEventUtils.getRecurrenceEvents(teamEventFilter.getStartDate(),
            teamEventFilter.getEndDate(), eventDO, timeZone);
        if (events == null) {
          continue;
        }
        for (final TeamEvent event : events) {
          if (matches(event.getStartDate(), event.getEndDate(), event.isAllDay(), teamEventFilter) == false) {
            continue;
          }
          result.add(event);
        }
      }
    }
    return result;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#getListForSearchDao(org.projectforge.framework.persistence.api.BaseSearchFilter)
   */
  @Override
  public List<TeamEventDO> getListForSearchDao(final BaseSearchFilter filter)
  {
    final TeamEventFilter teamEventFilter = new TeamEventFilter(filter); // May-be called by SeachPage
    final Collection<TeamCalDO> allAccessibleCalendars = teamCalCache.getAllAccessibleCalendars();
    if (CollectionUtils.isEmpty(allAccessibleCalendars) == true) {
      // No calendars accessible, nothing to search.
      return new ArrayList<TeamEventDO>();
    }
    teamEventFilter.setTeamCals(teamCalService.getCalIdList(allAccessibleCalendars));
    return getList(teamEventFilter);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#getList(org.projectforge.framework.persistence.api.BaseSearchFilter)
   */
  @Override
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<TeamEventDO> getList(final BaseSearchFilter filter)
  {
    final TeamEventFilter teamEventFilter;
    if (filter instanceof TeamEventFilter) {
      teamEventFilter = ((TeamEventFilter) filter).clone();
    } else {
      teamEventFilter = new TeamEventFilter(filter);
    }
    if (CollectionUtils.isEmpty(teamEventFilter.getTeamCals()) == true && teamEventFilter.getTeamCalId() == null) {
      return new ArrayList<TeamEventDO>();
    }
    final QueryFilter qFilter = buildQueryFilter(teamEventFilter);
    final List<TeamEventDO> list = getList(qFilter);
    final List<TeamEventDO> result = new ArrayList<TeamEventDO>(list.size());
    if (list != null) {
      for (final TeamEventDO event : list) {
        if (matches(event.getStartDate(), event.getEndDate(), event.isAllDay(), teamEventFilter) == true) {
          result.add(event);
        }
      }
    }
    // subscriptions
    final List<Integer> alreadyAdded = new ArrayList<Integer>();
    // precondition for abos: existing teamcals in filter
    if (teamEventFilter.getTeamCals() != null) {
      for (final Integer calendarId : teamEventFilter.getTeamCals()) {
        if (teamEventExternalSubscriptionCache.isExternalSubscribedCalendar(calendarId) == true) {
          addEventsToList(teamEventFilter, result, teamEventExternalSubscriptionCache, calendarId);
          alreadyAdded.add(calendarId);
        }
      }
    }
    // if the getTeamCalId is not null and we do not added this before, do it now
    final Integer teamCalId = teamEventFilter.getTeamCalId();
    if (teamCalId != null && alreadyAdded.contains(teamCalId) == false) {
      if (teamEventExternalSubscriptionCache.isExternalSubscribedCalendar(teamCalId) == true) {
        addEventsToList(teamEventFilter, result, teamEventExternalSubscriptionCache, teamCalId);
      }
    }
    return result;
  }

  /**
   * Get all locations of the user's calendar events (not deleted ones) with modification date within last year.
   *
   * @param searchString
   */
  @SuppressWarnings("unchecked")
  public List<String> getLocationAutocompletion(final String searchString, final TeamCalDO[] calendars)
  {
    if (calendars == null || calendars.length == 0) {
      return null;
    }
    if (StringUtils.isBlank(searchString) == true) {
      return null;
    }
    checkLoggedInUserSelectAccess();
    final String s = "select distinct location from "
        + clazz.getSimpleName()
        + " t where deleted=false and t.calendar in :cals and lastUpdate > :lastUpdate and lower(t.location) like :location) order by t.location";
    final Query query = getSession().createQuery(s);
    query.setParameterList("cals", calendars);
    final DateHolder dh = new DateHolder();
    dh.add(Calendar.YEAR, -1);
    query.setDate("lastUpdate", dh.getDate());
    query.setString("location", "%" + StringUtils.lowerCase(searchString) + "%");
    final List<String> list = query.list();
    return list;
  }

  private void addEventsToList(final TeamEventFilter teamEventFilter, final List<TeamEventDO> result,
      final TeamEventExternalSubscriptionCache aboCache, final Integer calendarId)
  {
    final Date startDate = teamEventFilter.getStartDate();
    final Date endDate = teamEventFilter.getEndDate();
    final Long startTime = startDate == null ? 0 : startDate.getTime();
    final Long endTime = endDate == null ? MAX_DATE_3000 : endDate.getTime();
    final List<TeamEventDO> events = aboCache.getEvents(calendarId, startTime, endTime);
    if (events != null && events.size() > 0) {
      result.addAll(events);
    }
  }

  private boolean matches(final Date eventStartDate, final Date eventEndDate, final boolean allDay,
      final TeamEventFilter teamEventFilter)
  {
    final Date startDate = teamEventFilter.getStartDate();
    final Date endDate = teamEventFilter.getEndDate();
    if (allDay == true) {
      // Check date match:
      final Calendar utcCal = Calendar.getInstance(DateHelper.UTC);
      utcCal.setTime(eventStartDate);
      if (startDate != null && eventEndDate.before(startDate) == true) {
        // Check same day (eventStartDate in UTC and startDate of filter in user's time zone):
        final Calendar userCal = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
        userCal.setTime(startDate);
        if (CalendarUtils.isSameDay(utcCal, utcCal) == true) {
          return true;
        }
        return false;
      }
      if (endDate != null && eventStartDate.after(endDate) == true) {
        // Check same day (eventEndDate in UTC and endDate of filter in user's time zone):
        final Calendar userCal = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
        userCal.setTime(endDate);
        if (CalendarUtils.isSameDay(utcCal, utcCal) == true) {
          return true;
        }
        return false;
      }
      return true;
    } else {
      // Check start and stop date due to extension of time period of buildQueryFilter:
      if (startDate != null && eventEndDate.before(startDate) == true) {
        return false;
      }
      if (endDate != null && eventStartDate.after(endDate) == true) {
        return false;
      }
    }
    return true;
  }

  /**
   * The time period of the filter will be extended by one day. This is needed due to all day events which are stored in
   * UTC. The additional events in the result list not matching the time period have to be removed by caller!
   *
   * @param filter
   * @return
   */
  private QueryFilter buildQueryFilter(final TeamEventFilter filter)
  {
    final QueryFilter queryFilter = new QueryFilter(filter);
    final Collection<Integer> cals = filter.getTeamCals();
    if (CollectionUtils.isNotEmpty(cals) == true) {
      queryFilter.add(Restrictions.in("calendar.id", cals));
    } else if (filter.getTeamCalId() != null) {
      queryFilter.add(Restrictions.eq("calendar.id", filter.getTeamCalId()));
    }
    // Following period extension is needed due to all day events which are stored in UTC. The additional events in the result list not
    // matching the time period have to be removed by caller!
    Date startDate = filter.getStartDate();
    if (startDate != null) {
      startDate = new Date(startDate.getTime() - ONE_DAY);
    }
    Date endDate = filter.getEndDate();
    if (endDate != null) {
      endDate = new Date(endDate.getTime() + ONE_DAY);
    }
    // limit events to load to chosen date view.
    if (startDate != null && endDate != null) {
      if (filter.isOnlyRecurrence() == false) {
        queryFilter.add(Restrictions.or(
            (Restrictions.or(Restrictions.between("startDate", startDate, endDate),
                Restrictions.between("endDate", startDate, endDate))),
            // get events whose duration overlap with chosen duration.
            (Restrictions.and(Restrictions.le("startDate", startDate), Restrictions.ge("endDate", endDate)))));
      } else {
        queryFilter.add(
            // "startDate" < endDate && ("recurrenceUntil" == null || "recurrenceUntil" > startDate)
            (Restrictions.and(Restrictions.lt("startDate", endDate),
                Restrictions.or(Restrictions.isNull("recurrenceUntil"),
                    Restrictions.gt("recurrenceUntil", startDate)))));
      }
    } else if (startDate != null) {
      if (filter.isOnlyRecurrence() == false) {
        queryFilter.add(Restrictions.ge("startDate", startDate));
      } else {
        // This branch is reached for subscriptions and calendar downloads.
        queryFilter.add(
            // "recurrenceUntil" == null || "recurrenceUntil" > startDate
            Restrictions.or(Restrictions.isNull("recurrenceUntil"), Restrictions.gt("recurrenceUntil", startDate)));
      }
    } else if (endDate != null) {
      queryFilter.add(Restrictions.le("startDate", endDate));
    }
    queryFilter.addOrder(Order.desc("startDate"));
    if (log.isDebugEnabled() == true) {
      log.debug(ToStringBuilder.reflectionToString(filter));
    }
    return queryFilter;
  }

  /**
   * Gets history entries of super and adds all history entries of the TeamEventAttendeeDO childs.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#getDisplayHistoryEntries(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final TeamEventDO obj)
  {
    final List<DisplayHistoryEntry> list = super.getDisplayHistoryEntries(obj);
    if (hasLoggedInUserHistoryAccess(obj, false) == false) {
      return list;
    }
    if (CollectionUtils.isNotEmpty(obj.getAttendees()) == true) {
      for (final TeamEventAttendeeDO attendee : obj.getAttendees()) {
        final List<DisplayHistoryEntry> entries = internalGetDisplayHistoryEntries(attendee);
        for (final DisplayHistoryEntry entry : entries) {
          final String propertyName = entry.getPropertyName();
          if (propertyName != null) {
            entry.setPropertyName(
                attendee.toString() + ":" + entry.getPropertyName()); // Prepend user name or url to identify.
          } else {
            entry.setPropertyName(attendee.toString());
          }
        }
        list.addAll(entries);
      }
    }
    Collections.sort(list, new Comparator<DisplayHistoryEntry>()
    {
      public int compare(final DisplayHistoryEntry o1, final DisplayHistoryEntry o2)
      {
        return (o2.getTimestamp().compareTo(o1.getTimestamp()));
      }
    });
    return list;
  }

  @Override
  protected Class<?>[] getAdditionalHistorySearchDOs()
  {
    return ADDITIONAL_HISTORY_SEARCH_DOS;
  }

  /**
   * Returns also true, if idSet contains the id of any attendee.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#contains(java.util.Set,
   * org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected boolean contains(final Set<Integer> idSet, final TeamEventDO entry)
  {
    if (super.contains(idSet, entry) == true) {
      return true;
    }
    for (final TeamEventAttendeeDO pos : entry.getAttendees()) {
      if (idSet.contains(pos.getId()) == true) {
        return true;
      }
    }
    return false;
  }

  @Override
  public TeamEventDO newInstance()
  {
    return new TeamEventDO();
  }

  /**
   * @return the log
   */
  public Logger getLog()
  {
    return log;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#useOwnCriteriaCacheRegion()
   */
  @Override
  protected boolean useOwnCriteriaCacheRegion()
  {
    return true;
  }

  /**
   * @param teamCalDao the teamCalDao to set
   */
  public void setTeamCalDao(final TeamCalDao teamCalDao)
  {
    this.teamCalDao = teamCalDao;
  }
}
