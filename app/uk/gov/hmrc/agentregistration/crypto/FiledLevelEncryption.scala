package uk.gov.hmrc.agentregistration.crypto

import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.crypto.Crypted
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.crypto.SymmetricCryptoFactory

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FiledLevelEncryption @Inject() (appConfig: AppConfig):

  def encrypt(plain: String): String =
    if appConfig.FieldLevelEncryption.enable
    then
      crypto
        .encrypt(PlainText(plain))
        .value
    else plain

  def decrypt(encrypted: String): String =
    if appConfig.FieldLevelEncryption.enable
    then
      crypto
        .decrypt(Crypted(encrypted))
        .value
    else encrypted

  private val crypto: Encrypter & Decrypter = SymmetricCryptoFactory.composeCrypto(
    currentCrypto = SymmetricCryptoFactory.aesCrypto(appConfig.FieldLevelEncryption.key),
    previousDecrypters = appConfig.FieldLevelEncryption.previousKeys
  )
