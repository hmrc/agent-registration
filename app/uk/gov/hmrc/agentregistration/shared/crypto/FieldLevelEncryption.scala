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

package uk.gov.hmrc.agentregistration.shared.crypto

import uk.gov.hmrc.crypto.Crypted
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.crypto.SymmetricCryptoFactory

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FieldLevelEncryption @Inject() (config: FieldLevelEncryptionConfig):

  def encrypt(plain: String): String =
    if config.enabled
    then
      crypto
        .encrypt(PlainText(plain))
        .value
    else plain

  def decrypt(encrypted: String): String =
    if config.enabled
    then
      crypto
        .decrypt(Crypted(encrypted))
        .value
    else encrypted

  private val crypto: Encrypter & Decrypter = SymmetricCryptoFactory.composeCrypto(
    currentCrypto = SymmetricCryptoFactory.aesCrypto(config.key),
    previousDecrypters = config.previousKeys.map(SymmetricCryptoFactory.aesCrypto)
  )
