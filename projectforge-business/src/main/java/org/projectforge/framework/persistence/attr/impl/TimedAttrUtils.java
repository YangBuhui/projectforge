/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2016, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.framework.persistence.attr.impl;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.projectforge.framework.persistence.attr.api.EntityWithAttributes;
import org.projectforge.framework.persistence.attr.api.EntityWithTimeableAttr;
import org.projectforge.framework.persistence.attr.api.TimeableAttrRow;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateHelper;

import de.micromata.genome.util.types.Pair;

/**
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
public class TimedAttrUtils
{
  private static final ThreadLocal<SimpleDateFormat> historyDateKeyFormatter = new ThreadLocal<SimpleDateFormat>()
  {

    @Override
    protected SimpleDateFormat initialValue()
    {
      final SimpleDateFormat df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_SECONDS);
      df.setTimeZone(DateHelper.UTC);
      return df;
    }

  };

  /**
   *
   * @param newEntity
   * @param oldEntity
   * @return Pair.first is new, Pair.second is old.
   */
  private static Map<Date, Pair<TimeableAttrRow, TimeableAttrRow>> genHistory(final EntityWithTimeableAttr<?> newEntity,
      final EntityWithTimeableAttr<?> oldEntity)
  {
    final Map<Date, Pair<TimeableAttrRow, TimeableAttrRow>> ret = new TreeMap<>();
    for (final TimeableAttrRow ar : newEntity.getTimeableAttributes()) {
      final Pair<TimeableAttrRow, TimeableAttrRow> re = ret.get(ar.getStartTime());
      if (re == null) {
        ret.put(ar.getStartTime(), new Pair<>(ar, null));
      } else {
        re.setFirst(ar);
      }
    }
    for (final TimeableAttrRow ar : oldEntity.getTimeableAttributes()) {
      final Pair<TimeableAttrRow, TimeableAttrRow> re = ret.get(ar.getStartTime());
      if (re == null) {
        ret.put(ar.getStartTime(), new Pair<>(null, ar));
      } else {
        re.setSecond(ar);
      }
    }
    return ret;
  }

  /**
   * An empty version of attributes.
   *
   * @author Roger Kommer (r.kommer.extern@micromata.de)
   *
   */
  private static class EmptyAttributes implements EntityWithAttributes
  {

    @Override
    public String getStringAttr(final String key)
    {
      return null;
    }

    @Override
    public void putStringAttr(final String key, final String value)
    {
    }

    @Override
    public Object getAttr(final String key)
    {
      return null;
    }

    @Override
    public <T> T getAttr(final String key, final Class<T> expectedClass)
    {
      return null;
    }

    @Override
    public void putAttr(final String key, final Object value)
    {
    }

    @Override
    public void removeAttr(final String key)
    {
    }

    @Override
    public Set<String> getAttrKeys()
    {
      return Collections.emptySet();
    }

  }

}
