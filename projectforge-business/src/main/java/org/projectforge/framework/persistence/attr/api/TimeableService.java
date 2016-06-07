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

package org.projectforge.framework.persistence.attr.api;

import java.util.Date;

/**
 * Methods to manage/validate Timeable DOs.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
@Deprecated
public interface TimeableService
{
  /**
   * Search the row, on given date.
   *
   * @param date must not be null
   * @return null if not found
   */
  <T extends TimeableAttrRow> T getRowForTime(final Date date, final EntityWithTimeableAttr<T> entity);

  <R, T extends TimeableAttrRow> R getAttrValue(final Date date, final EntityWithTimeableAttr<T> entity,
      final String propertyName, final Class<R> expectedClass);

  <R, T extends TimeableAttrRow> R getAttrValue(final EntityWithTimeableAttr<T> entity,
      final String propertyName, final Class<R> expectedClass);

}
