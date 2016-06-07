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

import java.util.Date;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.PaymentScheduleDO;
import org.projectforge.business.fibu.RechnungDao;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.converter.CurrencyConverter;
import org.projectforge.web.wicket.flowlayout.CheckBoxButton;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivType;

/**
 * @author Werner Feder (werner.feder@t-online.de)
 */
public class PaymentSchedulePanel extends Panel
{
  private static final long serialVersionUID = 2669766778018430028L;

  private RepeatingView entrysRepeater;

  private WebMarkupContainer mainContainer;

  private final IModel<AuftragDO> model;

  private final PFUserDO user;

  @SpringBean
  AccessChecker accessChecker;

  /**
   * @param id
   */
  public PaymentSchedulePanel(final String id, final IModel<AuftragDO> model, final PFUserDO user)
  {
    super(id);
    this.model = model;
    this.user = user;
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    mainContainer = new WebMarkupContainer("main");
    add(mainContainer.setOutputMarkupId(true));
    mainContainer.add(new Label("dateLabel", getString("fibu.rechnung.datum.short")));
    mainContainer.add(new Label("amountLabel", getString("fibu.common.betrag")));
    mainContainer.add(new Label("commentLabel", getString("comment")));
    mainContainer.add(new Label("reachedLabel", getString("fibu.common.reached")));
    entrysRepeater = new RepeatingView("liRepeater");
    mainContainer.add(entrysRepeater);
    rebuildEntries();
    entrysRepeater.setVisible(true);
  }

  @SuppressWarnings("serial")
  public void rebuildEntries()
  {
    final List<PaymentScheduleDO> entries = model.getObject().getPaymentSchedules();
    if (entries != null) {
      entrysRepeater.removeAll();
      for (final PaymentScheduleDO entry : entries) {
        final WebMarkupContainer item = new WebMarkupContainer(entrysRepeater.newChildId());
        entrysRepeater.add(item);
        final DatePanel datePanel = new DatePanel("scheduleDate", new PropertyModel<Date>(entry, "scheduleDate"),
            DatePanelSettings.get()
                .withTargetType(java.sql.Date.class));
        item.add(datePanel);
        final TextField<String> amount = new TextField<String>("amount", new PropertyModel<String>(entry, "amount"))
        {
          @SuppressWarnings({ "rawtypes", "unchecked" })
          @Override
          public IConverter getConverter(final Class type)
          {
            return new CurrencyConverter();
          }
        };
        item.add(amount);
        item.add(new MaxLengthTextField("comment", new PropertyModel<String>(entry, "comment")));
        item.add(new CheckBox("reached", new PropertyModel<Boolean>(entry, "reached")));
        if (accessChecker.hasRight(user, RechnungDao.USER_RIGHT_ID, UserRightValue.READWRITE) == true) {
          final DivPanel checkBoxDiv = new DivPanel("vollstaendigFakturiert", DivType.BTN_GROUP);
          item.add(checkBoxDiv);
          checkBoxDiv.add(
              new CheckBoxButton(checkBoxDiv.newChildId(), new PropertyModel<Boolean>(entry, "vollstaendigFakturiert"),
                  getString("fibu.auftrag.vollstaendigFakturiert")));
        } else {
          item.add(WicketUtils.getInvisibleComponent("vollstaendigFakturiert"));
        }
      }
    }
  }
}
