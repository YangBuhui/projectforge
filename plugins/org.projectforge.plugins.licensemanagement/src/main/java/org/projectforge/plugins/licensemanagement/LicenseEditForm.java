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

package org.projectforge.plugins.licensemanagement;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UsersComparator;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.user.UsersProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteMaxLengthTextField;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FileUploadPanel;
import org.projectforge.web.wicket.flowlayout.IconButtonPanel;
import org.projectforge.web.wicket.flowlayout.IconType;
import org.projectforge.web.wicket.flowlayout.InputPanel;

import com.vaynberg.wicket.select2.Select2MultiChoice;

/**
 * This is the edit formular page.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class LicenseEditForm extends AbstractEditForm<LicenseDO, LicenseEditPage>
{
  private static final long serialVersionUID = -6208809585214296635L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LicenseEditForm.class);

  MultiChoiceListHelper<PFUserDO> assignOwnersListHelper;

  protected FileUploadPanel fileUploadPanel1, fileUploadPanel2;

  @SpringBean
  UserDao userDao;

  @SpringBean
  UserRightService userRights;

  public LicenseEditForm(final LicenseEditPage parentPage, final LicenseDO data)
  {
    super(parentPage, data);
    setMultiPart(true);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    {
      // Organization
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("organization"));
      final PFAutoCompleteMaxLengthTextField organizationField = new PFAutoCompleteMaxLengthTextField(
          InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "organization"))
      {
        @Override
        protected List<String> getChoices(final String input)
        {
          return getBaseDao().getAutocompletion("organization", input);
        }
      };
      organizationField.withMatchContains(true).withMinChars(2).withFocus(true);
      WicketUtils.setStrong(organizationField);
      fs.add(organizationField);
    }
    {
      // Product
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.licensemanagement.product"));
      final PFAutoCompleteMaxLengthTextField productField = new PFAutoCompleteMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "product"))
      {
        @Override
        protected List<String> getChoices(final String input)
        {
          return getBaseDao().getAutocompletion("product", input);
        }
      };
      productField.withMatchContains(true).withMinChars(2);
      productField.setRequired(true);
      WicketUtils.setStrong(productField);
      fs.add(productField);
    }
    {
      // Version
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.licensemanagement.version"));
      final RequiredMaxLengthTextField versionField = new RequiredMaxLengthTextField(fs.getTextFieldId(),
          new PropertyModel<String>(data,
              "version"));
      WicketUtils.setStrong(versionField);
      fs.add(versionField);
    }
    {
      // UpdateFromVersion
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.licensemanagement.updateFromVersion"));
      fs.add(new MaxLengthTextField(fs.getTextFieldId(), new PropertyModel<String>(data, "updateFromVersion")));
      fs.addHelpIcon(getString("plugins.licensemanagement.updateFromVersion.tooltip"));
    }
    {
      // Device
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.licensemanagement.device"));
      fs.add(new MaxLengthTextField(fs.getTextFieldId(), new PropertyModel<String>(data, "device")));
      fs.addHelpIcon(getString("plugins.licensemanagement.device.tooltip"));
    }
    {
      // Number of license:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.licensemanagement.numberOfLicenses"));
      final MinMaxNumberField<Integer> maxNumberField = new MinMaxNumberField<Integer>(InputPanel.WICKET_ID,
          new PropertyModel<Integer>(
              data, "numberOfLicenses"),
          0, 999999);
      WicketUtils.setSize(maxNumberField, 6);
      fs.add(maxNumberField);
    }
    {
      // Owners
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.licensemanagement.owner"))
          .setLabelSide(false);
      final UsersProvider usersProvider = new UsersProvider(userDao);
      assignOwnersListHelper = new MultiChoiceListHelper<PFUserDO>().setComparator(new UsersComparator()).setFullList(
          usersProvider.getSortedUsers());
      final Collection<PFUserDO> owners = ((LicenseDao) getBaseDao()).getSortedOwners(data);
      if (owners != null) {
        for (final PFUserDO owner : owners) {
          assignOwnersListHelper.addOriginalAssignedItem(owner).assignItem(owner);
        }
      }
      final Select2MultiChoice<PFUserDO> ownersChoice = new Select2MultiChoice<PFUserDO>(fs.getSelect2MultiChoiceId(),
          new PropertyModel<Collection<PFUserDO>>(this.assignOwnersListHelper, "assignedItems"), usersProvider);
      fs.add(ownersChoice);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Valid since
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.licensemanagement.validSince"));
      final DatePanel validSinceDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<Date>(data, "validSince"),
          DatePanelSettings
              .get().withTargetType(java.sql.Date.class).withSelectProperty("validSince"));
      fs.add(validSinceDatePanel);
    }
    gridBuilder.newSplitPanel(GridSize.COL33);
    {
      // Valid until
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.licensemanagement.validUntil"));
      final DatePanel validUntilDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<Date>(data, "validUntil"),
          DatePanelSettings
              .get().withTargetType(java.sql.Date.class).withSelectProperty("validUntil"));
      fs.add(validUntilDatePanel);
    }
    gridBuilder.newGridPanel();
    {
      // License holder
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.licensemanagement.licenseHolder"));
      fs.add(new MaxLengthTextField(fs.getTextFieldId(), new PropertyModel<String>(data, "licenseHolder")));
    }
    final LicenseManagementRight right = (LicenseManagementRight) userRights
        .getRight(LicensemanagementPluginUserRightsId.PLUGIN_LICENSE_MANAGEMENT);
    {
      // Text key
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.licensemanagement.key"));
      if (right.isLicenseKeyVisible(getUser(), data) == true) {
        fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<String>(data, "key"))).setAutogrow();
      } else {
        fs.add(new DivTextPanel(fs.newChildId(), getString("plugins.licensemanagement.key.notvisible")));
        fs.addHelpIcon(getString("plugins.licensemanagement.key.notvisible.tooltip"));
      }
    }
    if ((isNew() == true && right.hasInsertAccess(getUser()) == true) //
        || (right.hasUpdateAccess(getUser(), data, null) == true
            && right.isLicenseKeyVisible(getUser(), data) == true)) {
      gridBuilder.newSplitPanel(GridSize.COL50);
      FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("plugins.licensemanagement.file1"));
      fileUploadPanel1 = new FileUploadPanel(fs.newChildId(), fs, this, true,
          new PropertyModel<String>(data, "filename1"),
          new PropertyModel<byte[]>(data, "file1"));
      if (isNew() == false && (data.getFile1() != null || data.getFile2() != null)) {
        // Swap files:
        fs.add(new IconButtonPanel(fs.newChildId(), IconType.SWAP, getString("plugins.licensemanagement.swapFiles"))
        {
          /**
           * @see org.projectforge.web.wicket.flowlayout.IconButtonPanel#onSubmit()
           */
          @Override
          protected void onSubmit()
          {
            final byte[] swap = data.getFile1();
            final String swapFilename = data.getFilename1();
            data.setFile1(data.getFile2());
            data.setFilename1(data.getFilename2());
            data.setFile2(swap);
            data.setFilename2(swapFilename);
          }
        });
      }
      gridBuilder.newSplitPanel(GridSize.COL50);
      fs = gridBuilder.newFieldset(gridBuilder.getString("plugins.licensemanagement.file2"));
      fileUploadPanel2 = new FileUploadPanel(fs.newChildId(), fs, this, true,
          new PropertyModel<String>(data, "filename2"),
          new PropertyModel<byte[]>(data, "file2"));
      gridBuilder.newGridPanel();
    } else if (right.isLicenseKeyVisible(getUser(), data) == true) {
    }
    {
      // Text comment
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("comment"));
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<String>(data, "comment"))).setAutogrow();
    }
    addCloneButton();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
