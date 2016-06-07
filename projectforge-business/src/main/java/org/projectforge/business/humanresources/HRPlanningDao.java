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

package org.projectforge.business.humanresources;

import java.sql.Date;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Mario Groß (m.gross@micromata.de)
 *
 */
@Repository
public class HRPlanningDao extends BaseDao<HRPlanningDO>
{
  public static final UserRightId USER_RIGHT_ID = UserRightId.PM_HR_PLANNING;

  private static final Logger log = Logger.getLogger(HRPlanningDao.class);

  private static final Class<?>[] ADDITIONAL_SEARCH_DOS = new Class[] { HRPlanningEntryDO.class };

  @Autowired
  private ProjektDao projektDao;

  @Autowired
  private UserDao userDao;

  protected HRPlanningDao()
  {
    super(HRPlanningDO.class);
    userRightId = USER_RIGHT_ID;
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return new String[] { "entries.projekt.name", "entries.projekt.kunde.name", "user.username", "user.firstname",
        "user.lastname" };
  }

  /**
   * @param sheet
   * @param projektId If null, then projekt will be set to null;
   */
  public void setProjekt(final HRPlanningEntryDO sheet, final Integer projektId)
  {
    final ProjektDO projekt = projektDao.getOrLoad(projektId);
    sheet.setProjekt(projekt);
  }

  /**
   * @param sheet
   * @param userId If null, then user will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setUser(final HRPlanningDO sheet, final Integer userId)
  {
    final PFUserDO user = userDao.getOrLoad(userId);
    sheet.setUser(user);
  }

  /**
   * Does an entry with the same user and week of year already exist?
   * 
   * @param planning
   * @return If week or user id is not given, return false.
   */
  public boolean doesEntryAlreadyExist(final HRPlanningDO planning)
  {
    Validate.notNull(planning);
    return doesEntryAlreadyExist(planning.getId(), planning.getUserId(), planning.getWeek());
  }

  /**
   * Does an entry with the same user and week of year already exist?
   * 
   * @param planningId Id of the current planning or null if new.
   * @param userId
   * @param week
   * @return If week or user id is not given, return false.
   */
  @SuppressWarnings("unchecked")
  public boolean doesEntryAlreadyExist(final Integer planningId, final Integer userId, final Date week)
  {
    if (week == null || userId == null) {
      return false;
    }
    final List<HRPlanningDO> list;
    if (planningId == null) {
      // New entry
      list = (List<HRPlanningDO>) getHibernateTemplate().find("from HRPlanningDO p where p.user.id = ? and p.week = ?",
          new Object[] { userId, week });
    } else {
      // Entry already exists. Check collision:
      list = (List<HRPlanningDO>) getHibernateTemplate().find(
          "from HRPlanningDO p where p.user.id = ? and p.week = ? and pk <> ?",
          new Object[] { userId, week, planningId });
    }
    return (CollectionUtils.isNotEmpty(list) == true);
  }

  public HRPlanningDO getEntry(final PFUserDO user, final Date week)
  {
    return getEntry(user.getId(), week);
  }

  @SuppressWarnings("unchecked")
  public HRPlanningDO getEntry(final Integer userId, final Date week)
  {
    final DateHolder date = new DateHolder(week, DateHelper.UTC, Locale.GERMANY);
    if (date.isBeginOfWeek() == false) {
      log.error("Date is not begin of week, try to change date: " + DateHelper.formatAsUTC(date.getDate()));
      date.setBeginOfWeek();
    }
    final List<HRPlanningDO> list = (List<HRPlanningDO>) getHibernateTemplate().find(
        "from HRPlanningDO p where p.user.id = ? and p.week = ?",
        new Object[] { userId, date.getSQLDate() });
    if (list == null || list.size() != 1) {
      return null;
    }
    final HRPlanningDO planning = list.get(0);
    if (accessChecker.hasLoggedInUserSelectAccess(userRightId, planning, false) == true) {
      return planning;
    } else {
      return null;
    }
  }

  @Override
  public List<HRPlanningDO> getList(final BaseSearchFilter filter)
  {
    final HRPlanningFilter myFilter = (HRPlanningFilter) filter;
    if (myFilter.getStopTime() != null) {
      final DateHolder date = new DateHolder(myFilter.getStopTime());
      date.setEndOfDay();
      myFilter.setStopTime(date.getDate());
    }
    final QueryFilter queryFilter = buildQueryFilter(myFilter);
    final List<HRPlanningDO> result = getList(queryFilter);
    if (result == null) {
      return null;
    }
    return result;
  }

  public QueryFilter buildQueryFilter(final HRPlanningFilter filter)
  {
    final QueryFilter queryFilter = new QueryFilter(filter);
    if (filter.getUserId() != null) {
      final PFUserDO user = new PFUserDO();
      user.setId(filter.getUserId());
      queryFilter.add(Restrictions.eq("user", user));
    }
    if (filter.getStartTime() != null && filter.getStopTime() != null) {
      queryFilter.add(Restrictions.between("week", filter.getStartTime(), filter.getStopTime()));
    } else if (filter.getStartTime() != null) {
      queryFilter.add(Restrictions.ge("week", filter.getStartTime()));
    } else if (filter.getStopTime() != null) {
      queryFilter.add(Restrictions.le("week", filter.getStopTime()));
    }
    if (filter.getProjektId() != null) {
      queryFilter.add(Restrictions.eq("projekt.id", filter.getProjektId()));
    }
    queryFilter.addOrder(Order.desc("week"));
    if (log.isDebugEnabled() == true) {
      log.debug(ToStringBuilder.reflectionToString(filter));
    }
    return queryFilter;
  }

  /**
   * <ul>
   * <li>Checks week date on: monday, 0:00:00.000 and if check fails then the date will be set to.</li>
   * <li>Check deleted entries and re-adds them instead of inserting a new entry, if exist.</li>
   * <ul>
   * 
   * @see org.projectforge.framework.persistence.api.BaseDao#onSaveOrModify(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void onSaveOrModify(final HRPlanningDO obj)
  {
    final DateHolder date = new DateHolder(obj.getWeek(), DateHelper.UTC, Locale.GERMANY);
    if (date.getDayOfWeek() != Calendar.MONDAY || date.getMilliSecond() != 0 || date.getMinute() != 0
        || date.getHourOfDay() != 0) {
      log.error("Date is not begin of week, try to change date: " + DateHelper.formatAsUTC(date.getDate()));
      obj.setFirstDayOfWeek(date.getSQLDate());
    }
    super.onSaveOrModify(obj);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#prepareHibernateSearch(org.projectforge.core.ExtendedBaseDO,
   *      org.projectforge.framework.access.OperationType)
   */
  @Override
  protected void prepareHibernateSearch(final HRPlanningDO obj, final OperationType operationType)
  {
    final List<HRPlanningEntryDO> entries = obj.getEntries();
    if (entries != null) {
      for (final HRPlanningEntryDO entry : entries) {
        projektDao.initializeProjektManagerGroup(entry.getProjekt());
      }
    }
    final PFUserDO user = obj.getUser();
    if (user != null) {
      obj.setUser(getUserGroupCache().getUser(user.getId()));
    }
  }

  @Override
  public HRPlanningDO newInstance()
  {
    return new HRPlanningDO();
  }

  /**
   * Gets history entries of super and adds all history entries of the HRPlanningEntryDO childs.
   * 
   * @see org.projectforge.framework.persistence.api.BaseDao#getDisplayHistoryEntries(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final HRPlanningDO obj)
  {
    final List<DisplayHistoryEntry> list = super.getDisplayHistoryEntries(obj);
    if (accessChecker.hasLoggedInUserHistoryAccess(userRightId, obj, false) == false) {
      return list;
    }
    if (CollectionUtils.isNotEmpty(obj.getEntries()) == true) {
      for (final HRPlanningEntryDO position : obj.getEntries()) {
        final List<DisplayHistoryEntry> entries = internalGetDisplayHistoryEntries(position);
        for (final DisplayHistoryEntry entry : entries) {
          final String propertyName = entry.getPropertyName();
          if (propertyName != null) {
            if (position.getProjekt() != null) {
              entry.setPropertyName(position.getProjektName() + ":" + entry.getPropertyName()); // Prepend name of project
            } else {
              entry.setPropertyName(position.getStatus() + ":" + entry.getPropertyName()); // Prepend status
            }
          } else {
            if (position.getProjekt() != null) {
              entry.setPropertyName(position.getProjektName());
            } else {
              entry.setPropertyName(String.valueOf(position.getStatus()));
            }
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
    return ADDITIONAL_SEARCH_DOS;
  }

}
