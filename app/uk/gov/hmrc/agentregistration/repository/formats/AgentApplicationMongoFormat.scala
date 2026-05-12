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

package uk.gov.hmrc.agentregistration.repository.formats

import play.api.libs.functional.syntax.*
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.OFormat
import play.api.libs.json.__
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.Crn
import uk.gov.hmrc.agentregistration.shared.CtUtr
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.PayeRef
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.Vrn
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentBusinessName
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentCorrespondenceAddress
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentDetails
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentEmailAddress
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentTelephoneNumber
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLimitedCompany
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsPartnership
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsScottishPartnership
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsSoleTrader
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.businessdetails.FullName
import uk.gov.hmrc.agentregistration.shared.companieshouse.ChroAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.json.JsonEncryption.stringEncrypterDecrypter

import scala.annotation.nowarn

object AgentApplicationMongoFormat:

  private def encryptedString(using crypto: Encrypter & Decrypter): Format[String] = stringEncrypterDecrypter

  private def credentialsMongoFormat(using crypto: Encrypter & Decrypter): OFormat[Credentials] =
    (
      (__ \ "providerId").format[String](encryptedString) and
        (__ \ "providerType").format[String]
    )(
      (
        providerId,
        providerType
      ) => Credentials(providerId, providerType),
      c => (c.providerId, c.providerType)
    )

  private def chroAddressMongoFormat(using crypto: Encrypter & Decrypter): OFormat[ChroAddress] =
    (
      (__ \ "address_line_1").formatNullable[String](encryptedString) and
        (__ \ "address_line_2").formatNullable[String](encryptedString) and
        (__ \ "locality").formatNullable[String] and
        (__ \ "care_of").formatNullable[String] and
        (__ \ "po_box").formatNullable[String] and
        (__ \ "postal_code").formatNullable[String](encryptedString) and
        (__ \ "premises").formatNullable[String] and
        (__ \ "country").formatNullable[String]
    )(
      (
        l1,
        l2,
        loc,
        co,
        pb,
        pc,
        pr,
        country
      ) =>
        ChroAddress(
          l1,
          l2,
          loc,
          co,
          pb,
          pc,
          pr,
          country
        ),
      c => (c.address_line_1, c.address_line_2, c.locality, c.care_of, c.po_box, c.postal_code, c.premises, c.country)
    )

  private def companyProfileMongoFormat(using crypto: Encrypter & Decrypter): OFormat[CompanyProfile] =
    given Format[Crn] = SensitiveFieldFormats.crnMongoFormat
    given OFormat[ChroAddress] = chroAddressMongoFormat
    given Format[String] = encryptedString
    Json.format[CompanyProfile]

  private def fullNameMongoFormat(using crypto: Encrypter & Decrypter): OFormat[FullName] =
    given Format[String] = encryptedString
    Json.format[FullName]

  private def applicantEmailAddressMongoFormat(using crypto: Encrypter & Decrypter): OFormat[ApplicantEmailAddress] =
    given Format[EmailAddress] = SensitiveFieldFormats.emailAddressMongoFormat
    Json.format[ApplicantEmailAddress]

  private def applicantContactDetailsMongoFormat(using crypto: Encrypter & Decrypter): OFormat[ApplicantContactDetails] =
    given Format[ApplicantName] = SensitiveFieldFormats.applicantNameMongoFormat
    given Format[TelephoneNumber] = SensitiveFieldFormats.telephoneNumberMongoFormat
    given OFormat[ApplicantEmailAddress] = applicantEmailAddressMongoFormat
    Json.format[ApplicantContactDetails]

  private def agentBusinessNameMongoFormat(using crypto: Encrypter & Decrypter): OFormat[AgentBusinessName] =
    given Format[String] = encryptedString
    Json.format[AgentBusinessName]

  private def agentTelephoneNumberMongoFormat(using crypto: Encrypter & Decrypter): OFormat[AgentTelephoneNumber] =
    given Format[String] = encryptedString
    Json.format[AgentTelephoneNumber]

  private def agentEmailAddressMongoFormat(using crypto: Encrypter & Decrypter): OFormat[AgentEmailAddress] =
    given Format[String] = encryptedString
    Json.format[AgentEmailAddress]

  private def agentVerifiedEmailAddressMongoFormat(using crypto: Encrypter & Decrypter): OFormat[AgentVerifiedEmailAddress] =
    given OFormat[AgentEmailAddress] = agentEmailAddressMongoFormat
    Json.format[AgentVerifiedEmailAddress]

  private def agentCorrespondenceAddressMongoFormat(using crypto: Encrypter & Decrypter): OFormat[AgentCorrespondenceAddress] =
    (
      (__ \ "addressLine1").format[String](encryptedString) and
        (__ \ "addressLine2").formatNullable[String](encryptedString) and
        (__ \ "addressLine3").formatNullable[String] and
        (__ \ "addressLine4").formatNullable[String] and
        (__ \ "postalCode").formatNullable[String](encryptedString) and
        (__ \ "countryCode").format[String]
    )(
      (
        l1,
        l2,
        l3,
        l4,
        pc,
        cc
      ) =>
        AgentCorrespondenceAddress(
          l1,
          l2,
          l3,
          l4,
          pc,
          cc
        ),
      a => (a.addressLine1, a.addressLine2, a.addressLine3, a.addressLine4, a.postalCode, a.countryCode)
    )

  private def agentDetailsMongoFormat(using crypto: Encrypter & Decrypter): OFormat[AgentDetails] =
    given OFormat[AgentBusinessName] = agentBusinessNameMongoFormat
    given OFormat[AgentTelephoneNumber] = agentTelephoneNumberMongoFormat
    given OFormat[AgentVerifiedEmailAddress] = agentVerifiedEmailAddressMongoFormat
    given OFormat[AgentCorrespondenceAddress] = agentCorrespondenceAddressMongoFormat
    Json.format[AgentDetails]

  private def businessDetailsLlpMongoFormat(using crypto: Encrypter & Decrypter): OFormat[BusinessDetailsLlp] =
    given Format[SaUtr] = SensitiveFieldFormats.saUtrMongoFormat
    given OFormat[CompanyProfile] = companyProfileMongoFormat
    Json.format[BusinessDetailsLlp]

  private def businessDetailsLimitedCompanyMongoFormat(using crypto: Encrypter & Decrypter): OFormat[BusinessDetailsLimitedCompany] =
    given Format[CtUtr] = SensitiveFieldFormats.ctUtrMongoFormat
    given OFormat[CompanyProfile] = companyProfileMongoFormat
    Json.format[BusinessDetailsLimitedCompany]

  private def businessDetailsPartnershipMongoFormat(using crypto: Encrypter & Decrypter): OFormat[BusinessDetailsPartnership] =
    given Format[SaUtr] = SensitiveFieldFormats.saUtrMongoFormat
    given OFormat[CompanyProfile] = companyProfileMongoFormat
    given Format[String] = encryptedString
    Json.format[BusinessDetailsPartnership]

  private def businessDetailsGeneralPartnershipMongoFormat(using crypto: Encrypter & Decrypter): OFormat[BusinessDetailsGeneralPartnership] =
    given Format[SaUtr] = SensitiveFieldFormats.saUtrMongoFormat
    given Format[String] = encryptedString
    Json.format[BusinessDetailsGeneralPartnership]

  private def businessDetailsScottishPartnershipMongoFormat(using crypto: Encrypter & Decrypter): OFormat[BusinessDetailsScottishPartnership] =
    given Format[SaUtr] = SensitiveFieldFormats.saUtrMongoFormat
    given Format[String] = encryptedString
    Json.format[BusinessDetailsScottishPartnership]

  private def businessDetailsSoleTraderMongoFormat(using crypto: Encrypter & Decrypter): OFormat[BusinessDetailsSoleTrader] =
    given Format[SaUtr] = SensitiveFieldFormats.saUtrMongoFormat
    given Format[Nino] = SensitiveFieldFormats.ninoMongoFormat
    given OFormat[FullName] = fullNameMongoFormat
    given Format[String] = encryptedString
    Json.format[BusinessDetailsSoleTrader]

  @nowarn()
  def mongoFormat(using crypto: Encrypter & Decrypter): OFormat[AgentApplication] =
    given Format[InternalUserId] = SensitiveFieldFormats.internalUserIdMongoFormat
    given Format[GroupId] = SensitiveFieldFormats.groupIdMongoFormat
    given Format[Vrn] = SensitiveFieldFormats.vrnMongoFormat
    given Format[PayeRef] = SensitiveFieldFormats.payeRefMongoFormat
    given OFormat[Credentials] = credentialsMongoFormat
    given OFormat[ApplicantContactDetails] = applicantContactDetailsMongoFormat
    given OFormat[AgentDetails] = agentDetailsMongoFormat
    given OFormat[BusinessDetailsLlp] = businessDetailsLlpMongoFormat
    given OFormat[BusinessDetailsLimitedCompany] = businessDetailsLimitedCompanyMongoFormat
    given OFormat[BusinessDetailsPartnership] = businessDetailsPartnershipMongoFormat
    given OFormat[BusinessDetailsGeneralPartnership] = businessDetailsGeneralPartnershipMongoFormat
    given OFormat[BusinessDetailsScottishPartnership] = businessDetailsScottishPartnershipMongoFormat
    given OFormat[BusinessDetailsSoleTrader] = businessDetailsSoleTraderMongoFormat

    given OFormat[AgentApplicationSoleTrader] = Json.format[AgentApplicationSoleTrader]
    given OFormat[AgentApplicationLlp] = Json.format[AgentApplicationLlp]
    given OFormat[AgentApplicationLimitedCompany] = Json.format[AgentApplicationLimitedCompany]
    given OFormat[AgentApplicationGeneralPartnership] = Json.format[AgentApplicationGeneralPartnership]
    given OFormat[AgentApplicationLimitedPartnership] = Json.format[AgentApplicationLimitedPartnership]
    given OFormat[AgentApplicationScottishLimitedPartnership] = Json.format[AgentApplicationScottishLimitedPartnership]
    given OFormat[AgentApplicationScottishPartnership] = Json.format[AgentApplicationScottishPartnership]

    given JsonConfiguration = JsonConfig.jsonConfiguration

    val dontDeleteMe = """
        |Don't delete me.
        |I will emit a warning so `@nowarn` can be applied to address below
        |`Unreachable case except for null` problem emited by Play Json macro"""

    Json.format[AgentApplication]
