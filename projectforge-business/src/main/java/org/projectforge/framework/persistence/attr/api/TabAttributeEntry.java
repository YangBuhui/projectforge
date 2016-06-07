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

import de.micromata.genome.util.strings.converter.StringConverter;

/**
 * Interface to one entry to the table attributes
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@Deprecated
public interface TabAttributeEntry
{
  /**
   * The PK of the attribute
   *
   * @return may null if not stored in db.
   */
  Integer getId();

  /**
   * The PK of the attribute
   *
   * @return
   */
  void setId(Integer pk);

  /**
   * The propertyName / key of the attribute
   *
   * @return
   */
  String getPropertyName();

  /**
   * The propertyName / key of the attribute
   *
   * @return
   */
  void setPropertyName(String propertyName);

  /**
   * The type of the attribute.
   *
   * Default is STRING('V')
   *
   * @see de.micromata.genome.db.attr.value.AttrValueType.shortTypeName
   * @return
   */
  char getType();

  void setType(char type);

  /**
   * The attribute value.
   *
   * @return
   */
  String getValue();

  /**
   * Set the String value.
   *
   * @param value
   */
  void setValue(String value);

  /**
   * Get Value object. Use the converter.O
   *
   * @param converter
   * @return
   */
  Object getValueObject(StringConverter converter);

  /**
   * Sets the value object.
   *
   * @param converter the converter
   * @param value the value
   */
  void setValueObject(StringConverter converter, Object value);

}
