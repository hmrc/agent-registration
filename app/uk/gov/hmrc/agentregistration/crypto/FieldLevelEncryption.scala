/*
 * Copyright 2026 HM Revenue & Customs
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

import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.crypto.Crypted
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.crypto.SymmetricCryptoFactory
import uk.gov.hmrc.crypto.SymmetricHasher

import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FieldLevelEncryption @Inject() (appConfig: AppConfig):

  def encrypt(plain: String): String =
    if appConfig.FieldLevelEncryption.enabled
    then
      crypto
        .encrypt(PlainText(plain))
        .value
    else plain

  def decrypt(encrypted: String): String =
    if appConfig.FieldLevelEncryption.enabled
    then
      crypto
        .decrypt(Crypted(encrypted))
        .value
    else encrypted

  def hash(plain: String): String =
    if plain.startsWith(HashPrefix) then plain
    else HashPrefix + hasher.hash(PlainText(plain)).value

  /** On read: if the stored value is already a hash, return it as-is. If it's a legacy encrypted value, decrypt it and hash it. The next upsert will then
    * persist the hash form.
    */
  def decryptThenHash(stored: String): String =
    if stored.startsWith(HashPrefix) then stored
    else hash(decrypt(stored))

  private val crypto: Encrypter & Decrypter = SymmetricCryptoFactory.composeCrypto(
    currentCrypto = SymmetricCryptoFactory.aesCrypto(appConfig.FieldLevelEncryption.key),
    previousDecrypters = appConfig.FieldLevelEncryption.previousKeys.map(SymmetricCryptoFactory.aesCrypto)
  )

  private val HashPrefix = "hash:"

  private val hasher =
    new SymmetricHasher(
      new SecretKeySpec(
        Base64.getDecoder.decode(appConfig.FieldLevelEncryption.hasherKey),
        "HmacSHA256"
      )
    )
