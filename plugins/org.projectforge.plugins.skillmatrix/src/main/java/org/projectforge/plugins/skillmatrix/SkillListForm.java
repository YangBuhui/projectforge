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

package org.projectforge.plugins.skillmatrix;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.model.Model;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.components.SingleButtonPanel;

/**
 * @author Billy Duong (b.duong@micromata.de)
 * 
 */
public class SkillListForm extends AbstractListForm<SkillFilter, SkillListPage>
{
  private static final long serialVersionUID = 5333752125044497290L;

  private static final Logger log = Logger.getLogger(SkillListForm.class);

  /**
   * @param parentPage
   */
  public SkillListForm(final SkillListPage parentPage)
  {
    super(parentPage);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#init()
   */
  @Override
  protected void init()
  {
    super.init();
    final Button skillTreeButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("treeView"))
    {
      private static final long serialVersionUID = -8763718088111525575L;

      @Override
      public void onSubmit()
      {
        parentPage.onTreeViewSubmit();
      }
    };
    actionButtons.add(2, new SingleButtonPanel(actionButtons.newChildId(), skillTreeButton, "Tree View",
        SingleButtonPanel.NORMAL));
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#newSearchFilterInstance()
   */
  @Override
  protected SkillFilter newSearchFilterInstance()
  {
    return new SkillFilter();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

}
