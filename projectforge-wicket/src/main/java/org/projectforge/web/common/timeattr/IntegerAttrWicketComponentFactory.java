/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.common.timeattr;

import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;

/**
 * Creates an Edit field for Integer
 * 
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
public class IntegerAttrWicketComponentFactory implements AttrWicketComponentFactory
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
    final MinMaxNumberField<Integer> mm = new MinMaxNumberField<>(InputPanel.WICKET_ID,
        new AttrModel<Integer>(entity, desc.getPropertyName(), Integer.class), desc.getMinIntValue(),
        desc.getMaxIntValue());
    mm.setRequired(desc.isRequired());
    panel.add(mm);
  }

}
