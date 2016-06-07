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

package org.projectforge.business.fibu;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Repräsentiert einee Position innerhalb einer Rechnung als Übersichtsobject (value object) zur Verwendung z. B. in Listen.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class RechnungsPositionVO implements Comparable<RechnungsPositionVO>, Serializable
{
  private static final long serialVersionUID = -4377826234871813253L;

  private final short number;

  private final Integer rechnungId;

  private final Date date;

  private final Integer rechnungNummer;

  private final String rechnungBetreff;

  private final String text;

  private Integer auftragsId;

  private short auftragsPositionNummer;

  private final BigDecimal netSum;

  public RechnungsPositionVO(final RechnungsPositionDO rechnungsPosition)
  {
    final RechnungDO rechnung = rechnungsPosition.getRechnung();
    this.date = rechnung.getDatum();
    this.number = rechnungsPosition.getNumber();
    this.rechnungId = rechnung.getId();
    this.rechnungNummer = rechnung.getNummer();
    this.rechnungBetreff = rechnung.getBetreff();
    this.text = rechnungsPosition.getText();
    this.netSum = rechnungsPosition.getNetSum();
    final AuftragsPositionDO auftragsPosition = rechnungsPosition.getAuftragsPosition();
    if (auftragsPosition != null) {
      final AuftragDO auftrag = auftragsPosition.getAuftrag();
      if (auftrag != null) {
        this.auftragsId = auftrag.getId();
      }
      this.auftragsPositionNummer = auftragsPosition.getNumber();
    }
  }

  public short getNumber()
  {
    return number;
  }

  /**
   * @return the date
   */
  public Date getDate()
  {
    return date;
  }

  public BigDecimal getNettoSumme()
  {
    return netSum;
  }

  public String getText()
  {
    return text;
  }

  public Integer getRechnungId()
  {
    return rechnungId;
  }

  public Integer getRechnungNummer()
  {
    return rechnungNummer;
  }

  /**
   * @see RechnungDO#getTitel()
   */
  public String getRechnungTitle()
  {
    return rechnungBetreff;
  }

  public Integer getAuftragsId()
  {
    return auftragsId;
  }

  public short getAuftragsPositionNummer()
  {
    return auftragsPositionNummer;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof RechnungsPositionVO) {
      final RechnungsPositionVO other = (RechnungsPositionVO) o;
      if (ObjectUtils.equals(this.getNumber(), other.getNumber()) == false)
        return false;
      if (ObjectUtils.equals(this.getRechnungId(), other.getRechnungId()) == false)
        return false;
      return true;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(getNumber());
    hcb.append(getRechnungId());
    return hcb.toHashCode();
  }

  public int compareTo(final RechnungsPositionVO o)
  {
    if (this.rechnungNummer.equals(o.rechnungNummer) == false) {
      return this.rechnungNummer.compareTo(o.rechnungNummer);
    }
    if (this.number < o.number) {
      return -1;
    } else if (this.number == o.number) {
      return 0;
    } else {
      return +1;
    }
  }
}
