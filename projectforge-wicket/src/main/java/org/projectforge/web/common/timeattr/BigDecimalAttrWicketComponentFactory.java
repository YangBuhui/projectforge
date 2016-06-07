/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.common.timeattr;

import java.math.BigDecimal;

import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;

/**
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
public class BigDecimalAttrWicketComponentFactory implements AttrWicketComponentFactory
{

  /**
   * @see org.projectforge.web.common.timeattr.AttrWicketComponentFactory#createComponents(org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel,
   *      org.projectforge.framework.persistence.attr.api.AttrDescription,
   *      org.projectforge.framework.persistence.attr.api.EntityWithAttributes)
   */
  @Override
  public void createComponents(final AbstractFieldsetPanel<?> panel, final AttrDescription desc,
      final EntityWithAttributes entity)
  {
    final MinMaxNumberField<BigDecimal> mm = new MinMaxNumberField<>(InputPanel.WICKET_ID,
        new AttrModel<BigDecimal>(entity, desc.getPropertyName(), BigDecimal.class),
        new BigDecimal(desc.getMinIntValue()),
        new BigDecimal(desc.getMaxIntValue()));
    mm.setRequired(desc.isRequired());
    panel.add(mm);
  }

}
