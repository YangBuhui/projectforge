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

package org.projectforge.framework.xstream.converter;

import org.apache.commons.lang.StringUtils;
import org.projectforge.framework.xstream.XmlConstants;

/**
 * Class names are stored as full qualified strings such as "org.projectforge.web.calendar.CalendarPage".
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ClassConverter implements IConverter<Class< ? >>
{
  static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ClassConverter.class);

  /**
   * Returns the Class represented by the given string, otherwise null if the class can't be instantiated or the given string is null.
   * @see org.projectforge.framework.xstream.converter.IConverter#fromString(java.lang.String)
   * @see Class#forName(String)
   */
  @Override
  public Class< ? > fromString(final String str)
  {
    if (StringUtils.isBlank(str) == true || XmlConstants.NULL_IDENTIFIER.equals(str) == true) {
      return null;
    }
    try {
      final Class< ? > clazz = Class.forName(str);
      return clazz;
    } catch (final ClassNotFoundException ex) {
      log.warn("Can't convert value '" + str + "' to class (no such class found).");
      return null;
    }
  }

  /**
   * @see org.projectforge.framework.xstream.converter.IConverter#toString(java.lang.Object)
   */
  @Override
  public String toString(final Object obj)
  {
    return String.valueOf(obj);
  }
}
