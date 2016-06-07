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

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;

/**
 *
 * Time pending attributes of an employee.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
@Entity
@Table(name = "t_address_attr"
/*
 * , uniqueConstraints = @UniqueConstraint(name = "ctx_address_attr_parentpropn", columnNames = { "parent",
 * "propertyName" })
 */
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "withdata", discriminatorType = DiscriminatorType.CHAR)
@DiscriminatorValue("0")
public class AddressAttrDO extends JpaTabAttrBaseDO<AddressDO, Integer>
{
  public AddressAttrDO()
  {
    super();
  }

  public AddressAttrDO(final AddressDO parent, final String propertyName, final char type, final String value)
  {
    super(parent, propertyName, type, value);
  }

  public AddressAttrDO(final AddressDO parent)
  {
    super(parent);
  }

  @Override
  @Id
  @GeneratedValue
  @Column(name = "pk")
  public Integer getPk()
  {
    return pk;
  }

  /**
   * @see org.projectforge.framework.persistence.attr.entities.DeprAttrBaseDO#createData(java.lang.String)
   */
  @Override
  public JpaTabAttrDataBaseDO<?, Integer> createData(final String data)
  {
    return new AddressAttrDataDO(this, data);

  }

  @Override
  @ManyToOne(optional = false)
  @JoinColumn(name = "parent", referencedColumnName = "pk")
  public AddressDO getParent()
  {
    return super.getParent();

  }

}
