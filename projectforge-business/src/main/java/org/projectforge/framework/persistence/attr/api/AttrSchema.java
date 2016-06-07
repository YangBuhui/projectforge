/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.framework.persistence.attr.api;

import java.io.Serializable;
import java.util.List;

/**
 *
 * A Schema for Attributes
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
@Deprecated
public class AttrSchema implements Serializable
{
  private List<AttrDescription> columns;

  public AttrSchema()
  {

  }

  public AttrSchema(final List<AttrDescription> columns)
  {
    this.columns = columns;
  }

  public List<AttrDescription> getColumns()
  {
    return columns;
  }

  public void setColumns(final List<AttrDescription> columns)
  {
    this.columns = columns;

  }
}
