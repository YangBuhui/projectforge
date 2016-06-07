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

package org.projectforge.framework.persistence.attr.entities;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.history.api.NoHistory;

/**
 * Long data of an attribute will be stored in AttrData.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
@MappedSuperclass
@NoHistory
@Deprecated
public class DeprAttrDataBaseDO<A extends DeprAttrBaseDO<?>>extends DefaultBaseDO
{

  private static final long serialVersionUID = -1040673599827915302L;

  /**
   * Maximum string length fitting into the datarow columns.
   *
   * NOTE: If you chance this, you have also change the jpa annotation.
   *
   */
  public static final int DATA_MAXLENGTH = 2990;

  /**
   * Link to parent.
   *
   * Annotate the getter in the derived class with proper ManyToOne annotation
   */
  protected A parent;

  /**
   * Row number of splittet datas
   */
  private int datarow;

  /**
   * The string encoded data.
   */
  private String data;

  public DeprAttrDataBaseDO()
  {

  }

  public DeprAttrDataBaseDO(final A parent)
  {
    this.parent = parent;
  }

  public DeprAttrDataBaseDO(final A parent, final String value)
  {
    this.parent = parent;
    this.data = value;
  }

  @Column(name = "DATAROW", nullable = false)
  public int getDatarow()
  {
    return datarow;
  }

  @Column(name = "DATACOL", length = DATA_MAXLENGTH)
  public String getData()
  {
    return data;
  }

  public void setDatarow(final int datarow)
  {
    this.datarow = datarow;
  }

  public void setData(final String data)
  {
    this.data = data;
  }

  @Transient
  public A getParent()
  {
    return parent;
  }

  public void setParent(final A parent)
  {
    this.parent = parent;
  }
}
