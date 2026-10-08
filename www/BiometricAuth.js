function BiometricAuth() {
  BiometricAuth.prototype.ERRORS = {
    BAD_PADDING_EXCEPTION: "BAD_PADDING_EXCEPTION",
    CERTIFICATE_EXCEPTION: "CERTIFICATE_EXCEPTION",
    BIOMETRIC_CANCELLED: "BIOMETRIC_CANCELLED",
    BIOMETRIC_DATA_NOT_DELETED: "BIOMETRIC_DATA_NOT_DELETED",
    BIOMETRIC_ERROR: "BIOMETRIC_ERROR",
    BIOMETRIC_NOT_AVAILABLE: "BIOMETRIC_NOT_AVAILABLE",
    BIOMETRIC_PERMISSION_DENIED: "BIOMETRIC_PERMISSION_DENIED",
    BIOMETRIC_PERMISSION_DENIED_SHOW_REQUEST: "BIOMETRIC_PERMISSION_DENIED_SHOW_REQUEST",
    ILLEGAL_BLOCK_SIZE_EXCEPTION: "ILLEGAL_BLOCK_SIZE_EXCEPTION",
    INIT_CIPHER_FAILED: "INIT_CIPHER_FAILED",
    INVALID_ALGORITHM_PARAMETER_EXCEPTION: "INVALID_ALGORITHM_PARAMETER_EXCEPTION",
    IO_EXCEPTION: "IO_EXCEPTION",
    JSON_EXCEPTION: "JSON_EXCEPTION",
    MINIMUM_SDK: "MINIMUM_SDK",
    MISSING_ACTION_PARAMETERS: "MISSING_ACTION_PARAMETERS",
    MISSING_PARAMETERS: "MISSING_PARAMETERS",
    NO_SUCH_ALGORITHM_EXCEPTION: "NO_SUCH_ALGORITHM_EXCEPTION",
    SECURITY_EXCEPTION: "SECURITY_EXCEPTION",
  };
}

BiometricAuth.prototype.encrypt = function (params, successCallback, errorCallback) {
  cordova.exec(
    successCallback,
    errorCallback,
    "BiometricAuth", // Java Class
    "encrypt", // action
    [
      // Array of arguments to pass to the Java class
      params,
    ],
  );
};

BiometricAuth.prototype.decrypt = function (params, successCallback, errorCallback) {
  cordova.exec(
    successCallback,
    errorCallback,
    "BiometricAuth", // Java Class
    "decrypt", // action
    [
      // Array of arguments to pass to the Java class
      params,
    ],
  );
};

BiometricAuth.prototype.delete = function (params, successCallback, errorCallback) {
  cordova.exec(
    successCallback,
    errorCallback,
    "BiometricAuth", // Java Class
    "delete", // action
    [
      // Array of arguments to pass to the Java class
      params,
    ],
  );
};

BiometricAuth.prototype.dismiss = function (successCallback, errorCallback) {
  cordova.exec(
    successCallback,
    errorCallback,
    "BiometricAuth", // Java Class
    "dismiss", // action
    [{}],
  );
};

BiometricAuth.prototype.isAvailable = function (successCallback, errorCallback) {
  cordova.exec(
    successCallback,
    errorCallback,
    "BiometricAuth", // Java Class
    "availability", // action
    [{}],
  );
};

BiometricAuth = new BiometricAuth();
module.exports = BiometricAuth;
