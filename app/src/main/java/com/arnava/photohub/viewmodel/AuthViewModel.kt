package com.arnava.photohub.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arnava.photohub.R
import com.arnava.photohub.data.repository.AuthRepository
import com.arnava.photohub.data.repository.LocalRepository
import com.arnava.photohub.data.repository.UnsplashNetworkRepository
import com.arnava.photohub.data.local.UserInfoStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.TokenRequest
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val localRepository: LocalRepository,
    private val authRepository: AuthRepository,
    private val unsplashNetworkRepository: UnsplashNetworkRepository
) : ViewModel() {
    private val authService: AuthorizationService = AuthorizationService(appContext)
    var intentExternalData: Uri? = null

    private val toastEventChannel = Channel<String>(Channel.BUFFERED)
    val toastFlow = toastEventChannel.receiveAsFlow()

    private val authSuccessEventChannel = Channel<Unit>(Channel.BUFFERED)
    val authSuccessFlow = authSuccessEventChannel.receiveAsFlow()

    private val loadingMutableStateFlow = MutableStateFlow(false)
    val loadingFlow = loadingMutableStateFlow.asStateFlow()


    private fun onAuthCodeFailed(exception: AuthorizationException) {
        toastEventChannel.trySendBlocking(exception.errorDescription.toString())
    }

    private fun onAuthCodeReceived(tokenRequest: TokenRequest) {
        viewModelScope.launch {
            loadingMutableStateFlow.value = true
            runCatching {
                authRepository.performTokenRequest(
                    authService = authService,
                    tokenRequest = tokenRequest
                )
            }.onSuccess {
                loadingMutableStateFlow.value = false
                authSuccessEventChannel.send(Unit)
            }.onFailure {
                loadingMutableStateFlow.value = false
                toastEventChannel.send(appContext.getString(R.string.auth_canceled))
            }
        }
    }

    fun authorizeAndWait(authResponseResult: ActivityResultLauncher<Intent?>) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        val authRequest = authRepository.getAuthRequest()
        val openAuthPageIntent = authService.getAuthorizationRequestIntent(
            authRequest,
            customTabsIntent
        )
        authResponseResult.launch(openAuthPageIntent)
    }

    fun handleAuthResponseIntent(intent: Intent) {
        // пытаемся получить ошибку из ответа. null - если все ок
        val exception = AuthorizationException.fromIntent(intent)
        // пытаемся получить запрос для обмена кода на токен, null - если произошла ошибка
        val tokenExchangeRequest =
            AuthorizationResponse.fromIntent(intent)?.createTokenExchangeRequest()
        when {
            // авторизация завершались ошибкой
            exception != null -> onAuthCodeFailed(exception)
            // авторизация прошла успешно, меняем код на токен
            tokenExchangeRequest != null -> onAuthCodeReceived(tokenExchangeRequest)
        }
    }

    fun getTokenFromLocalStorage() = localRepository.getTokenFromSharedPrefs()
    fun clearToken() = authRepository.corruptAccessToken()

    fun saveUserNameToStorage() {
        runBlocking {
            val userInfo = unsplashNetworkRepository.getUserInfo()
            UserInfoStorage.userInfo = userInfo
        }
    }

    fun isFirstRun() = localRepository.isFirstRun()

    override fun onCleared() {
        super.onCleared()
        authService.dispose()
    }

}