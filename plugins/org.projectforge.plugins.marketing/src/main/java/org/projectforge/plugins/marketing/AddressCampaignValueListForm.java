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

package org.projectforge.plugins.marketing;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.address.AddressListForm;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class AddressCampaignValueListForm
    extends AbstractListForm<AddressCampaignValueFilter, AddressCampaignValueListPage>
{
  private static final long serialVersionUID = 6190615904711764514L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(AddressCampaignValueListForm.class);

  static final String ADDRESS_CAMPAIGN_VALUE_UNDEFINED = "-(null)-";

  @SpringBean
  private AddressCampaignDao addressCampaignDao;

  private Integer addressCampaignId;

  @SuppressWarnings("unused")
  private String addressCampaignValue;

  private DropDownChoice<String> addressCampaignValueDropDownChoice;

  public AddressCampaignValueListForm(final AddressCampaignValueListPage parentPage)
  {
    super(parentPage);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    this.addressCampaignId = searchFilter.getAddressCampaignId();
    this.addressCampaignValue = searchFilter.getAddressCampaignValue();
    final List<AddressCampaignDO> addressCampaignList = addressCampaignDao.getList(new AddressCampaignValueFilter());
    gridBuilder.newSplitPanel(GridSize.COL66);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.marketing.addressCampaign"));
      final LabelValueChoiceRenderer<Integer> addressCampaignRenderer = new LabelValueChoiceRenderer<Integer>();
      for (final AddressCampaignDO addressCampaign : addressCampaignList) {
        addressCampaignRenderer.addValue(addressCampaign.getId(), addressCampaign.getTitle());
      }
      final DropDownChoice<Integer> addressCampaignChoice = new DropDownChoice<Integer>(fs.getDropDownChoiceId(),
          new PropertyModel<Integer>(this, "addressCampaignId"), addressCampaignRenderer.getValues(),
          addressCampaignRenderer)
      {
        @Override
        protected void onSelectionChanged(final Integer newSelection)
        {
          for (final AddressCampaignDO addressCampaign : addressCampaignList) {
            if (addressCampaign.getId().equals(addressCampaignId) == true) {
              searchFilter.setAddressCampaign(addressCampaign);
              final String oldValue = searchFilter.getAddressCampaignValue();
              // Is oldValue given and not "-(null)-"?
              if (oldValue != null && ADDRESS_CAMPAIGN_VALUE_UNDEFINED.equals(oldValue) == false) {
                // Check whether the campaign has the former selected value or not.
                boolean found = false;
                for (final String value : addressCampaign.getValuesArray()) {
                  if (oldValue.equals(value) == true) {
                    found = true;
                    break;
                  }
                }
                if (found == false) {
                  // Not found, therefore set the value to null:
                  searchFilter.setAddressCampaignValue(null);
                  addressCampaignValueDropDownChoice.modelChanged();
                }
              }
              break;
            }
          }
          refresh();
        }

        @Override
        protected boolean wantOnSelectionChangedNotifications()
        {
          return true;
        }
      };
      fs.add(addressCampaignChoice);
    }
    {
      gridBuilder.newSplitPanel(GridSize.COL33);
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("value"));
      final LabelValueChoiceRenderer<String> choiceRenderer = getValueLabelValueChoiceRenderer();
      addressCampaignValueDropDownChoice = new DropDownChoice<String>(fs.getDropDownChoiceId(),
          new PropertyModel<String>(this,
              "addressCampaignValue"),
          choiceRenderer.getValues(), choiceRenderer)
      {
        @Override
        protected void onSelectionChanged(final String newSelection)
        {
          searchFilter.setAddressCampaignValue(newSelection);
          parentPage.refresh();
        }

        @Override
        protected boolean wantOnSelectionChangedNotifications()
        {
          return true;
        }

      };
      addressCampaignValueDropDownChoice.setNullValid(true);
      fs.add(addressCampaignValueDropDownChoice);
    }
    AddressListForm.addFilter(parentPage, this, gridBuilder, getSearchFilter());
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   *      org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    AddressListForm.onOptionsPanelCreate(parentPage, optionsFieldsetPanel, searchFilter);
  }

  protected void refresh()
  {
    final LabelValueChoiceRenderer<String> choiceRenderer = getValueLabelValueChoiceRenderer();
    addressCampaignValueDropDownChoice.setChoiceRenderer(choiceRenderer);
    addressCampaignValueDropDownChoice.setChoices(choiceRenderer.getValues());
    parentPage.refresh();
  }

  private LabelValueChoiceRenderer<String> getValueLabelValueChoiceRenderer()
  {
    final LabelValueChoiceRenderer<String> choiceRenderer = new LabelValueChoiceRenderer<String>();
    if (searchFilter.getAddressCampaign() != null) {
      choiceRenderer.addValue(ADDRESS_CAMPAIGN_VALUE_UNDEFINED, "- " + getString("undefined") + " -");
      for (final String value : searchFilter.getAddressCampaign().getValuesArray()) {
        choiceRenderer.addValue(value, value);
      }
    }
    return choiceRenderer;
  }

  @Override
  protected boolean isFilterVisible()
  {
    return parentPage.isMassUpdateMode() == false;
  }

  @Override
  protected AddressCampaignValueFilter newSearchFilterInstance()
  {
    return new AddressCampaignValueFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
