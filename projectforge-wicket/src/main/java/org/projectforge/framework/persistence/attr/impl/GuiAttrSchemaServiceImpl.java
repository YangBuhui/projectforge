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

package org.projectforge.framework.persistence.attr.impl;

import org.projectforge.web.common.timeattr.AttrWicketComponentFactory;
import org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;
import de.micromata.genome.util.bean.PrivateBeanUtils;

/**
 * Interface to handle with Attrs.
 *
 * TODO RK Move to web.
 * 
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
public class GuiAttrSchemaServiceImpl extends AttrSchemaServiceSpringBeanImpl implements GuiAttrSchemaService
{

  @Override
  public void createWicketComponent(final AbstractFieldsetPanel<?> panel, final AttrDescription desc,
      final EntityWithAttributes entity)
  {
    try {
      // TODO RK cachen der factory
      final Class<?> cls = Class.forName(desc.getWicketComponentFactoryClass());
      final AttrWicketComponentFactory factory = (AttrWicketComponentFactory) PrivateBeanUtils.createInstance(cls);
      factory.createComponents(panel, desc, entity);

    } catch (final ClassNotFoundException ex) {
      throw new UnsupportedOperationException(
          "Attr cannot load component factory: " + desc.getPropertyName() + "; "
              + desc.getWicketComponentFactoryClass());
    }

  }
}
