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
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.crypto.SymmetricHasher

import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FieldHasher @Inject() (appConfig: AppConfig):

  private val HashPrefix = "hash:"

  def isHashed(value: String): Boolean = value.startsWith(HashPrefix)

  def hash(plain: String): String =
    if isHashed(plain)
    then plain
    else HashPrefix + hasher.hash(PlainText(plain)).value

  private val hasher =
    new SymmetricHasher(
      new SecretKeySpec(
        Base64.getDecoder.decode(appConfig.FieldLevelEncryption.hasherKey),
        "HmacSHA256"
      )
    )
