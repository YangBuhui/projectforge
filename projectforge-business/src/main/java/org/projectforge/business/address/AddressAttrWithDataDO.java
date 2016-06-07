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

package org.projectforge.business.address;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;

/**
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
@Entity
@DiscriminatorValue("1")
public class AddressAttrWithDataDO extends AddressAttrDO
{

  public AddressAttrWithDataDO()
  {
    super();
  }

  public AddressAttrWithDataDO(final AddressDO parent, final String propertyName, final char type, final String value)
  {
    super(parent, propertyName, type, value);
  }

  public AddressAttrWithDataDO(final AddressDO parent)
  {
    super(parent);
  }

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", targetEntity = AddressAttrDataDO.class,
      orphanRemoval = true, fetch = FetchType.EAGER)
  @OrderColumn(name = "datarow")
  @Override
  public List<JpaTabAttrDataBaseDO<?, Integer>> getData()
  {
    return super.getData();
  }
}
