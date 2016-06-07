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

package org.projectforge.web.teamcal.dialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.injection.Injector;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.TeamCalFilter;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.utils.NumberHelper;

import com.vaynberg.wicket.select2.Response;
import com.vaynberg.wicket.select2.TextChoiceProvider;

/**
 * Provider class for multipleChoice.
 * 
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
public class TeamCalChoiceProvider extends TextChoiceProvider<TeamCalDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalChoiceProvider.class);

  private static final long serialVersionUID = -8310756569504320965L;

  private static int RESULT_PAGE_SIZE = 20;

  @SpringBean
  private TeamCalDao teamCalDao;

  public TeamCalChoiceProvider()
  {
    Injector.get().inject(this);
  }

  /**
   * @see com.vaynberg.wicket.select2.TextChoiceProvider#getDisplayText(java.lang.Object)
   */
  @Override
  protected String getDisplayText(final TeamCalDO teamCal)
  {
    return teamCal.getTitle();
  }

  /**
   * @see com.vaynberg.wicket.select2.TextChoiceProvider#getId(java.lang.Object)
   */
  @Override
  protected Object getId(final TeamCalDO choice)
  {
    return choice.getId();
  }

  /**
   * @see com.vaynberg.wicket.select2.ChoiceProvider#query(java.lang.String, int, com.vaynberg.wicket.select2.Response)
   */
  @Override
  public void query(String term, final int page, final Response<TeamCalDO> response)
  {
    // add all access groups
    final List<TeamCalDO> fullAccessTeamCals = getTeamCalDao().getList(new TeamCalFilter());
    final List<TeamCalDO> result = new ArrayList<TeamCalDO>();
    term = term.toLowerCase();

    final int offset = page * RESULT_PAGE_SIZE;

    int matched = 0;
    boolean hasMore = false;
    for (final TeamCalDO teamCal : fullAccessTeamCals) {
      if (result.size() == RESULT_PAGE_SIZE) {
        hasMore = true;
        break;
      }
      if (teamCal.getTitle().toLowerCase().contains(term) == true
          || teamCal.getOwner().getFullname().toLowerCase().contains(term) == true
          || teamCal.getOwner().getUsername().toLowerCase().contains(term) == true) {
        matched++;
        if (matched > offset) {
          result.add(teamCal);
        }
      }
    }
    response.addAll(result);
    response.setHasMore(hasMore);
  }

  /**
   * @see com.vaynberg.wicket.select2.ChoiceProvider#toChoices(java.util.Collection)
   */
  @Override
  public Collection<TeamCalDO> toChoices(final Collection<String> ids)
  {
    final List<TeamCalDO> list = new ArrayList<TeamCalDO>();
    if (ids == null) {
      return list;
    }
    for (final String id : ids) {
      final Integer teamCalId = NumberHelper.parseInteger(id);
      if (teamCalId == null) {
        continue;
      }
      TeamCalDO teamCal = null;
      try {
        teamCal = getTeamCalDao().getById(teamCalId);
      } catch (final AccessException ex) {
        log.warn("User has no access to the selected calendar '" + id + "'.");
      }
      if (teamCal != null) {
        list.add(teamCal);
      }
    }
    return list;
  }

  private TeamCalDao getTeamCalDao()
  {
    return teamCalDao;
  }

}
