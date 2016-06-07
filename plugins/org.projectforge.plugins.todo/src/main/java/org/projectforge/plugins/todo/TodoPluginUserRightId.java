/////////////////////////////////////////////////////////////////////////////
//
//Project ProjectForge Community Edition
//www.projectforge.org
//
//Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
//
//ProjectForge is dual-licensed.
//
//This community edition is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License as published
//by the Free Software Foundation; version 3 of the License.
//
//This community edition is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
//Public License for more details.
//
//You should have received a copy of the GNU General Public License along
//with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////
package org.projectforge.plugins.todo;

import java.util.Arrays;
import java.util.Collection;

import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.user.HibernateSearchUserRightIdBridge;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.RightRightIdProviderService;

@Indexed
@ClassBridge(index = Index.YES /* TOKENIZED */, store = Store.NO, impl = HibernateSearchUserRightIdBridge.class)
public enum TodoPluginUserRightId implements IUserRightId
{
  PLUGIN_TODO("PLUGIN_TODO", "plugin10", "plugins.todo.todo");

  public static class ProviderService implements RightRightIdProviderService
  {
    @Override
    public Collection<IUserRightId> getUserRightIds()
    {
      return Arrays.asList(TodoPluginUserRightId.values());
    }
  }

  private final String id;

  private final String orderString;

  private final String i18nKey;

  /**
   * @param id Must be unique (including all plugins).
   * @param orderString For displaying the rights in e. g. UserEditPage in the correct order.
   * @param i18nKey
   */
  private TodoPluginUserRightId(final String id, final String orderString, final String i18nKey)
  {
    this.id = id;
    this.orderString = orderString;
    this.i18nKey = i18nKey;
  }

  @Override
  public String getId()
  {
    return id;
  }

  @Override
  public String getI18nKey()
  {
    return i18nKey;
  }

  @Override
  public String getOrderString()
  {
    return orderString;
  }

  @Override
  public String toString()
  {
    return String.valueOf(id);
  }

  @Override
  public int compareTo(IUserRightId o)
  {
    return this.compareTo(o);
  }

}
