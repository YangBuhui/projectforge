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

package org.projectforge.plugins.todo;

import org.apache.log4j.Logger;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.mobile.AbstractMobileEditPage;
import org.projectforge.web.mobile.AbstractMobileListPage;

public class ToDoMobileEditPage extends AbstractMobileEditPage<ToDoDO, ToDoMobileEditForm, ToDoDao>
{
  private static final long serialVersionUID = 3060701092253890337L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ToDoMobileEditPage.class);

  @SpringBean
  private ToDoDao toDoDao;

  public ToDoMobileEditPage(final PageParameters parameters)
  {
    super(parameters, "toDo");
    init();
  }

  @Override
  protected ToDoDao getBaseDao()
  {
    return toDoDao;
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  protected ToDoMobileEditForm newEditForm(final AbstractMobileEditPage<?, ?, ?> parentPage, final ToDoDO data)
  {
    return new ToDoMobileEditForm(this, data);
  }

  @Override
  protected Class<? extends AbstractMobileListPage<?, ?, ?>> getListPageClass()
  {
    return ToDoMobileListPage.class;
  }
}
