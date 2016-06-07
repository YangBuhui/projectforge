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

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.fibu.AbstractRechnungsStatistik;
import org.projectforge.business.fibu.RechnungFilter;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.YearListCoiceRenderer;
import org.projectforge.web.wicket.flowlayout.CheckBoxButton;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.RadioGroupPanel;
import org.projectforge.web.wicket.flowlayout.TextStyle;

public abstract class AbstractRechnungListForm<F extends RechnungFilter, P extends AbstractListPage< ? , ? , ? >> extends
AbstractListForm<F, P>
{
  private static final long serialVersionUID = 2678813484329104564L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AbstractRechnungListForm.class);

  protected int[] years;

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();
    gridBuilder.newGridPanel();
    {
      // Statistics
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("statistics")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject()
        {
          return getString("fibu.common.brutto") + ": " + CurrencyFormatter.format(getStats().getBrutto()) + WebConstants.HTML_TEXT_DIVIDER;
        }
      }));
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject()
        {
          return getString("fibu.common.netto") + ": " + CurrencyFormatter.format(getStats().getNetto()) + WebConstants.HTML_TEXT_DIVIDER;
        }
      }));
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject()
        {
          return getString("fibu.rechnung.offen") + ": " + CurrencyFormatter.format(getStats().getOffen()) + WebConstants.HTML_TEXT_DIVIDER;
        }
      }, TextStyle.BLUE));
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject()
        {
          return getString("fibu.rechnung.filter.ueberfaellig") + ": " + CurrencyFormatter.format(getStats().getUeberfaellig());
        }
      }, TextStyle.RED));
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject()
        {
          return WebConstants.HTML_TEXT_DIVIDER
              + getString("fibu.rechnung.skonto")
              + ": "
              + CurrencyFormatter.format(getStats().getSkonto())
              + WebConstants.HTML_TEXT_DIVIDER;
        }
      }));
      // fieldset.add(new HtmlCodePanel(fieldset.newChildId(), "<br/>"));
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject()
        {
          return getString("fibu.rechnung.zahlungsZiel")
              + ": Ø "
              + String.valueOf(getStats().getZahlungszielAverage())
              + WebConstants.HTML_TEXT_DIVIDER;
        }
      }));
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject()
        {
          return getString("fibu.rechnung.zahlungsZiel.actual") + ": Ø " + String.valueOf(getStats().getTatsaechlichesZahlungzielAverage());
        }
      }));
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   *      org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @SuppressWarnings("serial")
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    // DropDownChoice years
    final YearListCoiceRenderer yearListChoiceRenderer = new YearListCoiceRenderer(years, true);
    final DropDownChoice<Integer> yearChoice = new DropDownChoice<Integer>(optionsFieldsetPanel.getDropDownChoiceId(),
        new PropertyModel<Integer>(this, "year"), yearListChoiceRenderer.getYears(), yearListChoiceRenderer);
    yearChoice.setNullValid(false);
    optionsFieldsetPanel.add(yearChoice, true);

    // // DropDownChoice months
    final LabelValueChoiceRenderer<Integer> monthChoiceRenderer = new LabelValueChoiceRenderer<Integer>();
    monthChoiceRenderer.addValue(-1, "");
    for (int i = 0; i <= 11; i++) {
      monthChoiceRenderer.addValue(i, StringHelper.format2DigitNumber(i + 1));
    }
    final DropDownChoice<Integer> monthChoice = new DropDownChoice<Integer>(optionsFieldsetPanel.getDropDownChoiceId(),
        new PropertyModel<Integer>(this, "month"), monthChoiceRenderer.getValues(), monthChoiceRenderer);
    monthChoice.setNullValid(false);
    optionsFieldsetPanel.add(monthChoice, true);

    final DivPanel radioGroupPanel = optionsFieldsetPanel.addNewRadioBoxButtonDiv();
    final RadioGroupPanel<String> radioGroup = new RadioGroupPanel<String>(radioGroupPanel.newChildId(), "listtype",
        new PropertyModel<String>(getSearchFilter(), "listType")) {
      /**
       * @see org.projectforge.web.wicket.flowlayout.RadioGroupPanel#wantOnSelectionChangedNotifications()
       */
      @Override
      protected boolean wantOnSelectionChangedNotifications()
      {
        return true;
      }

      /**
       * @see org.projectforge.web.wicket.flowlayout.RadioGroupPanel#onSelectionChanged(java.lang.Object)
       */
      @Override
      protected void onSelectionChanged(final Object newSelection)
      {
        parentPage.refresh();
      }
    };
    radioGroupPanel.add(radioGroup);
    radioGroup.add(new Model<String>("all"), getString("filter.all"));
    radioGroup.add(new Model<String>("unbezahlt"), getString("fibu.rechnung.filter.unbezahlt"));
    radioGroup.add(new Model<String>("ueberfaellig"), getString("fibu.rechnung.filter.ueberfaellig"));

    if (Configuration.getInstance().isCostConfigured() == true) {
      optionsCheckBoxesPanel.add(new CheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(),
          "showKostZuweisungStatus"), getString("fibu.rechnung.showKostZuweisungstatus")) {
        /**
         * @see org.projectforge.web.wicket.flowlayout.CheckBoxButton#wantOnSelectionChangedNotifications()
         */
        @Override
        protected boolean wantOnSelectionChangedNotifications()
        {
          return true;
        }

        /**
         * @see org.projectforge.web.wicket.flowlayout.CheckBoxButton#onSelectionChanged()
         */
        @Override
        protected void onSelectionChanged(final Boolean newSelection)
        {
          parentPage.refresh();
        }
      });
    }
  }

  protected abstract AbstractRechnungsStatistik< ? > getStats();

  public Integer getYear()
  {
    return getSearchFilter().getYear();
  }

  public void setYear(final Integer year)
  {
    if (year == null) {
      getSearchFilter().setYear(-1);
    } else {
      getSearchFilter().setYear(year);
    }
  }

  public Integer getMonth()
  {
    return getSearchFilter().getMonth();
  }

  public void setMonth(final Integer month)
  {
    if (month == null) {
      getSearchFilter().setMonth(-1);
    } else {
      getSearchFilter().setMonth(month);
    }
  }

  public AbstractRechnungListForm(final P parentPage)
  {
    super(parentPage);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
