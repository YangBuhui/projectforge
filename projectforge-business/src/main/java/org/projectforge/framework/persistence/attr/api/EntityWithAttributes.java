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

import java.util.Set;

/**
 * A entity, which as attached attributes.
 *
 * The getStringAttribute and putStringAttribute stores values as string.
 *
 * getAttribute and putAttribute uses internally a StringConverter to convert a object from/to string.
 *
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@Deprecated
public interface EntityWithAttributes
{
  /**
   * get an entity.
   *
   * @param key aka propertyName
   * @return if not set, returns null
   */
  String getStringAttr(String key);

  /**
   * set a attribute.
   *
   * @param key must not be null
   * @param value must not be null
   */
  void putStringAttr(String key, String value);

  /**
   * Gets the attribute.
   *
   * @param key the key
   * @return the attribute
   */
  Object getAttr(String key);

  /**
   * Get an Attribute with expected class.
   *
   * @param key
   * @param expectedClass
   * @return
   * @throws IllegalArgumentException if value is not null and class does not match
   */
  <T> T getAttr(String key, Class<T> expectedClass);

  /**
   * Put attribute.
   *
   * @param key the key
   * @param value the value
   */
  void putAttr(String key, Object value);

  /**
   * Remove the attribute.
   *
   * @param key aka propertyName
   */
  void removeAttr(String key);

  /**
   * The keys of the attributes.
   *
   * @return
   */
  Set<String> getAttrKeys();
}
