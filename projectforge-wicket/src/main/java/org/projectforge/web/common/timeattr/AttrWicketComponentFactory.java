/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.common.timeattr;

import org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;

/**
 * A factory, which creates edit components for Attr Values.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
public interface AttrWicketComponentFactory
{
  void createComponents(final AbstractFieldsetPanel<?> panel, final AttrDescription desc,
      final EntityWithAttributes entity);
}
