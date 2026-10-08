# Cordova Plugin Android Biometric Auth

This plugin is a fork of [Cordova Plugin Android Fingerprint Auth](https://github.com/mjwheatley/cordova-plugin-android-fingerprint-auth), updated in order to use the new Android Biometric API (androidx.biometric).

This plugin will open a native dialog using the Android Biometric API to allow the user to authenticate using their biometric data.

This plugin will only work on devices with SDK >= 23 (Android 6.0) and with cordova-android >= 10.

# Migration from cordova-plugin-android-fingerprint-auth

All you have to do is to replace `FingerprintAuth` with `BiometricAuth`.

### Example

```javascript
//old code (cordova-plugin-android-fingerprint-auth)
FingerprintAuth.isAvailable(success, error);

//new code (cordova-plugin-android-biometric-auth)
BiometricAuth.isAvailable(success, error);
```

# Installation

### Cordova

`cordova plugin add https://github.com/mattiabottes/cordova-plugin-android-biometric-auth`

# How to use

- Call `isAvailable()` to check the biometric status.
- Call `encrypt()` or `decrypt()` show the Authentication Dialog.
- Call `delete()` when you want to delete the cipher for the user.

If you are not concerned with encrypting credentials and just want device authentication (biometric or backup), just call `encrypt()` with a `clientId` and look for a callback to the `successCallback`.

### Encrypt/Decrypt User Credentials

- Encrypt user credentials
  - Have user sign in with username and password.
  - Check plugin availability and pass username and password to `encrypt()`.
  - Store encrypted token with user profile.
- Decrypt user credentials
  - Prompt for username.
  - Query on username to retrieve encrypted token.
  - Pass username and token to `decrypt()` to return password.
  - Login using username and decrypted password.

# API Reference

- BiometricAuth
  - [isAvailable(isAvailableSuccess, isAvailableError)](#module_BiometricAuth.isAvailable)
  - [encrypt(encryptConfig, successCallback, errorCallback)](#module_BiometricAuth.encrypt)
  - [decrypt(decryptConfig, successCallback, errorCallback)](#module_BiometricAuth.decrypt)
  - [delete(deleteConfg, successCallback, errorCallback)](#module_BiometricAuth.delete)
  - [dismiss(successCallback, errorCallback)](#module_BiometricAuth.dismiss)
  - [ERRORS](#module_BiometricAuth.errors)
- [Config Object](#module_BiometricAuth.config)

<a name="module_BiometricAuth.isAvailable"></a>

# BiometricAuth.isAvailable(successCallback, errorCallback)

Opens a native dialog fragment to to authenticate against biometrics registered for the device.

### isAvailable() Result Object

| Param                 | Type    | Description                                           |
| --------------------- | ------- | ----------------------------------------------------- |
| isAvailable           | boolean | Biometric Authentication Dialog is available for use. |
| isHardwareDetected    | boolean | Device has hardware biometrics sensor.                |
| hasEnrolledBiometrics | boolean | Device has any biometrics enrolled.                   |

**Example**

```javascript
BiometricAuth.isAvailable(isAvailableSuccess, isAvailableError);

/**
 * @return {
 *      isAvailable:boolean,
 *      isHardwareDetected:boolean,
 *      hasEnrolledBiometrics:boolean
 *   }
 */
function isAvailableSuccess(result) {
  console.log("BiometricAuth available: " + JSON.stringify(result));
  if (result.isAvailable) {
    var encryptConfig = {}; // See config object for required parameters
    BiometricAuth.encrypt(encryptConfig, encryptSuccessCallback, encryptErrorCallback);
  }
}

function isAvailableError(message) {
  console.log("isAvailableError(): " + message);
}
```

<a name="module_BiometricAuth.config"></a>

### BiometricAuth Config Object

| Param            | Type    | Default   | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| ---------------- | ------- | --------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| clientId         | String  | undefined | (**REQUIRED**) Used as the alias for your app's secret key in the Android Key Store. Also used as part of the Shared Preferences key for the cipher userd to encrypt the user credentials.                                                                                                                                                                                                                                                                                                                                                 |
| username         | String  | undefined | Used to create credential string for encrypted token and as alias to retrieve the cipher.                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| password         | String  | undefined | Used to create credential string for encrypted token                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| token            | String  | undefined | Data to be decrypted. **_Required_** for `decrypt()`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| disableBackup    | boolean | false     | Set to true to remove the "USE BACKUP" button                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| maxAttempts      | number  | 5         | The device max is 5 attempts. Set this parameter if you want to allow fewer than 5 attempts.                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| locale           | String  | "en_US"   | Change the language displayed on the authentication dialog.<br/><ul><li>English: "en_US"</li><li>Italian: "it"</li><li>Spanish: "es"</li><li>Russian: "ru"</li><li>French: "fr"</li><li>Chinese (Simplified): <ul><li>"zh_CN"</li><li>"zh_SG"</li></ul></li><li>Chinese (Traditional): <ul><li>"zh"</li><li>"zh_HK"</li><li>"zh_TW"</li><li>"zh_MO"</li></ul></li><li>Norwegian: "no"</li><li>Portuguese: "pt"</li><li>Japanese: "ja"</li><li>German: "de"</li><li>Thai: "th"</li><li>Arabic: "ar"</li><li>Korean: "ko", "ko-KR"</li></ul> |
| userAuthRequired | boolean | false     | Require the user to authenticate with biometric to authorize every use of the key. New biometrics enrollment will invalidate key and require backup authenticate to re-enable the biometrics authentication dialog.                                                                                                                                                                                                                                                                                                                        |
| encryptNoAuth    | boolean | undefined | Bypass authentication and just encrypt input. If `true` this option will not display the authentication dialog for biometric or backup credentials. It will just encrypt the input and return a token.                                                                                                                                                                                                                                                                                                                                     |
| dialogTitle      | String  | undefined | Set the title of the biometric authentication dialog.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| dialogMessage    | String  | undefined | Set the message of the biometric authentication dialog.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| dialogHint       | String  | undefined | Set the hint displayed by the biometric icon on the biometric authentication dialog.                                                                                                                                                                                                                                                                                                                                                                                                                                                       |

<a name="module_BiometricAuth.encrypt"></a>

# BiometricAuth.encrypt(encryptConfig, encryptSuccessCallback, encryptErrorCallback)

### Result Object

| Param         | Type    | Description                                                                           |
| ------------- | ------- | ------------------------------------------------------------------------------------- |
| withBiometric | boolean | User authenticated using biometric.                                                   |
| withBackup    | boolean | User authenticated using backup credentials.                                          |
| token         | String  | Will contain the base64 encoded credentials upon successful biometric authentication. |

**Example**

```javascript
var encryptConfig = {
  clientId: "myAppName",
  username: "currentUser",
  password: "currentUserPassword",
};

BiometricAuth.encrypt(encryptConfig, successCallback, errorCallback);

function successCallback(result) {
  console.log("successCallback(): " + JSON.stringify(result));
  if (result.withBiometric) {
    console.log("Successfully encrypted credentials.");
    console.log("Encrypted credentials: " + result.token);
  } else if (result.withBackup) {
    console.log("Authenticated with backup password");
  }
}

function errorCallback(error) {
  if (error === BiometricAuth.ERRORS.BIOMETRIC_CANCELLED) {
    console.log("BiometricAuth Dialog Cancelled!");
  } else {
    console.log("BiometricAuth Error: " + error);
  }
}
```

<a name="module_BiometricAuth.decrypt"></a>

# BiometricAuth.decrypt(decryptConfig, encryptSuccessCallback, encryptErrorCallback)

### Result Object

| Param         | Type    | Description                                                                   |
| ------------- | ------- | ----------------------------------------------------------------------------- |
| withBiometric | boolean | User authenticated using biometric.                                           |
| withBackup    | boolean | User authenticated using backup credentials.                                  |
| password      | String  | Will contain the decrypted password upon successful biometric authentication. |

**Example**

```javascript
var decryptConfig = {
  clientId: "myAppName",
  username: "currentUser",
  token: "base64encodedUserCredentials",
};

BiometricAuth.decrypt(decryptConfig, successCallback, errorCallback);

function successCallback(result) {
  console.log("successCallback(): " + JSON.stringify(result));
  if (result.withBiometric) {
    console.log("Successful biometric authentication.");
    if (result.password) {
      console.log("Successfully decrypted credential token.");
      console.log("password: " + result.password);
    }
  } else if (result.withBackup) {
    console.log("Authenticated with backup password");
  }
}

function errorCallback(error) {
  if (error === BiometricAuth.ERRORS.BIOMETRIC_CANCELLED) {
    console.log("BiometricAuth Dialog Cancelled!");
  } else {
    console.log("BiometricAuth Error: " + error);
  }
}
```

<a name="module_BiometricAuth.delete"></a>

# BiometricAuth.delete(config, successCallback, errorCallback)

Used to delete a cipher.

#### Config Object

| Param    | Type   | Default   | Description                                                         |
| -------- | ------ | --------- | ------------------------------------------------------------------- |
| clientId | String | undefined | (REQUIRED) Used as the alias for your key in the Android Key Store. |
| username | String | undefined | Identify which cipher to delete.                                    |

**Example**

```javascript
BiometricAuth.delete(
  {
    clientId: "myAppName",
    username: "usernameToDelete",
  },
  successCallback,
  errorCallback,
);

function successCallback(result) {
  console.log("Successfully deleted cipher: " + JSON.stringify(result));
}

function errorCallback(error) {
  console.log(error);
}
```

<a name="module_BiometricAuth.dismiss"></a>

# BiometricAuth.dismiss(successCallback, errorCallback)

Used to dismiss a Biometric Authentication Dialog if one is being displayed

**Example**

```javascript
BiometricAuth.dismiss(successCallback, errorCallback);

function successCallback(result) {
  console.log("Successfully dismissed BiometricAuth dialog: " + JSON.stringify(result));
}

function errorCallback(error) {
  console.log(error);
}
```

<a name="module_BiometricAuth.errors"></a>

# BiometricAuth.ERRORS `JSON Object`

| Property                                 | Type   | Value                                      |
| ---------------------------------------- | ------ | ------------------------------------------ |
| BAD_PADDING_EXCEPTION                    | String | "BAD_PADDING_EXCEPTION"                    |
| CERTIFICATE_EXCEPTION                    | String | "BAD_PADDING_EXCEPTION"                    |
| BIOMETRIC_CANCELLED                      | String | "BIOMETRIC_CANCELLED"                      |
| BIOMETRIC_DATA_NOT_DELETED               | String | "BIOMETRIC_DATA_NOT_DELETED"               |
| BIOMETRIC_ERROR                          | String | "BIOMETRIC_ERROR"                          |
| BIOMETRIC_NOT_AVAILABLE                  | String | "BIOMETRIC_NOT_AVAILABLE"                  |
| BIOMETRIC_PERMISSION_DENIED              | String | "BIOMETRIC_PERMISSION_DENIED"              |
| BIOMETRIC_PERMISSION_DENIED_SHOW_REQUEST | String | "BIOMETRIC_PERMISSION_DENIED_SHOW_REQUEST" |
| ILLEGAL_BLOCK_SIZE_EXCEPTION             | String | "ILLEGAL_BLOCK_SIZE_EXCEPTION"             |
| INIT_CIPHER_FAILED                       | String | "INIT_CIPHER_FAILED"                       |
| INVALID_ALGORITHM_PARAMETER_EXCEPTION    | String | "INVALID_ALGORITHM_PARAMETER_EXCEPTION"    |
| IO_EXCEPTION                             | String | "IO_EXCEPTION"                             |
| JSON_EXCEPTION                           | String | "JSON_EXCEPTION"                           |
| MINIMUM_SDK                              | String | "MINIMUM_SDK"                              |
| MISSING_ACTION_PARAMETERS                | String | "MISSING_ACTION_PARAMETERS"                |
| MISSING_PARAMETERS                       | String | "MISSING_PARAMETERS"                       |
| NO_SUCH_ALGORITHM_EXCEPTION              | String | "NO_SUCH_ALGORITHM_EXCEPTION"              |
| SECURITY_EXCEPTION                       | String | "SECURITY_EXCEPTION"                       |
| FRAGMENT_NOT_EXIST                       | String | "FRAGMENT_NOT_EXIST                        |
