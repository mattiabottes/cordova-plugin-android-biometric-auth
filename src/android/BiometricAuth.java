package com.cordova.plugin.android.BiometricAuth;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cordova.plugin.android.BiometricAuth.BiometricAuth.PluginError;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

/*
 * Biometric Authentication using androidx.biometric.BiometricPrompt.
 * 
 * Derived from FingerprintAuth.java, cordova-plugin-android-fingerprint-auth
 * (https://github.com/mjwheatley/cordova-plugin-android-fingerprint-auth.git).
 * Edited by Mattia Bottes in 2026:
 * - renamed in BiometricAuth,
 * - migration to androidx.biometric, 
 * - deleted FingerprintManager and custom dialog files.
 */
public class BiometricAuth extends CordovaPlugin {
    public static final String TAG = "BiometricAuth";

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    public static final String FINGERPRINT_PREF_IV = "aes_iv";
    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;
    private static final String CREDENTIAL_DELIMITER = "|:|";

    public enum PluginError {
        BAD_PADDING_EXCEPTION,
        CERTIFICATE_EXCEPTION,
        BIOMETRIC_CANCELLED,
        BIOMETRIC_DATA_NOT_DELETED,
        BIOMETRIC_ERROR,
        BIOMETRIC_NOT_AVAILABLE,
        BIOMETRIC_PERMISSION_DENIED,
        BIOMETRIC_PERMISSION_DENIED_SHOW_REQUEST,
        ILLEGAL_BLOCK_SIZE_EXCEPTION,
        INIT_CIPHER_FAILED,
        INVALID_ALGORITHM_PARAMETER_EXCEPTION,
        IO_EXCEPTION,
        JSON_EXCEPTION,
        MINIMUM_SDK,
        MISSING_ACTION_PARAMETERS,
        MISSING_PARAMETERS,
        NO_SUCH_ALGORITHM_EXCEPTION,
        SECURITY_EXCEPTION,
        FRAGMENT_NOT_EXIST
    }

    private String mPackageName;
    private KeyguardManager mKeyguardManager;
    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;
    private Cipher mCipher;

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Executor mMainExecutor = mMainHandler::post;

    private CallbackContext mAuthCallback;
    private BiometricPrompt mBiometricPrompt;
    private int mSession = 0;
    private int mFailedAttempts = 0;
    private boolean mTooManyAttempts = false;

    private String mClientId;
    private String mUsername = "";
    private String mClientSecret;
    private boolean mCipherModeCrypt = true;
    private boolean mDisableBackup = false;
    private int mMaxAttempts = 6;
    private boolean mUserAuthRequired = false;
    private String mDialogTitle;
    private String mDialogMessage;
    private String mDialogHint;
    private Context mStringsContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mPackageName = cordova.getActivity().getPackageName();
        mKeyguardManager = cordova.getActivity().getSystemService(KeyguardManager.class);

        // catch for corrupted or malformed KeyStore
        try {
            mKeyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
            mKeyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            mCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize KeyStore/KeyGenerator/Cipher", e);
            mKeyGenerator = null;
            mKeyStore = null;
            mCipher = null;
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
            throws JSONException {
        Log.v(TAG, "BiometricAuth action: " + action);
        switch (action) {
            case "availability":
                sendAvailabilityResult(callbackContext);
                return true;
            case "encrypt":
            case "decrypt":
                startCryptoOperation("encrypt".equals(action), args.optJSONObject(0), callbackContext);
                return true;
            case "delete":
                deleteKey(args.optJSONObject(0), callbackContext);
                return true;
            case "dismiss":
                cordova.getActivity().runOnUiThread(() -> dismiss(callbackContext));
                return true;
            default:
                return false;
        }
    }

    //-- availability --

    private int canAuthenticateStrong() {
        return BiometricManager.from(cordova.getActivity()).canAuthenticate(BIOMETRIC_STRONG);
    }

    private void sendAvailabilityResult(CallbackContext cb) {
        int status = canAuthenticateStrong();
        boolean hardwareDetected = status != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
                && status != BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED;
        boolean available = status == BiometricManager.BIOMETRIC_SUCCESS;
        try {
            JSONObject result = new JSONObject();
            result.put("isAvailable", available);
            result.put("isHardwareDetected", hardwareDetected);
            result.put("hasEnrolledBiometrics", available);
            cb.success(result);
        } catch (JSONException e) {
            cb.error(PluginError.JSON_EXCEPTION.name());
        }
    }

    //-- encrypt / decrypt --

    private void startCryptoOperation(boolean encrypt, JSONObject arg, CallbackContext cb)
            throws JSONException {
        if (arg == null || !arg.has("clientId")) {
            cb.error(PluginError.MISSING_PARAMETERS.name());
            return;
        }
        if (!isCryptoReady()) {
            Log.e(TAG, "KeyStore not available on this device.");
            cb.error(PluginError.INIT_CIPHER_FAILED.name());
            return;
        }

        mClientId = arg.getString("clientId");
        mUsername = arg.optString("username", "");
        mCipherModeCrypt = encrypt;
        mDisableBackup = arg.optBoolean("disableBackup", false);
        mUserAuthRequired = arg.optBoolean("userAuthRequired", false);
        mMaxAttempts = 6;
        if (arg.has("maxAttempts")) {
            int maxAttempts = arg.getInt("maxAttempts");
            if (maxAttempts < 5) {
                mMaxAttempts = maxAttempts;
            }
        }
        mDialogTitle = arg.has("dialogTitle") ? arg.getString("dialogTitle") : null;
        mDialogMessage = arg.has("dialogMessage") ? arg.getString("dialogMessage") : null;
        mDialogHint = arg.has("dialogHint") ? arg.getString("dialogHint") : null;
        mStringsContext = arg.has("locale")
                ? localizedContext(arg.getString("locale")) : cordova.getActivity();

        final boolean encryptNoAuth;
        if (encrypt) {
            String password = arg.optString("password", "");
            mClientSecret = mClientId + mUsername + CREDENTIAL_DELIMITER + password;
            encryptNoAuth = arg.optBoolean("encryptNoAuth", false);
        } else {
            if (!arg.has("token")) {
                cb.error(PluginError.MISSING_ACTION_PARAMETERS.name());
                return;
            }
            mClientSecret = arg.getString("token");
            encryptNoAuth = false;
        }

        if (getSecretKey() == null) {
            String keyError = createKey();
            if (keyError != null) {
                cb.error(keyError);
                return;
            }
        }

        cordova.getActivity().runOnUiThread(() -> {
            cancelCurrentOperation();
            mAuthCallback = cb;

            if (encryptNoAuth) {
                finishWithBackupOrNoAuth();
                return;
            }

            if (canAuthenticateStrong() == BiometricManager.BIOMETRIC_SUCCESS) {
                if (initCipher()) {
                    showBiometricPrompt();
                } else if (!mDisableBackup) {
                    showBackupAuthentication();
                } else {
                    Log.e(TAG, "Failed to init Cipher and backup disabled.");
                    finishError(PluginError.INIT_CIPHER_FAILED.name());
                }
            } else if (!mDisableBackup && mKeyguardManager.isKeyguardSecure()) {
                showBackupAuthentication();
            } else {
                Log.e(TAG, "Biometric authentication not available");
                finishError(PluginError.BIOMETRIC_NOT_AVAILABLE.name());
            }
        });
    }

    private void showBiometricPrompt() {
        Activity activity = cordova.getActivity();
        if (!(activity instanceof FragmentActivity)) {
            Log.e(TAG, "BiometricPrompt requires a FragmentActivity (cordova-android >= 10)");
            finishError(PluginError.BIOMETRIC_NOT_AVAILABLE.name());
            return;
        }

        final int session = ++mSession;
        mFailedAttempts = 0;
        mTooManyAttempts = false;

        BiometricPrompt.PromptInfo.Builder info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(mDialogTitle != null ? mDialogTitle
                        : str("biometric_auth_dialog_title", "Authentication"))
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .setNegativeButtonText(mDisableBackup
                        ? str("cancel", "Cancel")
                        : str("use_backup", "Use backup"));

        if (mDialogMessage != null) {
            info.setSubtitle(mDialogMessage);
        }
        if (mDialogHint != null) {
            info.setDescription(mDialogHint);
        }

        mBiometricPrompt = new BiometricPrompt((FragmentActivity) activity, mMainExecutor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        if (session != mSession) return;
                        mBiometricPrompt = null;
                        BiometricPrompt.CryptoObject crypto = result.getCryptoObject();
                        finishWithCipher(true, crypto != null ? crypto.getCipher() : null);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        if (session != mSession) return;
                        mFailedAttempts++;
                        if (mFailedAttempts > mMaxAttempts && !mTooManyAttempts) {
                            mTooManyAttempts = true;
                            mBiometricPrompt.cancelAuthentication();
                        }
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        if (session != mSession) return;
                        mBiometricPrompt = null;
                        Log.w(TAG, "Biometric error " + errorCode + ": " + errString);
                        if (mTooManyAttempts) {
                            handleAuthFailure();
                            return;
                        }
                        switch (errorCode) {
                            case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
                                if (mDisableBackup) {
                                    finishCancelled();
                                } else {
                                    showBackupAuthenticationDeferred();
                                }
                                break;
                            case BiometricPrompt.ERROR_USER_CANCELED:
                            case BiometricPrompt.ERROR_CANCELED:
                                finishCancelled();
                                break;
                            default:
                                //no hardware available
                                handleAuthFailure();
                                break;
                        }
                    }
                });

        mBiometricPrompt.authenticate(info.build(), new BiometricPrompt.CryptoObject(mCipher));
    }

    private void handleAuthFailure() {
        if (mDisableBackup) {
            finishError(PluginError.BIOMETRIC_ERROR.name());
        } else {
            showBackupAuthenticationDeferred();
        }
    }

    private void showBackupAuthenticationDeferred() {
        final int session = mSession;
        mMainHandler.postDelayed(() -> {
            if (session == mSession && mAuthCallback != null) {
                showBackupAuthentication();
            }
        }, 300);
    }

    //-- PIN / sequence / password authentication (backup) --
    private void showBackupAuthentication() {
        if (mDisableBackup) {
            finishError(PluginError.BIOMETRIC_ERROR.name());
            return;
        }
        if (!mKeyguardManager.isKeyguardSecure()) {
            Toast.makeText(cordova.getActivity(),
                    str("secure_lock_screen_required", "Secure lock screen required"),
                    Toast.LENGTH_LONG).show();
            finishError(PluginError.BIOMETRIC_ERROR.name());
            return;
        }

        final int session = ++mSession;
        Activity activity = cordova.getActivity();
        String title = mDialogTitle != null ? mDialogTitle
                : str("biometric_auth_dialog_title", "Authentication");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activity instanceof FragmentActivity) {
            //API 30+: system prompt
            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setAllowedAuthenticators(DEVICE_CREDENTIAL)
                    .build();
            mBiometricPrompt = new BiometricPrompt((FragmentActivity) activity, mMainExecutor,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            if (session != mSession) return;
                            mBiometricPrompt = null;
                            finishWithBackupOrNoAuth();
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            if (session != mSession) return;
                            mBiometricPrompt = null;
                            Log.w(TAG, "Device credential error " + errorCode + ": " + errString);
                            finishCancelled();
                        }
                    });
            mBiometricPrompt.authenticate(info);
        } else {
            //API 23-29: confirm credential prompt (KeyGuard)
            @SuppressWarnings("deprecation")
            Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(title, mDialogMessage);
            if (intent == null) {
                finishError(PluginError.BIOMETRIC_NOT_AVAILABLE.name());
                return;
            }
            cordova.startActivityForResult(this, intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            return;
        }
        final int session = mSession;
        cordova.getActivity().runOnUiThread(() -> {
            if (session != mSession) return;
            if (resultCode == Activity.RESULT_OK) {
                finishWithBackupOrNoAuth();
            } else {
                finishCancelled();
            }
        });
    }

    //-- dismiss / delete --

    private void dismiss(CallbackContext cb) {
        if (mAuthCallback == null) {
            cb.error(PluginError.FRAGMENT_NOT_EXIST.name());
            return;
        }
        cancelCurrentOperation();
        cb.success("Fragment dismissed");
    }

    private void cancelCurrentOperation() {
        mSession++;
        if (mBiometricPrompt != null) {
            mBiometricPrompt.cancelAuthentication();
            mBiometricPrompt = null;
        }
        finishCancelled();
    }

    private void deleteKey(JSONObject arg, CallbackContext cb) throws JSONException {
        if (arg == null || !arg.has("clientId")) {
            cb.error(PluginError.MISSING_PARAMETERS.name());
            return;
        }
        if (!isCryptoReady()) {
            cb.error(PluginError.INIT_CIPHER_FAILED.name());
            return;
        }
        String clientId = arg.getString("clientId");
        String username = arg.optString("username", "");
        boolean secretKeyDeleted = false;
        boolean ivDeleted = false;
        try {
            mKeyStore.load(null);
            mKeyStore.deleteEntry(clientId);
            secretKeyDeleted = true;
            ivDeleted = deleteStringPreference(cordova.getActivity(), clientId + username,
                    FINGERPRINT_PREF_IV);
        } catch (Exception e) {
            Log.e(TAG, "Error while deleting SecretKey.", e);
        }
        if (secretKeyDeleted && ivDeleted) {
            cb.success();
        } else {
            cb.error(PluginError.BIOMETRIC_DATA_NOT_DELETED.name());
        }
    }

    //-- responses --

    private void finishWithBackupOrNoAuth() {
        if (!initCipher()) {
            createKey();
        }
        finishWithCipher(false, initCipher() ? mCipher : null);
    }

    private void finishWithCipher(boolean withBiometric, Cipher cipher) {
        if (mAuthCallback == null) return;
        String errorMessage;
        try {
            JSONObject result = new JSONObject();
            result.put(withBiometric ? "withBiometric" : "withBackup", true);

            if (cipher == null) {
                finishError(PluginError.INIT_CIPHER_FAILED.name());
                return;
            }

            if (mCipherModeCrypt) {
                byte[] bytes = cipher.doFinal(mClientSecret.getBytes(StandardCharsets.UTF_8));
                result.put("token", Base64.encodeToString(bytes, Base64.NO_WRAP));
            } else {
                byte[] bytes = cipher.doFinal(Base64.decode(mClientSecret, Base64.NO_WRAP));
                String credentialString = new String(bytes, StandardCharsets.UTF_8);
                String[] credentialArray = Pattern.compile(Pattern.quote(CREDENTIAL_DELIMITER))
                        .split(credentialString);
                if (credentialArray.length != 2) {
                    credentialArray = credentialString.split(":");
                }
                if (credentialArray.length == 2
                        && credentialArray[0].equalsIgnoreCase(mClientId + mUsername)) {
                    result.put("password", credentialArray[1]);
                }
            }
            finishSuccess(result);
            return;
        } catch (BadPaddingException e) {
            Log.e(TAG, "BadPaddingException: " + e);
            errorMessage = PluginError.BAD_PADDING_EXCEPTION.name();
        } catch (IllegalBlockSizeException e) {
            Log.e(TAG, "IllegalBlockSizeException: " + e);
            errorMessage = PluginError.ILLEGAL_BLOCK_SIZE_EXCEPTION.name();
        } catch (JSONException e) {
            Log.e(TAG, "JSONException: " + e);
            errorMessage = PluginError.JSON_EXCEPTION.name();
        } catch (RuntimeException e) {
            //base64 corrupted / malformed
            Log.e(TAG, "Crypto error: " + e);
            errorMessage = PluginError.INIT_CIPHER_FAILED.name();
        }
        finishError(errorMessage);
    }

    private void finishSuccess(JSONObject result) {
        if (mAuthCallback != null) {
            mAuthCallback.success(result);
            mAuthCallback = null;
        }
    }

    private void finishError(String error) {
        if (mAuthCallback != null) {
            mAuthCallback.error(error);
            mAuthCallback = null;
        }
    }

    private void finishCancelled() {
        finishError(PluginError.BIOMETRIC_CANCELLED.name());
    }

    //-- keystore --

    private boolean isCryptoReady() {
        return mKeyStore != null && mKeyGenerator != null && mCipher != null;
    }

    private boolean initCipher() {
        try {
            SecretKey key = getSecretKey();
            if (key == null) {
                return false;
            }
            if (mCipherModeCrypt) {
                mCipher.init(Cipher.ENCRYPT_MODE, key);
                setStringPreference(cordova.getActivity(), mClientId + mUsername, FINGERPRINT_PREF_IV,
                        Base64.encodeToString(mCipher.getIV(), Base64.NO_WRAP));
            } else {
                String iv = getStringPreference(cordova.getActivity(), mClientId + mUsername,
                        FINGERPRINT_PREF_IV);
                if (iv == null) {
                    Log.e(TAG, "Failed to init Cipher: IV not found");
                    return false;
                }
                mCipher.init(Cipher.DECRYPT_MODE, key,
                        new IvParameterSpec(Base64.decode(iv, Base64.NO_WRAP)));
            }
            return true;
        } catch (Exception e) {
            //KeyPermanentlyInvalidatedException if lockscreen or biometric has changed
            Log.e(TAG, "Failed to init Cipher: " + e);
            return false;
        }
    }

    private SecretKey getSecretKey() {
        try {
            mKeyStore.load(null);
            return (SecretKey) mKeyStore.getKey(mClientId, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get SecretKey from KeyStore: " + e);
            return null;
        }
    }

    private String createKey() {
        try {
            mKeyStore.load(null);
            mKeyGenerator.init(new KeyGenParameterSpec.Builder(mClientId,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(mUserAuthRequired)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            mKeyGenerator.generateKey();
            return null;
        } catch (java.security.NoSuchAlgorithmException e) {
            Log.e(TAG, "Failed to create key: " + e);
            return PluginError.NO_SUCH_ALGORITHM_EXCEPTION.name();
        } catch (java.security.InvalidAlgorithmParameterException e) {
            Log.e(TAG, "Failed to create key: " + e);
            return PluginError.INVALID_ALGORITHM_PARAMETER_EXCEPTION.name();
        } catch (java.security.cert.CertificateException e) {
            Log.e(TAG, "Failed to create key: " + e);
            return PluginError.CERTIFICATE_EXCEPTION.name();
        } catch (java.io.IOException e) {
            Log.e(TAG, "Failed to create key: " + e);
            return PluginError.IO_EXCEPTION.name();
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to create key: " + e);
            return PluginError.INIT_CIPHER_FAILED.name();
        }
    }

    //-- translations --

    private Context localizedContext(String langCode) {
        Activity activity = cordova.getActivity();
        if (langCode == null || langCode.isEmpty()) {
            return activity;
        }
        Configuration conf = new Configuration(activity.getResources().getConfiguration());
        conf.setLocale(Locale.forLanguageTag(langCode.replace('_', '-')));
        return activity.createConfigurationContext(conf);
    }

    private String str(String name, String fallback) {
        Context ctx = mStringsContext != null ? mStringsContext : cordova.getActivity();
        int id = ctx.getResources().getIdentifier(name, "string", mPackageName);
        return id != 0 ? ctx.getString(id) : fallback;
    }

    //-- SharedPreferences --

    public static String getStringPreference(Context context, String name, String key) {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE).getString(key, null);
    }

    public static void setStringPreference(Context context, String name, String key, String value) {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().putString(key, value).apply();
    }

    public static boolean deleteStringPreference(Context context, String name, String key) {
        SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit();
        return editor.remove(key).commit();
    }
}