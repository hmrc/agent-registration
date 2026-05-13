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

import com.softwaremill.quicklens.{each, modify}
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.crypto.FiledLevelEncryption
import uk.gov.hmrc.agentregistration.shared.{AgentApplication, InternalUserId, Vrn}
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainBytes, PlainContent, PlainText, SymmetricCryptoFactory}

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.{Inject, Singleton}

class AgentApplicationEncryption @Inject() (filedLevelEncryption: FiledLevelEncryption):


  def encrypt(agentApplication: AgentApplication): AgentApplication = agentApplication
    .modify(_.applicantContactDetails.each.applicantName.value)
    .using(filedLevelEncryption.encrypt)
    .modify(_.vrns.each)
    .using(_.map(encrypt))

  def decrypt(agentApplication: AgentApplication): AgentApplication = agentApplication
    .modify(_.applicantContactDetails.each.applicantName.value)
    .using(filedLevelEncryption.decrypt)


  def encrypt(vrn: Vrn): Vrn = vrn.modify(_.value).using(filedLevelEncryption.encrypt)
  def decrypt(vrn: Vrn): Vrn = vrn.modify(_.value).using(filedLevelEncryption.decrypt)

  def encrypt(internalUserId: InternalUserId): InternalUserId = internalUserId.modify(_.value).using(filedLevelEncryption.encrypt)
  def decrypt(internalUserId: InternalUserId): InternalUserId = internalUserId.modify(_.value).using(filedLevelEncryption.decrypt)

// TODO: and so on ...


//
//class CryptoProviderModule
//extends Module:
//
//  def aesCryptoInstance(configuration: Configuration): Encrypter & Decrypter =
//    if configuration.underlying.getBoolean("fieldLevelEncryption.enable")
//    then SymmetricCryptoFactory.aesCryptoFromConfig("fieldLevelEncryption", configuration.underlying)
//    else NoCrypto
//
//  override def bindings(
//    environment: Environment,
//    configuration: Configuration
//  ): Seq[Binding[?]] = Seq(
//    bind[Encrypter & Decrypter].qualifiedWith("ars").toInstance(aesCryptoInstance(configuration))
//  )
//
///** Encrypter/decrypter that does nothing (i.e. leaves content in plaintext). Only to be used for local development when fieldLevelEncryption.enable=false so we
//  * can read documents as plain text in mongo.
//  */
//trait NoCrypto
//extends Encrypter
//with Decrypter:
//
//  def encrypt(plain: PlainContent): Crypted =
//    plain match
//      case PlainText(text) => Crypted(text)
//      case PlainBytes(bytes) => Crypted(new String(Base64.getEncoder.encode(bytes), StandardCharsets.UTF_8))
//
//  def decrypt(notEncrypted: Crypted): PlainText = PlainText(notEncrypted.value)
//
//  def decryptAsBytes(notEncrypted: Crypted): PlainBytes = PlainBytes(Base64.getDecoder.decode(notEncrypted.value))
//
//object NoCrypto
//extends NoCrypto
