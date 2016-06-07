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

package org.projectforge.web.fibu;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeStatus;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.common.timeattr.TimedAttributeTogglePanel;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;

public class EmployeeEditForm extends AbstractEditForm<EmployeeDO, EmployeeEditPage>
{
  private static final long serialVersionUID = 8746545908106124484L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeEditForm.class);

  private static final BigDecimal NUMBER_OF_WEEK_HOURS = new BigDecimal(168);

  @SpringBean
  private EmployeeService employeeService;

  public EmployeeEditForm(final EmployeeEditPage parentPage, final EmployeeDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // User
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "user");
      final UserSelectPanel userSelectPanel = new UserSelectPanel(fs.newChildId(),
          new PropertyModel<PFUserDO>(data, "user"), parentPage,
          "userId");
      userSelectPanel.setShowSelectMeButton(false).setRequired(true);
      fs.add(userSelectPanel);
      userSelectPanel.init();
    }
    {
      // cost 1
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "kost1");
      fs.add(new Kost1FormComponent(InputPanel.WICKET_ID, new PropertyModel<Kost1DO>(data, "kost1"), true));
    }
    {
      // DropDownChoice status
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "status");
      final LabelValueChoiceRenderer<EmployeeStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<EmployeeStatus>(
          this,
          EmployeeStatus.values());
      final DropDownChoice<EmployeeStatus> statusChoice = new DropDownChoice<EmployeeStatus>(fs.getDropDownChoiceId(),
          new PropertyModel<EmployeeStatus>(data, "status"), statusChoiceRenderer.getValues(), statusChoiceRenderer);
      statusChoice.setNullValid(false).setRequired(true);
      fs.add(statusChoice);
    }
    {
      // Division
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "abteilung");
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "abteilung")));
    }
    {
      // Position
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "position");
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "position")));
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Weekly hours
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "weeklyWorkingHours");
      fs.add(new MinMaxNumberField<BigDecimal>(InputPanel.WICKET_ID,
          new PropertyModel<BigDecimal>(data, "weeklyWorkingHours"), BigDecimal.ZERO, NUMBER_OF_WEEK_HOURS));
    }
    {
      // Holidays
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "urlaubstage");
      fs.add(new MinMaxNumberField<Integer>(InputPanel.WICKET_ID, new PropertyModel<Integer>(data, "urlaubstage"), 0, 366));
    }
    {
      // Start date
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "eintrittsDatum");
      fs.add(new DatePanel(fs.newChildId(), new PropertyModel<Date>(data, "eintrittsDatum"), new DatePanelSettings()));
    }
    {
      // End date
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "austrittsDatum");
      fs.add(new DatePanel(fs.newChildId(), new PropertyModel<Date>(data, "austrittsDatum"), new DatePanelSettings()));
    }

    gridBuilder.newGridPanel();
    {
      // Comment
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "comment");
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "comment")), true);
    }

    gridBuilder.newSplitPanel(GridSize.COL100);
    {
      final TimedAttributeTogglePanel tattrpanel = new TimedAttributeTogglePanel(this,
          gridBuilder.getPanel().newChildId(),
          data, "employee")
      {

        @Override
        protected void addNewEntry()
        {
          employeeService.addNewTimeAttributeRow(data);
        }

      };
      if (tattrpanel.isAttrSchemaAvailable()) {
        tattrpanel.setClosed();
        tattrpanel.initHeading();
        gridBuilder.getPanel().add(tattrpanel);
      }
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
