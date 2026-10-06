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
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistration.repository.MongoDateFormats
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr

import javax.inject.Inject
import javax.inject.Singleton

object IndividualProvidedDetailsMongoFormats:

  /** Dates as BSON dates, PII as it is. */
  val plaintextFormat: OFormat[IndividualProvidedDetails] = IndividualProvidedDetails.makeFormat(using MongoDateFormats.instantFormat)

/** Mongo formats of [[IndividualProvidedDetails]]. `encrypt` and `decrypt` share one `transform` that lists every PII path once, so the two cannot drift apart.
  */
@Singleton
class IndividualProvidedDetailsMongoFormats @Inject() (fieldLevelEncryption: FieldLevelEncryption):

  /** Encrypts PII on write and decrypts it on read. The repository's format, so PII is never stored in plaintext, whatever writes it. */
  val encryptingFormat: OFormat[IndividualProvidedDetails] = OFormat[IndividualProvidedDetails](
    r = IndividualProvidedDetailsMongoFormats.plaintextFormat.map[IndividualProvidedDetails](decrypt),
    w = IndividualProvidedDetailsMongoFormats.plaintextFormat.contramap[IndividualProvidedDetails](encrypt)
  )

  def encrypt(d: IndividualProvidedDetails): IndividualProvidedDetails = transform(d, fieldLevelEncryption.encrypt)
  def decrypt(d: IndividualProvidedDetails): IndividualProvidedDetails = transform(d, fieldLevelEncryption.decrypt)

  def encrypt(internalUserId: InternalUserId): InternalUserId = internalUserId.modify(_.value).using(fieldLevelEncryption.encrypt)
  def decrypt(internalUserId: InternalUserId): InternalUserId = internalUserId.modify(_.value).using(fieldLevelEncryption.decrypt)

  private def transform(
    d: IndividualProvidedDetails,
    cryptoOp: String => String
  ): IndividualProvidedDetails = d
    .modify(_.individualName.value).using(cryptoOp)
    .modify(_.internalUserId.each.value).using(cryptoOp)
    .modify(_.telephoneNumber.each.value).using(cryptoOp)
    .modify(_.emailAddress.each.emailAddress.value).using(cryptoOp)
    .modify(_.individualNino.each).using(transformIndividualNino(_, cryptoOp))
    .modify(_.individualSaUtr.each).using(transformIndividualSaUtr(_, cryptoOp))
    .modify(_.vrns.each.each.value).using(cryptoOp)
    .modify(_.payeRefs.each.each.value).using(cryptoOp)

  private def transformIndividualNino(
    n: IndividualNino,
    cryptoOp: String => String
  ): IndividualNino =
    n match
      case IndividualNino.NotProvided => n
      case p: IndividualNino.Provided => p.modify(_.nino.value).using(cryptoOp)
      case a: IndividualNino.FromAuth => a.modify(_.nino.value).using(cryptoOp)

  private def transformIndividualSaUtr(
    s: IndividualSaUtr,
    cryptoOp: String => String
  ): IndividualSaUtr =
    s match
      case IndividualSaUtr.NotProvided => s
      case p: IndividualSaUtr.Provided => p.modify(_.saUtr.value).using(cryptoOp)
      case a: IndividualSaUtr.FromAuth => a.modify(_.saUtr.value).using(cryptoOp)
      case c: IndividualSaUtr.FromCitizenDetails => c.modify(_.saUtr.value).using(cryptoOp)
