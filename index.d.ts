/**
 * Derived from index.d.ts, cordova-plugin-android-biometric-auth
 * (https://github.com/mjwheatley/cordova-plugin-android-biometric-auth.git).
 *
 * Edited by Mattia Bottes, refactoring biometric refs in biometric.
 */

export const enum IBiometricAuthErrors {
  BAD_PADDING_EXCEPTION = "BAD_PADDING_EXCEPTION",
  CERTIFICATE_EXCEPTION = "CERTIFICATE_EXCEPTION",
  BIOMETRIC_CANCELLED = "BIOMETRIC_CANCELLED",
  BIOMETRIC_DATA_NOT_DELETED = "BIOMETRIC_DATA_NOT_DELETED",
  BIOMETRIC_ERROR = "BIOMETRIC_ERROR",
  BIOMETRIC_NOT_AVAILABLE = "BIOMETRIC_NOT_AVAILABLE",
  BIOMETRIC_PERMISSION_DENIED = "BIOMETRIC_PERMISSION_DENIED",
  BIOMETRIC_PERMISSION_DENIED_SHOW_REQUEST = "BIOMETRIC_PERMISSION_DENIED_SHOW_REQUEST",
  ILLEGAL_BLOCK_SIZE_EXCEPTION = "ILLEGAL_BLOCK_SIZE_EXCEPTION",
  INIT_CIPHER_FAILED = "INIT_CIPHER_FAILED",
  INVALID_ALGORITHM_PARAMETER_EXCEPTION = "INVALID_ALGORITHM_PARAMETER_EXCEPTION",
  IO_EXCEPTION = "IO_EXCEPTION",
  JSON_EXCEPTION = "JSON_EXCEPTION",
  MINIMUM_SDK = "MINIMUM_SDK",
  MISSING_ACTION_PARAMETERS = "MISSING_ACTION_PARAMETERS",
  MISSING_PARAMETERS = "MISSING_PARAMETERS",
  NO_SUCH_ALGORITHM_EXCEPTION = "NO_SUCH_ALGORITHM_EXCEPTION",
}

/**
 * The Cordova "BiometricAuth" plugin
 * See {@link https://github.com/mattiabottes/cordova-plugin-android-biometric-auth}
 */
interface IBiometricAuth {
  /**
   * @description Opens a native dialog fragment to use the device hardware biometric scanner
   * to authenticate against biometrics registered for the device.
   *
   * @param {BiometricAuthIsAvailableSuccess} successCallback - Success callback.
   * @param {string} errorCallback - Error callback.
   */
  isAvailable(successCallback, errorCallback): void;

  /**
   * @description Call encrypt() show the Authentication Dialog.
   *
   * @param {BiometricAuthConfig} encryptConfig - Encrypt config.
   * @param {BiometricAuthEncryptSuccess} encryptSuccessCallback - Encrypt success callback.
   * @param {IBiometricAuthErrors} encryptErrorCallback - Encrypt error callback.
   */
  encrypt(encryptConfig, encryptSuccessCallback, encryptErrorCallback): void;

  /**
   * @description Call decrypt() show the Authentication Dialog.
   *
   * @param {BiometricAuthConfig} decryptConfig - decryptConfig.
   * @param {BiometricAuthDecryptSuccess} encryptSuccessCallback - Encrypt success callback.
   * @param {IBiometricAuthErrors} encryptErrorCallback - Encrypt error callback.
   */
  decrypt(decryptConfig, encryptSuccessCallback, encryptErrorCallback): void;

  /**
   * @description Call delete() when you want to delete the cipher for the user.
   *
   * @param {BiometricDeleteConfig} config
   * @param {Object} successCallback - Success callback.
   * @param {string} errorCallback - Error callback.
   */
  delete(config, successCallback, errorCallback): void;
}

interface BiometricAuthIsAvailableSuccess {
  /**
   * @description Fingerprint Authentication Dialog is available for use.
   */
  isAvailable: boolean;

  /**
   * @description Device has hardware biometric sensor.
   */
  isHardwareDetected: boolean;

  /**
   * @description Device has any biometrics enrolled.
   */
  hasEnrolledBiometrics: boolean;
}

interface BiometricAuthEncryptSuccess {
  /**
   * @description User authenticated using biometric.
   */
  withBiometric: boolean;

  /**
   * @description User authenticated using backup credentials.
   */
  withBackup: boolean;

  /**
   * @description Will contain the base64 encoded credentials upon successful biometric authentication.
   */
  token: string;
}

interface BiometricAuthDecryptSuccess {
  /**
   * @description User authenticated using a biometric.
   */
  withBiometric: boolean;

  /**
   * @description User authenticated using backup credentials.
   */
  withBackup: boolean;

  /**
   * @description Will contain the decrypted password upon successful biometric authentication.
   */
  password: string;
}

interface BiometricAuthConfig {
  /**
   * @description (REQUIRED) Used as the alias for your app's secret key in the Android Key Store.
   * Also used as part of the Shared Preferences key for the cipher userd to encrypt the user credentials.
   */
  clientId: string;

  /**
   * @description Used to create credential string for encrypted token and as alias to retrieve the cipher.
   */
  username: string;

  /**
   * @description Used to create credential string for encrypted token.
   */
  password: string;

  /**
   * @description Data to be decrypted. Required for decrypt().
   */
  token: string;

  /**
   * @description Set to true to remove the "USE BACKUP" button.
   */
  disableBackup: boolean;

  /**
   * @description The device max is 5 attempts. Set this parameter if you want to allow fewer than 5 attempts.
   */
  maxAttempts: number;

  /**
   * @description Change the language displayed on the authentication dialog.
   * English: "en_US"
   * Italian: "it"
   * Spanish: "es"
   * Russian: "ru"
   * French: "fr"
   * Chinese (Simplified):
   *      "zh_CN"
   *      "zh_SG"
   * Chinese (Traditional):
   *      "zh"
   *      "zh_HK"
   *      "zh_TW"
   *      "zh_MO"
   * Norwegian: "no"
   * Portuguese: "pt"
   * Japanese: "ja"
   * German: "de"
   * Thai: "th"
   * Arabic: "ar"
   */
  locale: string;

  /**
   * @description Require the user to authenticate with a biometric to authorize every use of the key.
   * New biometric enrollment will invalidate key and require backup authenticate to re-enable the biometric authentication dialog.
   */
  userAuthRequired: boolean;

  /**
   * @description Set the title of the biometric authentication dialog.
   */
  dialogTitle: string;

  /**
   * @description Set the message of the biometric authentication dialog.
   */
  dialogMessage: string;

  /**
   * @description Set the hint displayed by the biometric icon on the biometric authentication dialog.
   */
  dialogHint: string;
}

interface BiometricDeleteConfig {
  /**
   * @description Identify which cipher to delete.
   */
  username: string;

  /**
   * @description (REQUIRED) Used as the alias for your key in the Android Key Store.
   */
  clientId: string;
}

declare var BiometricAuth: IBiometricAuth;
