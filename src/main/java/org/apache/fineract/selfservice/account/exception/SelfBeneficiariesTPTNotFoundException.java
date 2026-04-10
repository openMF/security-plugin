package org.apache.fineract.selfservice.account.exception;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

public class SelfBeneficiariesTPTNotFoundException extends AbstractPlatformResourceNotFoundException {
  public SelfBeneficiariesTPTNotFoundException(final Long id) {
    super("error.msg.self.beneficiary.tpt.not.found", "SelfBeneficiariesTPT with identifier " + id + " does not exist", id);
  }
}
