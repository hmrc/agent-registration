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

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.crypto.SymmetricCryptoFactory

class AgentApplicationMongoFormatSpec
extends UnitSpec:

  private val tdAll: TdAll = TdAll()

  private given crypto: Encrypter & Decrypter = SymmetricCryptoFactory.aesCrypto("HIvqb3uQRW8oryUZ3jEQPgMQsvgBSgl71ygWJk6VIdc=")

  private def enc(plain: String): String = crypto.encrypt(PlainText(plain)).value

  private val model: AgentApplicationLlp = tdAll.agentApplicationLlp.afterAgentDetailsComplete

  private val rendered: JsObject = Json.toJson[uk.gov.hmrc.agentregistration.shared.AgentApplication](model)(
    AgentApplicationMongoFormat.mongoFormat
  ).as[JsObject]
  private val renderedString: String = rendered.toString

  "mongoFormat encrypts PII fields at the expected paths" - {

    "internalUserId is encrypted" in:
      (rendered \ "internalUserId").as[String] shouldBe enc(model.internalUserId.value)

    "groupId is encrypted" in:
      (rendered \ "groupId").as[String] shouldBe enc(model.groupId.value)

    "applicantCredentials.providerId is encrypted" in:
      (rendered \ "applicantCredentials" \ "providerId").as[String] shouldBe enc(model.applicantCredentials.providerId)

    "applicantCredentials.providerType is plaintext" in:
      (rendered \ "applicantCredentials" \ "providerType").as[String] shouldBe model.applicantCredentials.providerType

    "businessDetails.saUtr is encrypted" in:
      (rendered \ "businessDetails" \ "saUtr").as[String] shouldBe enc(model.getBusinessDetails.saUtr.value)

    "businessDetails.companyProfile.companyNumber is encrypted" in:
      (rendered \ "businessDetails" \ "companyProfile" \ "companyNumber").as[String] shouldBe enc(model.getBusinessDetails.companyProfile.companyNumber.value)

    "businessDetails.companyProfile.companyName is encrypted" in:
      (rendered \ "businessDetails" \ "companyProfile" \ "companyName").as[String] shouldBe enc(model.getBusinessDetails.companyProfile.companyName)

    "businessDetails.companyProfile.unsanitisedCHROAddress.address_line_1 is encrypted" in:
      val chro = model.getBusinessDetails.companyProfile.unsanitisedCHROAddress.value
      (rendered \ "businessDetails" \ "companyProfile" \ "unsanitisedCHROAddress" \ "address_line_1").as[String] shouldBe enc(chro.address_line_1.value)

    "businessDetails.companyProfile.unsanitisedCHROAddress.address_line_2 is encrypted" in:
      val chro = model.getBusinessDetails.companyProfile.unsanitisedCHROAddress.value
      (rendered \ "businessDetails" \ "companyProfile" \ "unsanitisedCHROAddress" \ "address_line_2").as[String] shouldBe enc(chro.address_line_2.value)

    "businessDetails.companyProfile.unsanitisedCHROAddress.postal_code is encrypted" in:
      val chro = model.getBusinessDetails.companyProfile.unsanitisedCHROAddress.value
      (rendered \ "businessDetails" \ "companyProfile" \ "unsanitisedCHROAddress" \ "postal_code").as[String] shouldBe enc(chro.postal_code.value)

    "businessDetails.companyProfile.unsanitisedCHROAddress.country stays plaintext" in:
      val chro = model.getBusinessDetails.companyProfile.unsanitisedCHROAddress.value
      (rendered \ "businessDetails" \ "companyProfile" \ "unsanitisedCHROAddress" \ "country").as[String] shouldBe chro.country.value

    "applicantContactDetails.applicantName is encrypted" in:
      (rendered \ "applicantContactDetails" \ "applicantName").as[String] shouldBe enc(model.getApplicantContactDetails.applicantName.value)

    "applicantContactDetails.telephoneNumber is encrypted" in:
      (rendered \ "applicantContactDetails" \ "telephoneNumber").as[String] shouldBe enc(model.getApplicantContactDetails.getTelephoneNumber.value)

    "applicantContactDetails.applicantEmailAddress.emailAddress is encrypted" in:
      (rendered \ "applicantContactDetails" \ "applicantEmailAddress" \ "emailAddress").as[String] shouldBe enc(
        model.getApplicantContactDetails.getApplicantEmailAddress.emailAddress.value
      )

    "agentDetails.businessName.agentBusinessName is encrypted" in:
      (rendered \ "agentDetails" \ "businessName" \ "agentBusinessName").as[String] shouldBe enc(model.getAgentDetails.businessName.agentBusinessName)

    "agentDetails.telephoneNumber.agentTelephoneNumber is encrypted" in:
      (rendered \ "agentDetails" \ "telephoneNumber" \ "agentTelephoneNumber").as[String] shouldBe enc(
        model.getAgentDetails.getTelephoneNumber.agentTelephoneNumber
      )

    "agentDetails.agentEmailAddress.emailAddress.agentEmailAddress is encrypted" in:
      (rendered \ "agentDetails" \ "agentEmailAddress" \ "emailAddress" \ "agentEmailAddress").as[String] shouldBe enc(
        model.getAgentDetails.getAgentEmailAddress.emailAddress.agentEmailAddress
      )

    "agentDetails.agentCorrespondenceAddress.addressLine1 is encrypted" in:
      (rendered \ "agentDetails" \ "agentCorrespondenceAddress" \ "addressLine1").as[String] shouldBe enc(
        model.getAgentDetails.getAgentCorrespondenceAddress.addressLine1
      )

    "agentDetails.agentCorrespondenceAddress.addressLine2 is encrypted" in:
      (rendered \ "agentDetails" \ "agentCorrespondenceAddress" \ "addressLine2").as[String] shouldBe enc(
        model.getAgentDetails.getAgentCorrespondenceAddress.addressLine2.value
      )

    "agentDetails.agentCorrespondenceAddress.postalCode is encrypted" in:
      (rendered \ "agentDetails" \ "agentCorrespondenceAddress" \ "postalCode").as[String] shouldBe enc(
        model.getAgentDetails.getAgentCorrespondenceAddress.postalCode.value
      )

    "agentDetails.agentCorrespondenceAddress.countryCode stays plaintext" in:
      (rendered \ "agentDetails" \ "agentCorrespondenceAddress" \ "countryCode").as[String] shouldBe
        model.getAgentDetails.getAgentCorrespondenceAddress.countryCode

    "vrns are encrypted element-wise" in:
      (rendered \ "vrns").as[Seq[String]] shouldBe model.vrns.value.map(v => enc(v.value))

    "payeRefs are encrypted element-wise" in:
      (rendered \ "payeRefs").as[Seq[String]] shouldBe model.payeRefs.value.map(p => enc(p.value))

    "applicationReference stays plaintext (search key, not PII)" in:
      (rendered \ "applicationReference").as[String] shouldBe model.applicationReference.value

    "linkId stays plaintext" in:
      (rendered \ "linkId").as[String] shouldBe model.linkId.value

    "raw JSON does not contain any plaintext PII" in:
      val plaintextPii =
        Seq(
          model.internalUserId.value,
          model.groupId.value,
          model.applicantCredentials.providerId,
          model.getBusinessDetails.saUtr.value,
          model.getBusinessDetails.companyProfile.companyNumber.value,
          model.getBusinessDetails.companyProfile.companyName,
          model.getBusinessDetails.companyProfile.unsanitisedCHROAddress.value.address_line_1.value,
          model.getBusinessDetails.companyProfile.unsanitisedCHROAddress.value.address_line_2.value,
          model.getBusinessDetails.companyProfile.unsanitisedCHROAddress.value.postal_code.value,
          model.getApplicantContactDetails.applicantName.value,
          model.getApplicantContactDetails.getTelephoneNumber.value,
          model.getApplicantContactDetails.getApplicantEmailAddress.emailAddress.value,
          model.getAgentDetails.businessName.agentBusinessName,
          model.getAgentDetails.getTelephoneNumber.agentTelephoneNumber,
          model.getAgentDetails.getAgentEmailAddress.emailAddress.agentEmailAddress,
          model.getAgentDetails.getAgentCorrespondenceAddress.addressLine1,
          model.getAgentDetails.getAgentCorrespondenceAddress.addressLine2.value,
          model.getAgentDetails.getAgentCorrespondenceAddress.postalCode.value
        ) ++ model.vrns.value.map(_.value) ++ model.payeRefs.value.map(_.value)

      plaintextPii.foreach { plaintext =>
        withClue(s"plaintext '$plaintext' must not appear in raw JSON: ") {
          renderedString should not include s"\"$plaintext\""
        }
      }
  }

  "mongoFormat round-trips an AgentApplicationLlp" in:
    rendered.as[AgentApplication](AgentApplicationMongoFormat.mongoFormat) shouldBe model

  "mongoFormat encrypts subtype-specific PII fields" - {

    "BusinessDetailsPartnership.postcode is encrypted (LimitedPartnership)" in:
      val lp: AgentApplicationLimitedPartnership = tdAll.agentApplicationLimitedPartnership.afterDeclarationSubmitted
      val json: JsObject = Json.toJson[AgentApplication](lp)(AgentApplicationMongoFormat.mongoFormat).as[JsObject]
      (json \ "businessDetails" \ "postcode").as[String] shouldBe enc(lp.getBusinessDetails.postcode)

    "BusinessDetailsPartnership.postcode is encrypted (ScottishLimitedPartnership)" in:
      val slp: AgentApplicationScottishLimitedPartnership = tdAll.agentApplicationScottishLimitedPartnership.afterDeclarationSubmitted
      val json: JsObject = Json.toJson[AgentApplication](slp)(AgentApplicationMongoFormat.mongoFormat).as[JsObject]
      (json \ "businessDetails" \ "postcode").as[String] shouldBe enc(slp.getBusinessDetails.postcode)

    "BusinessDetailsGeneralPartnership.postcode is encrypted" in:
      val gp: AgentApplicationGeneralPartnership = tdAll.agentApplicationGeneralPartnership.afterDeclarationSubmitted
      val json: JsObject = Json.toJson[AgentApplication](gp)(AgentApplicationMongoFormat.mongoFormat).as[JsObject]
      (json \ "businessDetails" \ "postcode").as[String] shouldBe enc(gp.getBusinessDetails.postcode)

    "BusinessDetailsScottishPartnership.postcode is encrypted" in:
      val sp: AgentApplicationScottishPartnership = tdAll.agentApplicationScottishPartnership.afterDeclarationSubmitted
      val json: JsObject = Json.toJson[AgentApplication](sp)(AgentApplicationMongoFormat.mongoFormat).as[JsObject]
      (json \ "businessDetails" \ "postcode").as[String] shouldBe enc(sp.getBusinessDetails.postcode)

    "BusinessDetailsSoleTrader.trn is encrypted when provided" in:
      val withTrn = tdAll.agentApplicationSoleTrader.soleTraderWithTrn
      val json: JsObject = Json.toJson[AgentApplication](withTrn)(AgentApplicationMongoFormat.mongoFormat).as[JsObject]
      (json \ "businessDetails" \ "trn").as[String] shouldBe enc(tdAll.trn)

    "BusinessDetailsSoleTrader.fullName fields are encrypted" in:
      val st: AgentApplicationSoleTrader = tdAll.agentApplicationSoleTrader.afterDeclarationSubmitted
      val json: JsObject = Json.toJson[AgentApplication](st)(AgentApplicationMongoFormat.mongoFormat).as[JsObject]
      (json \ "businessDetails" \ "fullName" \ "firstName").as[String] shouldBe enc(st.getBusinessDetails.fullName.firstName)
      (json \ "businessDetails" \ "fullName" \ "lastName").as[String] shouldBe enc(st.getBusinessDetails.fullName.lastName)

    "BusinessDetailsLimitedCompany.ctUtr is encrypted" in:
      val ltd: AgentApplicationLimitedCompany = tdAll.agentApplicationLimitedCompany.afterDeclarationSubmitted
      val json: JsObject = Json.toJson[AgentApplication](ltd)(AgentApplicationMongoFormat.mongoFormat).as[JsObject]
      (json \ "businessDetails" \ "ctUtr").as[String] shouldBe enc(ltd.getBusinessDetails.ctUtr.value)
  }

  "mongoFormat round-trips every AgentApplication subtype and does not leak plaintext PII" - {
    val subtypes: Seq[(String, AgentApplication)] = Seq(
      "AgentApplicationLlp" -> tdAll.agentApplicationLlp.afterDeclarationSubmitted,
      "AgentApplicationSoleTrader" -> tdAll.agentApplicationSoleTrader.soleTraderWithTrn,
      "AgentApplicationLimitedCompany" -> tdAll.agentApplicationLimitedCompany.afterDeclarationSubmitted,
      "AgentApplicationGeneralPartnership" -> tdAll.agentApplicationGeneralPartnership.afterDeclarationSubmitted,
      "AgentApplicationLimitedPartnership" -> tdAll.agentApplicationLimitedPartnership.afterDeclarationSubmitted,
      "AgentApplicationScottishLimitedPartnership" -> tdAll.agentApplicationScottishLimitedPartnership.afterDeclarationSubmitted,
      "AgentApplicationScottishPartnership" -> tdAll.agentApplicationScottishPartnership.afterDeclarationSubmitted
    )

    subtypes.foreach { case (name, instance) =>
      s"$name writes and reads back equal" in:
        val json = Json.toJson[AgentApplication](instance)(AgentApplicationMongoFormat.mongoFormat)
        json.as[AgentApplication](AgentApplicationMongoFormat.mongoFormat) shouldBe instance

      s"$name rendered JSON does not contain any plaintext PII" in:
        val rendered = Json.toJson[AgentApplication](instance)(AgentApplicationMongoFormat.mongoFormat).toString
        piiStringsFor(instance).foreach { plaintext =>
          withClue(s"plaintext '$plaintext' must not appear in $name's raw JSON: ") {
            rendered should not include plaintext
          }
        }
    }
  }

  private def companyProfilePiiStrings(cp: CompanyProfile): Seq[String] =
    Seq(cp.companyNumber.value, cp.companyName) ++
      cp.unsanitisedCHROAddress.toList.flatMap(a =>
        Seq(
          a.address_line_1,
          a.address_line_2,
          a.postal_code
        ).flatten
      )

  private def piiStringsFor(application: AgentApplication): Seq[String] = {
    val common: Seq[String] =
      Seq(
        application.internalUserId.value,
        application.groupId.value,
        application.applicantCredentials.providerId
      ) ++
        application.applicantContactDetails.toList.flatMap { c =>
          Seq(c.applicantName.value) ++
            c.telephoneNumber.toList.map(_.value) ++
            c.applicantEmailAddress.toList.map(_.emailAddress.value)
        } ++
        application.agentDetails.toList.flatMap { a =>
          Seq(a.businessName.agentBusinessName) ++
            a.businessName.otherAgentBusinessName.toList ++
            a.telephoneNumber.toList.flatMap(t => Seq(t.agentTelephoneNumber) ++ t.otherAgentTelephoneNumber.toList) ++
            a.agentEmailAddress.toList.flatMap(e => Seq(e.emailAddress.agentEmailAddress) ++ e.emailAddress.otherAgentEmailAddress.toList) ++
            a.agentCorrespondenceAddress.toList.flatMap(addr => Seq(addr.addressLine1) ++ addr.addressLine2.toList ++ addr.postalCode.toList)
        } ++
        application.vrns.toList.flatten.map(_.value) ++
        application.payeRefs.toList.flatten.map(_.value)

    val businessSpecific: Seq[String] =
      application match
        case llp: AgentApplicationLlp =>
          val bd = llp.getBusinessDetails
          bd.saUtr.value +: companyProfilePiiStrings(bd.companyProfile)
        case ltd: AgentApplicationLimitedCompany =>
          val bd = ltd.getBusinessDetails
          bd.ctUtr.value +: companyProfilePiiStrings(bd.companyProfile)
        case lp: AgentApplicationLimitedPartnership =>
          val bd = lp.getBusinessDetails
          Seq(bd.saUtr.value, bd.postcode) ++ companyProfilePiiStrings(bd.companyProfile)
        case slp: AgentApplicationScottishLimitedPartnership =>
          val bd = slp.getBusinessDetails
          Seq(bd.saUtr.value, bd.postcode) ++ companyProfilePiiStrings(bd.companyProfile)
        case gp: AgentApplicationGeneralPartnership =>
          val bd = gp.getBusinessDetails
          Seq(bd.saUtr.value, bd.postcode)
        case sp: AgentApplicationScottishPartnership =>
          val bd = sp.getBusinessDetails
          Seq(bd.saUtr.value, bd.postcode)
        case st: AgentApplicationSoleTrader =>
          val bd = st.getBusinessDetails
          Seq(
            bd.saUtr.value,
            bd.fullName.firstName,
            bd.fullName.lastName
          ) ++
            bd.nino.map(_.value).toList ++
            bd.trn.toList

    common ++ businessSpecific
  }
