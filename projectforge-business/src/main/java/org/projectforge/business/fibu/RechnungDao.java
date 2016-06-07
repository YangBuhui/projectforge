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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.FetchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.fibu.kost.KostZuweisungDO;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.i18n.MessageParam;
import org.projectforge.framework.i18n.MessageParamType;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.xstream.XmlObjectReader;
import org.projectforge.framework.xstream.XmlObjectWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class RechnungDao extends BaseDao<RechnungDO>
{
  public static final UserRightId USER_RIGHT_ID = UserRightId.FIBU_AUSGANGSRECHNUNGEN;

  public final static int START_NUMBER = 1000;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RechnungDao.class);

  private static final Class<?>[] ADDITIONAL_SEARCH_DOS = new Class[] { RechnungsPositionDO.class };

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "kunde.name", "projekt.name",
      "projekt.kunde.name",
      "positionen.text", "positionen.auftragsPosition.position", "positionen.auftragsPosition.position",
      "positionen.auftragsPosition.titel", "positionen.auftragsPosition.bemerkung" };

  private static BigDecimal defaultSteuersatz = new BigDecimal(0.19);

  @Autowired
  private KundeDao kundeDao;

  @Autowired
  private ProjektDao projektDao;

  @Autowired
  private RechnungCache rechnungCache;

  public static BigDecimal getNettoSumme(final Collection<RechnungsPositionVO> col)
  {
    BigDecimal nettoSumme = BigDecimal.ZERO;
    if (col != null && col.size() > 0) {
      for (final RechnungsPositionVO pos : col) {
        nettoSumme = nettoSumme.add(pos.getNettoSumme());
      }
    }
    return nettoSumme;
  }

  static void readUiStatusFromXml(final AbstractRechnungDO<?> rechnung)
  {
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(RechnungUIStatus.class);
    final String styleAsXml = rechnung.getUiStatusAsXml();
    final RechnungUIStatus status;
    if (StringUtils.isEmpty(styleAsXml) == true) {
      status = new RechnungUIStatus();
    } else {
      status = (RechnungUIStatus) reader.read(styleAsXml);
    }
    rechnung.setUiStatus(status);
  }

  static void writeUiStatusToXml(final AbstractRechnungDO<?> rechnung)
  {
    final String uiStatusAsXml = XmlObjectWriter.writeAsXml(rechnung.getUiStatus());
    rechnung.setUiStatusAsXml(uiStatusAsXml);
  }

  /**
   * @return the rechnungCache
   */
  public RechnungCache getRechnungCache()
  {
    return rechnungCache;
  }

  public RechnungDao()
  {
    super(RechnungDO.class);
    userRightId = USER_RIGHT_ID;
  }

  /**
   * List of all years with invoices: select min(datum), max(datum) from t_fibu_rechnung.
   * 
   * @return
   */
  @SuppressWarnings("unchecked")
  public int[] getYears()
  {
    final List<Object[]> list = getSession().createQuery("select min(datum), max(datum) from RechnungDO t").list();
    return SQLHelper.getYears(list);
  }

  public RechnungsStatistik buildStatistik(final List<RechnungDO> list)
  {
    final RechnungsStatistik stats = new RechnungsStatistik();
    if (list == null) {
      return stats;
    }
    for (final RechnungDO rechnung : list) {
      stats.add(rechnung);
    }
    return stats;
  }

  /**
   * @param rechnung
   * @param days
   * @see DateHelper#getCalendar()
   */
  public Date calculateFaelligkeit(final RechnungDO rechnung, final int days)
  {
    if (rechnung.getDatum() == null) {
      return null;
    }
    final Calendar cal = DateHelper.getCalendar();
    cal.setTime(rechnung.getDatum());
    cal.add(Calendar.DAY_OF_YEAR, days);
    return cal.getTime();
  }

  /**
   * @param rechnung
   * @param kundeId If null, then kunde will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setKunde(final RechnungDO rechnung, final Integer kundeId)
  {
    final KundeDO kunde = kundeDao.getOrLoad(kundeId);
    rechnung.setKunde(kunde);
  }

  /**
   * @param rechnung
   * @param projektId If null, then projekt will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setProjekt(final RechnungDO rechnung, final Integer projektId)
  {
    final ProjektDO projekt = projektDao.getOrLoad(projektId);
    rechnung.setProjekt(projekt);
  }

  /**
   * Sets the scales of percentage and currency amounts. <br/>
   * Gutschriftsanzeigen dürfen keine Rechnungsnummer haben. Wenn eine Rechnungsnummer für neue Rechnungen gegeben
   * wurde, so muss sie fortlaufend sein. Berechnet das Zahlungsziel in Tagen, wenn nicht gesetzt, damit es indiziert
   * wird.
   * 
   * @see org.projectforge.framework.persistence.api.BaseDao#onSaveOrModify(org.projectforge.core.ExtendedBaseDO)
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void onSaveOrModify(final RechnungDO obj)
  {
    if (obj.getTyp() == RechnungTyp.GUTSCHRIFTSANZEIGE_DURCH_KUNDEN) {
      if (obj.getNummer() != null) {
        throw new UserException("fibu.rechnung.error.gutschriftsanzeigeDarfKeineRechnungsnummerHaben");
      }
    } else {
      if (obj.getNummer() == null) {
        throw new UserException("validation.required.valueNotPresent",
            new MessageParam("fibu.rechnung.nummer", MessageParamType.I18N_KEY));
      }
      if (obj.getId() == null) {
        // Neue Rechnung
        final Integer next = getNextNumber(obj);
        if (next.intValue() != obj.getNummer().intValue()) {
          throw new UserException("fibu.rechnung.error.rechnungsNummerIstNichtFortlaufend");
        }
      } else {
        final List<RechnungDO> list = (List<RechnungDO>) getHibernateTemplate().find(
            "from RechnungDO r where r.nummer = ? and r.id <> ?",
            new Object[] { obj.getNummer(), obj.getId() });
        if (list != null && list.size() > 0) {
          throw new UserException("fibu.rechnung.error.rechnungsNummerBereitsVergeben");
        }
      }
    }
    if (obj.getZahlBetrag() != null) {
      obj.setZahlBetrag(obj.getZahlBetrag().setScale(2, RoundingMode.HALF_UP));
    }
    obj.recalculate();
    if (CollectionUtils.isEmpty(obj.getPositionen()) == true) {
      throw new UserException("fibu.rechnung.error.rechnungHatKeinePositionen");
    }
    final int size = obj.getPositionen().size();
    for (int i = size - 1; i > 0; i--) {
      // Don't remove first position, remove only the last empty positions.
      final RechnungsPositionDO position = obj.getPositionen().get(i);
      if (position.getId() == null && position.isEmpty() == true) {
        obj.getPositionen().remove(i);
      } else {
        break;
      }
    }
    writeUiStatusToXml(obj);
  }

  @Override
  public void afterLoad(final RechnungDO obj)
  {
    readUiStatusFromXml(obj);
  }

  @Override
  protected void afterSaveOrModify(final RechnungDO obj)
  {
    getRechnungCache().setExpired(); // Expire the cache because assignments to order position may be changed.
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#prepareHibernateSearch(org.projectforge.core.ExtendedBaseDO,
   *      org.projectforge.framework.access.OperationType)
   */
  @Override
  protected void prepareHibernateSearch(final RechnungDO obj, final OperationType operationType)
  {
    projektDao.initializeProjektManagerGroup(obj.getProjekt());
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * Fetches the cost assignments.
   * 
   * @see org.projectforge.framework.persistence.api.BaseDao#getById(java.io.Serializable)
   */
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  @Override
  public RechnungDO getById(final Serializable id) throws AccessException
  {
    final RechnungDO rechnung = super.getById(id);
    for (final RechnungsPositionDO pos : rechnung.getPositionen()) {
      final List<KostZuweisungDO> list = pos.getKostZuweisungen();
      if (list != null && list.size() > 0) {
        // Kostzuweisung is initialized
      }
    }
    return rechnung;
  };

  @Override
  public List<RechnungDO> getList(final BaseSearchFilter filter)
  {
    final RechnungFilter myFilter;
    if (filter instanceof RechnungFilter) {
      myFilter = (RechnungFilter) filter;
    } else {
      myFilter = new RechnungFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    if (myFilter.getFromDate() != null || myFilter.getToDate() != null) {
      if (myFilter.getFromDate() != null && myFilter.getToDate() != null) {
        queryFilter.add(Restrictions.between("datum", myFilter.getFromDate(), myFilter.getToDate()));
      } else if (myFilter.getFromDate() != null) {
        queryFilter.add(Restrictions.ge("datum", myFilter.getFromDate()));
      } else if (myFilter.getToDate() != null) {
        queryFilter.add(Restrictions.le("datum", myFilter.getToDate()));
      }
    } else {
      queryFilter.setYearAndMonth("datum", myFilter.getYear(), myFilter.getMonth());
    }
    queryFilter.addOrder(Order.desc("datum"));
    queryFilter.addOrder(Order.desc("nummer"));
    if (myFilter.isShowKostZuweisungStatus() == true) {
      queryFilter.setFetchMode("positionen.kostZuweisungen", FetchMode.JOIN);
    }
    final List<RechnungDO> list = getList(queryFilter);
    if (myFilter.isShowAll() == true || myFilter.isDeleted() == true) {
      return list;
    }
    final List<RechnungDO> result = new ArrayList<RechnungDO>();
    for (final RechnungDO rechnung : list) {
      if (myFilter.isShowUnbezahlt() == true) {
        if (rechnung.isBezahlt() == false) {
          result.add(rechnung);
        }
      } else if (myFilter.isShowBezahlt() == true) {
        if (rechnung.isBezahlt() == true) {
          result.add(rechnung);
        }
      } else if (myFilter.isShowUeberFaellig() == true) {
        if (rechnung.isUeberfaellig() == true) {
          result.add(rechnung);
        }
      } else {
        log.error("Unknown filter setting: " + myFilter.listType);
        break;
      }
    }
    return result;
  }

  @Override
  public List<RechnungDO> sort(final List<RechnungDO> list)
  {
    Collections.sort(list);
    return list;
  }

  /**
   * Gets the highest Rechnungsnummer.
   * 
   * @param rechnung wird benötigt, damit geschaut werden kann, ob diese Rechnung ggf. schon existiert. Wenn sie schon
   *          eine Nummer hatte, so kann verhindert werden, dass sie eine nächst höhere Nummer bekommt. Eine solche
   *          Rechnung bekommt die alte Nummer wieder zugeordnet.
   */
  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public Integer getNextNumber(final RechnungDO rechnung)
  {
    if (rechnung.getId() != null) {
      final RechnungDO orig = internalGetById(rechnung.getId());
      if (orig.getNummer() != null) {
        rechnung.setNummer(orig.getNummer());
        return orig.getNummer();
      }
    }
    final List<Integer> list = getSession().createQuery("select max(t.nummer) from RechnungDO t").list();
    Validate.notNull(list);
    if (list.size() == 0 || list.get(0) == null) {
      log.info("First entry of RechnungDO");
      return START_NUMBER;
    }
    Integer number = list.get(0);
    return ++number;
  }

  /**
   * Gets history entries of super and adds all history entries of the RechnungsPositionDO childs.
   * 
   * @see org.projectforge.framework.persistence.api.BaseDao#getDisplayHistoryEntries(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final RechnungDO obj)
  {
    final List<DisplayHistoryEntry> list = super.getDisplayHistoryEntries(obj);
    if (hasLoggedInUserHistoryAccess(obj, false) == false) {
      return list;
    }
    if (CollectionUtils.isNotEmpty(obj.getPositionen()) == true) {
      for (final RechnungsPositionDO position : obj.getPositionen()) {
        final List<DisplayHistoryEntry> entries = internalGetDisplayHistoryEntries(position);
        for (final DisplayHistoryEntry entry : entries) {
          final String propertyName = entry.getPropertyName();
          if (propertyName != null) {
            entry.setPropertyName("#" + position.getNumber() + ":" + entry.getPropertyName()); // Prepend number of positon.
          } else {
            entry.setPropertyName("#" + position.getNumber());
          }
        }
        list.addAll(entries);
        if (CollectionUtils.isNotEmpty(position.getKostZuweisungen()) == true) {
          for (final KostZuweisungDO zuweisung : position.getKostZuweisungen()) {
            final List<DisplayHistoryEntry> kostEntries = internalGetDisplayHistoryEntries(zuweisung);
            for (final DisplayHistoryEntry entry : kostEntries) {
              final String propertyName = entry.getPropertyName();
              if (propertyName != null) {
                entry.setPropertyName(
                    "#" + position.getNumber() + ".kost#" + zuweisung.getIndex() + ":" + entry.getPropertyName()); // Prepend
                // number of positon and index of zuweisung.
              } else {
                entry.setPropertyName("#" + position.getNumber() + ".kost#" + zuweisung.getIndex());
              }
            }
            list.addAll(kostEntries);
          }
        }
      }
    }
    Collections.sort(list, new Comparator<DisplayHistoryEntry>()
    {
      @Override
      public int compare(final DisplayHistoryEntry o1, final DisplayHistoryEntry o2)
      {
        return (o2.getTimestamp().compareTo(o1.getTimestamp()));
      }
    });
    return list;
  }

  @Override
  protected Class<?>[] getAdditionalHistorySearchDOs()
  {
    return ADDITIONAL_SEARCH_DOS;
  }

  /**
   * Returns also true, if idSet contains the id of any order position.
   * 
   * @see org.projectforge.framework.persistence.api.BaseDao#contains(java.util.Set,
   *      org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected boolean contains(final Set<Integer> idSet, final RechnungDO entry)
  {
    if (super.contains(idSet, entry) == true) {
      return true;
    }
    for (final RechnungsPositionDO pos : entry.getPositionen()) {
      if (idSet.contains(pos.getId()) == true) {
        return true;
      }
    }
    return false;
  }

  /**
   * Defined in application context.
   */
  public static BigDecimal getDefaultSteuersatz()
  {
    return defaultSteuersatz;
  }

  /**
   * Not static for invocation of Spring.
   * 
   * @param value
   */
  public void setDefaultSteuersatz(final BigDecimal value)
  {
    defaultSteuersatz = value;
  }

  @Override
  public RechnungDO newInstance()
  {
    return new RechnungDO();
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#useOwnCriteriaCacheRegion()
   */
  @Override
  protected boolean useOwnCriteriaCacheRegion()
  {
    return true;
  }
}
