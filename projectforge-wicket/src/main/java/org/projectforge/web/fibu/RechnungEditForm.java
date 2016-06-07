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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.fibu.AbstractRechnungsPositionDO;
import org.projectforge.business.fibu.KontoCache;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.RechnungDO;
import org.projectforge.business.fibu.RechnungStatus;
import org.projectforge.business.fibu.RechnungTyp;
import org.projectforge.business.fibu.RechnungsPositionDO;
import org.projectforge.business.fibu.kost.AccountingConfig;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.KostZuweisungDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.PresizedImage;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;

public class RechnungEditForm extends AbstractRechnungEditForm<RechnungDO, RechnungsPositionDO, RechnungEditPage>
{
  private static final long serialVersionUID = -6018131069720611834L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RechnungEditForm.class);

  @SpringBean
  KontoCache kontoCache;

  private DropDownChoice<RechnungStatus> statusChoice;

  protected NewCustomerSelectPanel customerSelectPanel;

  protected NewProjektSelectPanel projektSelectPanel;

  public RechnungEditForm(final RechnungEditPage parentPage, final RechnungDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  @Override
  protected void onInit()
  {
    gridBuilder.newGridPanel();
    {
      // Subject
      final FieldsetPanel fs = gridBuilder.newFieldset(RechnungDO.class, "betreff");
      final MaxLengthTextField subject = new RequiredMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "betreff"));
      subject.add(WicketUtils.setFocus());
      fs.add(subject);
    }
    // GRID 50% - BLOCK
    gridBuilder.newSplitPanel(GridSize.COL50, true).newSubSplitPanel(GridSize.COL50);
    {
      // Number
      final FieldsetPanel fs = gridBuilder.newFieldset(RechnungDO.class, "nummer");
      final MinMaxNumberField<Integer> number = new MinMaxNumberField<Integer>(InputPanel.WICKET_ID,
          new PropertyModel<Integer>(data,
              "nummer"),
          0, 99999999);
      number.setMaxLength(8).add(AttributeModifier.append("style", "width: 6em !important;"));
      fs.add(number);
      if (NumberHelper.greaterZero(getData().getNummer()) == false) {
        fs.addHelpIcon(getString("fibu.tooltip.nummerWirdAutomatischVergeben"));
      }
    }
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    {
      // Status
      final FieldsetPanel fs = gridBuilder.newFieldset(RechnungDO.class, "status");
      final LabelValueChoiceRenderer<RechnungStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<RechnungStatus>(
          this,
          RechnungStatus.values());
      statusChoice = new DropDownChoice<RechnungStatus>(fs.getDropDownChoiceId(),
          new PropertyModel<RechnungStatus>(data, "status"),
          statusChoiceRenderer.getValues(), statusChoiceRenderer);
      statusChoice.setNullValid(false);
      statusChoice.setRequired(true);
      fs.add(statusChoice);
    }
    {
      // Type
      gridBuilder.newSubSplitPanel(GridSize.COL50);
      final FieldsetPanel fs = gridBuilder.newFieldset(RechnungDO.class, "typ");
      final LabelValueChoiceRenderer<RechnungTyp> typeChoiceRenderer = new LabelValueChoiceRenderer<RechnungTyp>(this,
          RechnungTyp.values());
      final DropDownChoice<RechnungTyp> typeChoice = new DropDownChoice<RechnungTyp>(fs.getDropDownChoiceId(),
          new PropertyModel<RechnungTyp>(data, "typ"), typeChoiceRenderer.getValues(), typeChoiceRenderer);
      typeChoice.setNullValid(false);
      typeChoice.setRequired(true);
      fs.add(typeChoice);
    }
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    if (kontoCache.isEmpty() == false) {
      // Show this field only if DATEV accounts does exist.
      final FieldsetPanel fs = gridBuilder.newFieldset(RechnungDO.class, "konto");
      final KontoSelectPanel kontoSelectPanel = new KontoSelectPanel(fs.newChildId(),
          new PropertyModel<KontoDO>(data, "konto"), null,
          "kontoId");
      kontoSelectPanel.setKontoNumberRanges(AccountingConfig.getInstance().getDebitorsAccountNumberRanges());
      fs.addHelpIcon(getString("fibu.rechnung.konto.tooltip"));
      fs.add(kontoSelectPanel);
      kontoSelectPanel.init();
    }
    gridBuilder.newSubSplitPanel(GridSize.COL100);
    {
      // Projekt
      final FieldsetPanel fs = gridBuilder.newFieldset(RechnungDO.class, "projekt").suppressLabelForWarning();
      projektSelectPanel = new NewProjektSelectPanel(fs.newChildId(), new PropertyModel<ProjektDO>(data,
          "projekt"), parentPage, "projektId");
      projektSelectPanel.getTextField().add(new AjaxFormComponentUpdatingBehavior("change")
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          if (getData().getKundeId() == null && StringUtils.isBlank(getData().getKundeText()) == true) {
            getData().setKunde(projektSelectPanel.getModelObject().getKunde());
          }
          target.add(customerSelectPanel.getTextField());
        }
      });
      fs.add(projektSelectPanel);
      projektSelectPanel.init();
    }
    {
      // Customer
      final FieldsetPanel fs = gridBuilder.newFieldset(RechnungDO.class, "kunde");
      customerSelectPanel = new NewCustomerSelectPanel(fs.newChildId(), new PropertyModel<KundeDO>(data, "kunde"),
          new PropertyModel<String>(data, "kundeText"), parentPage, "kundeId");
      customerSelectPanel.getTextField().setOutputMarkupId(true);
      fs.add(customerSelectPanel);
      customerSelectPanel.init();
      fs.setLabelFor(customerSelectPanel.getKundeTextField());
      fs.addHelpIcon(getString("fibu.rechnung.hint.kannVonProjektKundenAbweichen"));
    }
  }

  @SuppressWarnings("serial")
  @Override
  protected void onRenderPosition(final WebMarkupContainer item, final RechnungsPositionDO position)
  {
    // item.add(new AuftragsPositionFormComponent("orderPosition", new PropertyModel<AuftragsPositionDO>(position, "auftragsPosition"),
    // false));

    final Link<String> orderLink = new Link<String>("orderLink")
    {
      @Override
      public void onClick()
      {
        if (position.getAuftragsPosition() != null) {
          final PageParameters parameters = new PageParameters();
          parameters.add(AbstractEditPage.PARAMETER_KEY_ID, position.getAuftragsPosition().getAuftrag().getId());
          final AuftragEditPage auftragEditPage = new AuftragEditPage(parameters);
          auftragEditPage.setReturnToPage(getParentPage());
          setResponsePage(auftragEditPage);
        }
      }
    };
    item.add(orderLink);
    if (position.getAuftragsPosition() == null) {
      orderLink.setVisible(false);
    }
    orderLink.add(new PresizedImage("linkImage", WebConstants.IMAGE_FIND));
  }

  /**
   * Highlights the cost2 element if it differs from the cost2 of the given project (if any).
   * 
   * @param position
   * @param cost1
   * @param cost2
   */
  @Override
  protected void onRenderCostRow(final AbstractRechnungsPositionDO position, final KostZuweisungDO costAssignment,
      final Component cost1Component, final Component cost2Component)
  {
    final RechnungDO invoice = ((RechnungsPositionDO) position).getRechnung();
    if (invoice == null) {
      log.warn("Oups, no invoice given. Shouldn't occur!");
      return;
    }
    final Kost2DO cost2 = costAssignment.getKost2();
    final ProjektDO projekt = invoice.getProjekt();
    int numberRange = -1; // First number of cost.
    int area = -1; // Number 2-4
    int number = -1; // Number 5-6.
    if (projekt != null) {
      numberRange = projekt.getNummernkreis();
      area = projekt.getBereich();
      number = projekt.getNummer();
    } else {
      final KundeDO customer = invoice.getKunde();
      if (customer == null) {
        return;
      }
      numberRange = customer.getNummernkreis();
      number = customer.getBereich();
    }
    boolean differs = false;
    if (numberRange >= 0 && cost2.getNummernkreis() != numberRange) {
      differs = true;
    } else if (area >= 0 && cost2.getBereich() != area) {
      differs = true;
    } else if (number >= 0 && cost2.getTeilbereich() != number) {
      differs = true;
    }
    if (differs == true) {
      WicketUtils.setWarningTooltip(cost2Component);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void validation()
  {
    super.validation();

    final RechnungStatus status = statusChoice.getConvertedInput();
    final TextField<BigDecimal> zahlBetragField = (TextField<BigDecimal>) dependentFormComponents[3];
    final BigDecimal zahlBetrag = zahlBetragField.getConvertedInput();
    final Integer projektId = getData().getProjektId();
    final Integer kundeId = getData().getKundeId();
    // final String kundeText = customerSelectPanel.getKundeTextField().getConvertedInput();
    final boolean zahlBetragExists = (zahlBetrag != null && zahlBetrag.compareTo(BigDecimal.ZERO) != 0);
    if (status == RechnungStatus.BEZAHLT && zahlBetragExists == false) {
      addError("fibu.rechnung.error.statusBezahltErfordertZahlBetrag");
    }
    // if (projektId == null && StringUtils.isBlank(kundeText) == true && kundeId == null) {
    if (projektId == null && kundeId == null) {
      addError("fibu.rechnung.error.kundeTextOderProjektRequired");
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  protected RechnungsPositionDO newPositionInstance()
  {
    return new RechnungsPositionDO();
  }
}
