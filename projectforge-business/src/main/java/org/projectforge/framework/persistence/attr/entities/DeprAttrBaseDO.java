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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.attr.api.TabAttributeEntry;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.history.api.NoHistory;
import de.micromata.genome.util.strings.converter.ConvertedStringTypes;
import de.micromata.genome.util.strings.converter.StringConverter;
import de.micromata.genome.util.types.Pair;

/**
 *
 * M is the Master entity type, owning this attribute table.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
@MappedSuperclass
@NoHistory
@Deprecated
public abstract class DeprAttrBaseDO<M extends DefaultBaseDO>extends DefaultBaseDO implements TabAttributeEntry
{
  /**
   * Link to the owning table.
   *
   * Annotate the getter in the derived class with proper ManyToOne annotation
   */
  protected M parent;

  /**
   * Key of the attribute
   */
  private String propertyName;

  /**
   * Type of the value.
   */
  private char type;

  /**
   * value of the attribute. If the value is longer as VALUE_MAXLENGHT, following parts are stored in data children.
   */
  protected String value;

  /**
   * Optional data elements, if value doesn't fit into the value field.
   *
   * The data will only be used in *WithData*DOs.
   */
  private List<DeprAttrDataBaseDO<?>> data = new ArrayList<DeprAttrDataBaseDO<?>>();

  public DeprAttrBaseDO()
  {

  }

  public DeprAttrBaseDO(final M parent)
  {
    this.parent = parent;
  }

  public DeprAttrBaseDO(final M parent, final String propertyName, final char type, final String value)
  {
    this.parent = parent;
    this.type = type;
    this.propertyName = propertyName;
    setStringData(value);
  }

  public abstract DeprAttrDataBaseDO<?> createData(String data);

  @Transient
  public int getValueMaxLength()
  {
    return DeprAttrDataBaseDO.DATA_MAXLENGTH;
  }

  @Override
  @Transient
  public String getValue()
  {
    return getStringData();
  }

  @Override
  public void setValue(final String value)
  {
    setStringData(value);
    setType(ConvertedStringTypes.STRING.getShortType());
  }

  @Override
  public Object getValueObject(final StringConverter converter)
  {
    return converter.stringToObject(getType(), getStringData());
  }

  @Override
  public void setValueObject(final StringConverter converter, final Object value)
  {
    final Pair<Character, String> pair = converter.objectToString(value);
    setType(pair.getFirst());
    setStringData(pair.getSecond());
  }

  /**
   * Get the value of the attribute.
   *
   * If data children exists, the resulting value will be joined.
   */
  @Transient
  public String getStringData()
  {
    final List<DeprAttrDataBaseDO<?>> data = getData();
    if (data.isEmpty() == true) {
      return value;
    }
    final StringBuilder sb = new StringBuilder();
    sb.append(value);
    for (final DeprAttrDataBaseDO<?> dv : data) {
      sb.append(dv.getData());
    }
    return sb.toString();
  }

  /**
   * Set the value of the attribute.
   *
   * if value is longer than VALUE_MAXLENGHT the string will be split and stored in additional data children entities.
   */
  public void setStringData(final String value)
  {
    final List<DeprAttrDataBaseDO<?>> data = getData();
    data.clear();
    final int maxValLength = getValueMaxLength();
    if (StringUtils.length(value) > maxValLength) {
      this.value = value.substring(0, maxValLength);
      String rest = value.substring(maxValLength);
      final int maxDataLength = getValueMaxLength();
      int rowIdx = 0;
      while (rest.length() > 0) {
        final String ds = StringUtils.substring(rest, 0, maxDataLength);
        rest = StringUtils.substring(rest, maxDataLength);
        final DeprAttrDataBaseDO<?> dataDo = createData(ds);
        dataDo.setDatarow(rowIdx++);
        data.add(dataDo);
      }
    } else {
      this.value = value;
    }
  }

  @Override
  public ModificationStatus copyValuesFrom(final BaseDO<? extends Serializable> source, final String... ignoreFields)
  {
    ModificationStatus modificationStatus = super.copyValuesFrom(source, "data");

    final String tsd = getStringData();
    final DeprAttrBaseDO<M> src = (DeprAttrBaseDO<M>) source;
    final String ssd = src.getStringData();
    if (StringUtils.equals(tsd, ssd) == true) {
      return modificationStatus;
    }
    data.clear();
    data.addAll(src.getData());
    modificationStatus = getModificationStatus(modificationStatus, ModificationStatus.MAJOR);
    return modificationStatus;

  }

  private DeprAttrDataBaseDO<?> findDataById(final Integer id)
  {
    for (final DeprAttrDataBaseDO<?> srcdata : data) {
      if (ObjectUtils.equals(srcdata.getId(), id) == true) {
        return srcdata;
      }
    }
    return null;
  }

  @Override
  @Column(name = "PROPERTYNAME", length = 255)
  public String getPropertyName()
  {
    return propertyName;
  }

  @Override
  public void setPropertyName(final String propertyName)
  {
    this.propertyName = propertyName;
  }

  @Override
  @Column(name = "TYPE", nullable = false)
  public char getType()
  {
    return type;
  }

  @Column(name = "VALUE", length = 3000)
  public String getInternalValue()
  {
    return value;
  }

  public void setInternalValue(final String value)
  {
    this.value = value;
  }

  @Override
  public void setType(final char type)
  {
    this.type = type;
  }

  @Transient
  public M getParent()
  {
    return parent;
  }

  public void setParent(final M parent)
  {
    this.parent = parent;
  }

  @Transient
  public List<DeprAttrDataBaseDO<?>> getData()
  {
    return data;
  }

  public void setData(final List<DeprAttrDataBaseDO<?>> data)
  {
    this.data = data;
  }
}
