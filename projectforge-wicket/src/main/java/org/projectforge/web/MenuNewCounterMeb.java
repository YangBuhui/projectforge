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

package org.projectforge.web;

import org.apache.wicket.injection.Injector;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.meb.MebDao;

public class MenuNewCounterMeb extends Model<Integer>
{
  private static final long serialVersionUID = 6890654355525850696L;

  @SpringBean
  private MebDao mebDao;

  public void setMebDao(final MebDao mebDao)
  {
    this.mebDao = mebDao;
  }

  @Override
  public Integer getObject()
  {
    if (mebDao == null) {
      Injector.get().inject(this);
    }
    return mebDao.getRecentMEBEntries(null);
  }
}
