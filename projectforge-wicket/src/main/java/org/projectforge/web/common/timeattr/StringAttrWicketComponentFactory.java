/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.common.timeattr;

import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;

/**
 * A String input field.
 * 
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
public class StringAttrWicketComponentFactory implements AttrWicketComponentFactory
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
    final MaxLengthTextField mm = new MaxLengthTextField(InputPanel.WICKET_ID,
        new AttrModel<String>(entity, desc.getPropertyName(), String.class),
        desc.getMaxLength());
    mm.setRequired(desc.isRequired());
    panel.add(mm);
  }

}
