/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.framework.persistence.attr.impl;

import java.util.Date;

import org.projectforge.framework.persistence.attr.api.EntityWithTimeableAttr;
import org.projectforge.framework.persistence.attr.api.TimeableAttrRow;
import org.projectforge.framework.persistence.attr.api.TimeableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Standard implementation for TimeableService.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 */
@Service
public class TimeableServiceImpl implements TimeableService
{
  @Autowired
  private GuiAttrSchemaService attrSchemaService;

  /**
   * Validate timeable.
   *
   * @param ctx the ctx
   * @param list the list
   * @return true, if successful
   */
  //  public boolean validateTimeable(final FrontendValidationContext ctx, final List<TimeableBaseDO<?>> list)
  //  {
  //    if (list.isEmpty() == true) {
  //      return true;
  //    }
  //    list.sort(Comparator.comparing(TimeableBaseDO::getStartTime));
  //    final Stream<TimeableBaseDO<?>> noEndTime = list.stream().filter(a -> a.getEndTime() == null);
  //    if (noEndTime.count() > 1) {
  //      // TODO collect elements
  //      noEndTime.collect(Collectors.toList());
  //      ctx.addValidationMessage(ValidationState.Error, "TODO only one empty");
  //      return false;
  //    }
  //
  //    return true;
  //  }

  /**
   * @see org.projectforge.framework.persistence.attr.api.TimeableService#getRowForTime(java.util.Date,
   *      org.projectforge.framework.persistence.attr.api.EntityWithTimeableAttr)
   */
  @Override
  public <T extends TimeableAttrRow> T getRowForTime(final Date date, final EntityWithTimeableAttr<T> entity)
  {
    T lastRow = null;

    for (final T td : entity.getTimeableAttributes()) {
      if (td.getStartTime() == null || td.getStartTime().getTime() > date.getTime()) {
        continue;
      }
      if (td.getEndTime() != null && td.getEndTime().getTime() < date.getTime()) {
        continue;
      }
      lastRow = td;
      break;
    }
    return lastRow;
  }

  public <R, T extends TimeableAttrRow> R getDefaultAttrValue(final EntityWithTimeableAttr<T> entity,
      final String propertyName, final Class<R> expectedClass)
  {
    return attrSchemaService.getDefaultValue(entity.getAttrSchemaName(), propertyName, expectedClass);
  }

  /**
   * @see org.projectforge.framework.persistence.attr.api.TimeableService#getAttrValue(java.util.Date,
   *      org.projectforge.framework.persistence.attr.api.EntityWithTimeableAttr, java.lang.String, java.lang.Class)
   */
  @Override
  public <R, T extends TimeableAttrRow> R getAttrValue(final Date date, final EntityWithTimeableAttr<T> entity,
      final String propertyName, final Class<R> expectedClass)
  {
    final T row = getRowForTime(date, entity);
    if (row == null) {
      return getDefaultAttrValue(entity, propertyName, expectedClass);
    }
    final R ret = row.getAttr(propertyName, expectedClass);
    if (ret == null) {
      return getDefaultAttrValue(entity, propertyName, expectedClass);
    }
    return ret;
  }

  @Override
  public <R, T extends TimeableAttrRow> R getAttrValue(final EntityWithTimeableAttr<T> entity,
      final String propertyName, final Class<R> expectedClass)
  {
    return getAttrValue(new Date(), entity, propertyName, expectedClass);
  }

}
