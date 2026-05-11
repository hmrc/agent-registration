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

import play.api.libs.functional.syntax.toInvariantFunctorOps
import play.api.libs.json.Format
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.PayeRef
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.Vrn
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.json.JsonEncryption.stringEncrypterDecrypter

/** Mongo-only Formats that encrypt the underlying string of single-field value classes . Compose these into aggregate mongoFormats by bringing the leaves into
  * implicit scope before Json.format[Parent].
  *
  * HTTP/JSON formats on the model companion objects are left untouched and continue to (de)serialise plaintext.
  */
object SensitiveFieldFormats:

  def payeRefMongoFormat(using crypto: Encrypter & Decrypter): Format[PayeRef] = stringEncrypterDecrypter.inmap[PayeRef](PayeRef.apply, _.value)

  def vrnMongoFormat(using crypto: Encrypter & Decrypter): Format[Vrn] = stringEncrypterDecrypter.inmap[Vrn](Vrn.apply, _.value)

  def ninoMongoFormat(using crypto: Encrypter & Decrypter): Format[Nino] = stringEncrypterDecrypter.inmap[Nino](Nino.apply, _.value)

  def saUtrMongoFormat(using crypto: Encrypter & Decrypter): Format[SaUtr] = stringEncrypterDecrypter.inmap[SaUtr](SaUtr.apply, _.value)

  def internalUserIdMongoFormat(using crypto: Encrypter & Decrypter): Format[InternalUserId] = stringEncrypterDecrypter.inmap[InternalUserId](
    InternalUserId.apply,
    _.value
  )

  def telephoneNumberMongoFormat(using crypto: Encrypter & Decrypter): Format[TelephoneNumber] = stringEncrypterDecrypter.inmap[TelephoneNumber](
    TelephoneNumber.apply,
    _.value
  )

  def individualNameMongoFormat(using crypto: Encrypter & Decrypter): Format[IndividualName] = stringEncrypterDecrypter.inmap[IndividualName](
    IndividualName.apply,
    _.value
  )

  def emailAddressMongoFormat(using crypto: Encrypter & Decrypter): Format[EmailAddress] = stringEncrypterDecrypter.inmap[EmailAddress](
    EmailAddress.apply,
    _.value
  )
