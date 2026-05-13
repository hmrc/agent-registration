/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentregistration.crypto

import com.softwaremill.quicklens.*
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentDetails
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.companieshouse.ChroAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails

import javax.inject.Inject
import javax.inject.Singleton

/** Encrypts/decrypts the PII fields of an [[AgentApplication]] as a domain-model transform.
  *
  * The Mongo `Format[AgentApplication]` stays plaintext; `AgentApplicationRepo` applies this service on the way in (upsert) and out (find).
  *
  * Both `encrypt` and `decrypt` are driven from a single `transform(app, cryptoOp)` that lists every PII path exactly once, so the two sides cannot drift
  * apart.
  */
@Singleton
class AgentApplicationEncryption @Inject() (fieldLevelEncryption: FieldLevelEncryption):

  // Full-document encrypt / decrypt -----------------------------------------------------------

  def encrypt(app: AgentApplication): AgentApplication = transform(app, fieldLevelEncryption.encrypt)
  def decrypt(app: AgentApplication): AgentApplication = transform(app, fieldLevelEncryption.decrypt)

  // Type-preserving overloads — pick the right per-subtype transform so callers that already
  // hold a concrete subtype get the same subtype back without an unsafe cast.

  def encrypt(app: AgentApplicationLlp): AgentApplicationLlp = transformLlp(app, fieldLevelEncryption.encrypt)
  def decrypt(app: AgentApplicationLlp): AgentApplicationLlp = transformLlp(app, fieldLevelEncryption.decrypt)

  def encrypt(app: AgentApplicationSoleTrader): AgentApplicationSoleTrader = transformSoleTrader(app, fieldLevelEncryption.encrypt)
  def decrypt(app: AgentApplicationSoleTrader): AgentApplicationSoleTrader = transformSoleTrader(app, fieldLevelEncryption.decrypt)

  def encrypt(app: AgentApplicationLimitedCompany): AgentApplicationLimitedCompany = transformLimitedCompany(app, fieldLevelEncryption.encrypt)
  def decrypt(app: AgentApplicationLimitedCompany): AgentApplicationLimitedCompany = transformLimitedCompany(app, fieldLevelEncryption.decrypt)

  def encrypt(app: AgentApplicationGeneralPartnership): AgentApplicationGeneralPartnership = transformGeneralPartnership(app, fieldLevelEncryption.encrypt)
  def decrypt(app: AgentApplicationGeneralPartnership): AgentApplicationGeneralPartnership = transformGeneralPartnership(app, fieldLevelEncryption.decrypt)

  def encrypt(app: AgentApplicationLimitedPartnership): AgentApplicationLimitedPartnership = transformLimitedPartnership(app, fieldLevelEncryption.encrypt)
  def decrypt(app: AgentApplicationLimitedPartnership): AgentApplicationLimitedPartnership = transformLimitedPartnership(app, fieldLevelEncryption.decrypt)

  def encrypt(app: AgentApplicationScottishLimitedPartnership): AgentApplicationScottishLimitedPartnership = transformScottishLimitedPartnership(
    app,
    fieldLevelEncryption.encrypt
  )
  def decrypt(app: AgentApplicationScottishLimitedPartnership): AgentApplicationScottishLimitedPartnership = transformScottishLimitedPartnership(
    app,
    fieldLevelEncryption.decrypt
  )

  def encrypt(app: AgentApplicationScottishPartnership): AgentApplicationScottishPartnership = transformScottishPartnership(app, fieldLevelEncryption.encrypt)
  def decrypt(app: AgentApplicationScottishPartnership): AgentApplicationScottishPartnership = transformScottishPartnership(app, fieldLevelEncryption.decrypt)

  // Single-value helpers, for use in Mongo filters --------------------------------------------

  def encrypt(internalUserId: InternalUserId): InternalUserId = internalUserId.modify(_.value).using(fieldLevelEncryption.encrypt)
  def decrypt(internalUserId: InternalUserId): InternalUserId = internalUserId.modify(_.value).using(fieldLevelEncryption.decrypt)

  def encrypt(groupId: GroupId): GroupId = groupId.modify(_.value).using(fieldLevelEncryption.encrypt)
  def decrypt(groupId: GroupId): GroupId = groupId.modify(_.value).using(fieldLevelEncryption.decrypt)

  def encrypt(vrn: Vrn): Vrn = vrn.modify(_.value).using(fieldLevelEncryption.encrypt)
  def decrypt(vrn: Vrn): Vrn = vrn.modify(_.value).using(fieldLevelEncryption.decrypt)

  def encrypt(payeRef: PayeRef): PayeRef = payeRef.modify(_.value).using(fieldLevelEncryption.encrypt)
  def decrypt(payeRef: PayeRef): PayeRef = payeRef.modify(_.value).using(fieldLevelEncryption.decrypt)

  def encrypt(saUtr: SaUtr): SaUtr = saUtr.modify(_.value).using(fieldLevelEncryption.encrypt)
  def decrypt(saUtr: SaUtr): SaUtr = saUtr.modify(_.value).using(fieldLevelEncryption.decrypt)

  def encrypt(ctUtr: CtUtr): CtUtr = ctUtr.modify(_.value).using(fieldLevelEncryption.encrypt)
  def decrypt(ctUtr: CtUtr): CtUtr = ctUtr.modify(_.value).using(fieldLevelEncryption.decrypt)

  def encrypt(crn: Crn): Crn = crn.modify(_.value).using(fieldLevelEncryption.encrypt)
  def decrypt(crn: Crn): Crn = crn.modify(_.value).using(fieldLevelEncryption.decrypt)

  // Subtype dispatch --------------------------------------------------------------------------

  private def transform(
    app: AgentApplication,
    cryptoOp: String => String
  ): AgentApplication =
    app match
      case x: AgentApplicationLlp => transformLlp(x, cryptoOp)
      case x: AgentApplicationSoleTrader => transformSoleTrader(x, cryptoOp)
      case x: AgentApplicationLimitedCompany => transformLimitedCompany(x, cryptoOp)
      case x: AgentApplicationGeneralPartnership => transformGeneralPartnership(x, cryptoOp)
      case x: AgentApplicationLimitedPartnership => transformLimitedPartnership(x, cryptoOp)
      case x: AgentApplicationScottishLimitedPartnership => transformScottishLimitedPartnership(x, cryptoOp)
      case x: AgentApplicationScottishPartnership => transformScottishPartnership(x, cryptoOp)

  // Per-subtype transforms --------------------------------------------------------------------
  // Each subtype lists every PII field on it. The common-trait fields are repeated per subtype
  // because quicklens needs concrete types — the repetition is the audit surface, intentional.

  private def transformLlp(
    app: AgentApplicationLlp,
    cryptoOp: String => String
  ): AgentApplicationLlp = app
    .modify(_.internalUserId.value).using(cryptoOp)
    .modify(_.groupId.value).using(cryptoOp)
    .modify(_.applicantCredentials.providerId).using(cryptoOp)
    .modify(_.applicantContactDetails.each).using(transformApplicantContactDetails(_, cryptoOp))
    .modify(_.agentDetails.each).using(transformAgentDetails(_, cryptoOp))
    .modify(_.vrns.each.each.value).using(cryptoOp)
    .modify(_.payeRefs.each.each.value).using(cryptoOp)
    .modify(_.businessDetails.each.saUtr.value).using(cryptoOp)
    .modify(_.businessDetails.each.companyProfile).using(transformCompanyProfile(_, cryptoOp))

  private def transformSoleTrader(
    app: AgentApplicationSoleTrader,
    cryptoOp: String => String
  ): AgentApplicationSoleTrader = app
    .modify(_.internalUserId.value).using(cryptoOp)
    .modify(_.groupId.value).using(cryptoOp)
    .modify(_.applicantCredentials.providerId).using(cryptoOp)
    .modify(_.applicantContactDetails.each).using(transformApplicantContactDetails(_, cryptoOp))
    .modify(_.agentDetails.each).using(transformAgentDetails(_, cryptoOp))
    .modify(_.vrns.each.each.value).using(cryptoOp)
    .modify(_.payeRefs.each.each.value).using(cryptoOp)
    .modify(_.businessDetails.each.saUtr.value).using(cryptoOp)
    .modify(_.businessDetails.each.fullName.firstName).using(cryptoOp)
    .modify(_.businessDetails.each.fullName.lastName).using(cryptoOp)
    .modify(_.businessDetails.each.nino.each.value).using(cryptoOp)
    .modify(_.businessDetails.each.trn.each).using(cryptoOp)

  private def transformLimitedCompany(
    app: AgentApplicationLimitedCompany,
    cryptoOp: String => String
  ): AgentApplicationLimitedCompany = app
    .modify(_.internalUserId.value).using(cryptoOp)
    .modify(_.groupId.value).using(cryptoOp)
    .modify(_.applicantCredentials.providerId).using(cryptoOp)
    .modify(_.applicantContactDetails.each).using(transformApplicantContactDetails(_, cryptoOp))
    .modify(_.agentDetails.each).using(transformAgentDetails(_, cryptoOp))
    .modify(_.vrns.each.each.value).using(cryptoOp)
    .modify(_.payeRefs.each.each.value).using(cryptoOp)
    .modify(_.businessDetails.each.ctUtr.value).using(cryptoOp)
    .modify(_.businessDetails.each.companyProfile).using(transformCompanyProfile(_, cryptoOp))

  private def transformGeneralPartnership(
    app: AgentApplicationGeneralPartnership,
    cryptoOp: String => String
  ): AgentApplicationGeneralPartnership = app
    .modify(_.internalUserId.value).using(cryptoOp)
    .modify(_.groupId.value).using(cryptoOp)
    .modify(_.applicantCredentials.providerId).using(cryptoOp)
    .modify(_.applicantContactDetails.each).using(transformApplicantContactDetails(_, cryptoOp))
    .modify(_.agentDetails.each).using(transformAgentDetails(_, cryptoOp))
    .modify(_.vrns.each.each.value).using(cryptoOp)
    .modify(_.payeRefs.each.each.value).using(cryptoOp)
    .modify(_.businessDetails.each.saUtr.value).using(cryptoOp)
    .modify(_.businessDetails.each.postcode).using(cryptoOp)

  private def transformLimitedPartnership(
    app: AgentApplicationLimitedPartnership,
    cryptoOp: String => String
  ): AgentApplicationLimitedPartnership = app
    .modify(_.internalUserId.value).using(cryptoOp)
    .modify(_.groupId.value).using(cryptoOp)
    .modify(_.applicantCredentials.providerId).using(cryptoOp)
    .modify(_.applicantContactDetails.each).using(transformApplicantContactDetails(_, cryptoOp))
    .modify(_.agentDetails.each).using(transformAgentDetails(_, cryptoOp))
    .modify(_.vrns.each.each.value).using(cryptoOp)
    .modify(_.payeRefs.each.each.value).using(cryptoOp)
    .modify(_.businessDetails.each.saUtr.value).using(cryptoOp)
    .modify(_.businessDetails.each.companyProfile).using(transformCompanyProfile(_, cryptoOp))
    .modify(_.businessDetails.each.postcode).using(cryptoOp)

  private def transformScottishLimitedPartnership(
    app: AgentApplicationScottishLimitedPartnership,
    cryptoOp: String => String
  ): AgentApplicationScottishLimitedPartnership = app
    .modify(_.internalUserId.value).using(cryptoOp)
    .modify(_.groupId.value).using(cryptoOp)
    .modify(_.applicantCredentials.providerId).using(cryptoOp)
    .modify(_.applicantContactDetails.each).using(transformApplicantContactDetails(_, cryptoOp))
    .modify(_.agentDetails.each).using(transformAgentDetails(_, cryptoOp))
    .modify(_.vrns.each.each.value).using(cryptoOp)
    .modify(_.payeRefs.each.each.value).using(cryptoOp)
    .modify(_.businessDetails.each.saUtr.value).using(cryptoOp)
    .modify(_.businessDetails.each.companyProfile).using(transformCompanyProfile(_, cryptoOp))
    .modify(_.businessDetails.each.postcode).using(cryptoOp)

  private def transformScottishPartnership(
    app: AgentApplicationScottishPartnership,
    cryptoOp: String => String
  ): AgentApplicationScottishPartnership = app
    .modify(_.internalUserId.value).using(cryptoOp)
    .modify(_.groupId.value).using(cryptoOp)
    .modify(_.applicantCredentials.providerId).using(cryptoOp)
    .modify(_.applicantContactDetails.each).using(transformApplicantContactDetails(_, cryptoOp))
    .modify(_.agentDetails.each).using(transformAgentDetails(_, cryptoOp))
    .modify(_.vrns.each.each.value).using(cryptoOp)
    .modify(_.payeRefs.each.each.value).using(cryptoOp)
    .modify(_.businessDetails.each.saUtr.value).using(cryptoOp)
    .modify(_.businessDetails.each.postcode).using(cryptoOp)

  // Nested-type helpers (reused across subtypes) ----------------------------------------------

  private def transformApplicantContactDetails(
    c: ApplicantContactDetails,
    cryptoOp: String => String
  ): ApplicantContactDetails = c
    .modify(_.applicantName.value).using(cryptoOp)
    .modify(_.telephoneNumber.each.value).using(cryptoOp)
    .modify(_.applicantEmailAddress.each.emailAddress.value).using(cryptoOp)

  private def transformAgentDetails(
    d: AgentDetails,
    cryptoOp: String => String
  ): AgentDetails = d
    .modify(_.businessName.agentBusinessName).using(cryptoOp)
    .modify(_.businessName.otherAgentBusinessName.each).using(cryptoOp)
    .modify(_.telephoneNumber.each.agentTelephoneNumber).using(cryptoOp)
    .modify(_.telephoneNumber.each.otherAgentTelephoneNumber.each).using(cryptoOp)
    .modify(_.agentEmailAddress.each.emailAddress.agentEmailAddress).using(cryptoOp)
    .modify(_.agentEmailAddress.each.emailAddress.otherAgentEmailAddress.each).using(cryptoOp)
    .modify(_.agentCorrespondenceAddress.each.addressLine1).using(cryptoOp)
    .modify(_.agentCorrespondenceAddress.each.addressLine2.each).using(cryptoOp)
    .modify(_.agentCorrespondenceAddress.each.postalCode.each).using(cryptoOp)

  private def transformCompanyProfile(
    cp: CompanyProfile,
    cryptoOp: String => String
  ): CompanyProfile = cp
    .modify(_.companyNumber.value).using(cryptoOp)
    .modify(_.companyName).using(cryptoOp)
    .modify(_.unsanitisedCHROAddress.each).using(transformChroAddress(_, cryptoOp))

  private def transformChroAddress(
    a: ChroAddress,
    cryptoOp: String => String
  ): ChroAddress = a
    .modify(_.address_line_1.each).using(cryptoOp)
    .modify(_.address_line_2.each).using(cryptoOp)
    .modify(_.postal_code.each).using(cryptoOp)
