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

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.PayeRef
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.Vrn
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter

import scala.annotation.nowarn

object IndividualProvidedDetailsMongoFormat:

  private def individualVerifiedEmailAddressMongoFormat(using crypto: Encrypter & Decrypter): OFormat[IndividualVerifiedEmailAddress] =
    given Format[EmailAddress] = SensitiveFieldFormats.emailAddressMongoFormat
    Json.format[IndividualVerifiedEmailAddress]

  @nowarn()
  private def individualNinoMongoFormat(using crypto: Encrypter & Decrypter): OFormat[IndividualNino] =
    given JsonConfiguration = JsonConfig.jsonConfiguration
    given Format[Nino] = SensitiveFieldFormats.ninoMongoFormat
    given OFormat[IndividualNino.NotProvided.type] = Json.format[IndividualNino.NotProvided.type]
    given OFormat[IndividualNino.Provided] = Json.format[IndividualNino.Provided]
    given OFormat[IndividualNino.FromAuth] = Json.format[IndividualNino.FromAuth]
    Json.format[IndividualNino]

  @nowarn()
  private def individualSaUtrMongoFormat(using crypto: Encrypter & Decrypter): OFormat[IndividualSaUtr] =
    given JsonConfiguration = JsonConfig.jsonConfiguration
    given Format[SaUtr] = SensitiveFieldFormats.saUtrMongoFormat
    given OFormat[IndividualSaUtr.NotProvided.type] = Json.format[IndividualSaUtr.NotProvided.type]
    given OFormat[IndividualSaUtr.Provided] = Json.format[IndividualSaUtr.Provided]
    given OFormat[IndividualSaUtr.FromAuth] = Json.format[IndividualSaUtr.FromAuth]
    given OFormat[IndividualSaUtr.FromCitizenDetails] = Json.format[IndividualSaUtr.FromCitizenDetails]
    Json.format[IndividualSaUtr]

  def mongoFormat(using crypto: Encrypter & Decrypter): OFormat[IndividualProvidedDetails] =
    given Format[IndividualName] = SensitiveFieldFormats.individualNameMongoFormat
    given Format[InternalUserId] = SensitiveFieldFormats.internalUserIdMongoFormat
    given Format[TelephoneNumber] = SensitiveFieldFormats.telephoneNumberMongoFormat
    given Format[Vrn] = SensitiveFieldFormats.vrnMongoFormat
    given Format[PayeRef] = SensitiveFieldFormats.payeRefMongoFormat
    given OFormat[IndividualVerifiedEmailAddress] = individualVerifiedEmailAddressMongoFormat
    given OFormat[IndividualNino] = individualNinoMongoFormat
    given OFormat[IndividualSaUtr] = individualSaUtrMongoFormat
    Json.format[IndividualProvidedDetails]
