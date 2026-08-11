package com.example.reroplero.ui.presentation.screens


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reroplero.R
import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.domain.CurrencyRepository
import com.example.reroplero.domain.PaymentRepository
import com.example.reroplero.domain.ReceiptRepository
import com.example.reroplero.domain.SessionRepository
import com.example.reroplero.domain.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class MainPageViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val session: SessionRepository,
    private val globStore: PaymentRepository,
    private val apiRepo: CurrencyRepository,
    private val receiptRepo: ReceiptRepository,
    @ApplicationContext private val context: Context
    ): ViewModel() {

    private val MIN_DAYS_FOR_PROJECTION = 4
    private val FIXED_CATEGORIES = setOf<String>("Rent")

    private val _effects = Channel<MainEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        onIntent(MainIntent.Load)
        viewModelScope.launch {
            observeConnectivity().collect {
                isOnline -> if (isOnline) {
                    refreshCurrencies()

                }
            }
        }
    }

    fun onIntent(intent: MainIntent) {
        when (intent) {
            MainIntent.Load -> load()
            is MainIntent.SavePayment -> save(intent)
            is MainIntent.DeletePayment -> delete(intent.payment)
            is MainIntent.StartEditing -> _state.update { it.copy(editing = intent.payment) }
            is MainIntent.StopEditing -> _state.update { it.copy(editing = null) }
            is MainIntent.Logout -> logout()
            is MainIntent.RefreshCurrencies -> refreshCurrencies()
            is MainIntent.SetLogoutDialog -> _state.update { it.copy(showLogoutDialog = intent.visible) }
            is MainIntent.NextAnalyticsMonth -> nextAnalyticsMonth()
            is MainIntent.PreviousAnalyticsMonth -> previousAnalyticsMonth()
            is MainIntent.NextAnalyticsYear -> nextAnalyticsYear()
            is MainIntent.PreviousAnalyticsYear -> previousAnalyticsYear()
            is MainIntent.SetProfilePicture -> setProfilePicture(intent.uri)
            is MainIntent.ScanReceipt -> scanReceipt(intent.qr)
            is MainIntent.ChangePassword -> changePassword(intent.currentPassword, intent.newPassword)
        }
    }

    private fun earliestYearWithData(payments: List<Payment>) : Int {
        val zone = ZoneId.systemDefault()
        return payments.minOfOrNull {
            Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate().year
        } ?: YearMonth.now().year
    }
    private fun earliestMonthWithData(payments: List<Payment>): YearMonth? {
        val zone = ZoneId.systemDefault()
        return payments.minOfOrNull {
            YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate())
        } ?: YearMonth.now()
    }
    private fun MainUiState.withPayments(newPayments: List<Payment>) : MainUiState = copy(
        payments = newPayments,
        analytics = analyticsFrom(newPayments, analyticsMonth),
        canGoToNextMonth = analyticsMonth < YearMonth.now(),
        canGoToPreviousMonth = analyticsMonth > earliestMonthWithData(newPayments),
        canGoToNextYear = analyticsYear < YearMonth.now().year,
        canGoToPreviousYear = analyticsYear > earliestYearWithData(newPayments)
    )
    private fun MainUiState.withMonth(month: YearMonth) : MainUiState = copy(
        analyticsMonth = month,
        analytics = analyticsFrom(payments, month),
        canGoToPreviousMonth = month > earliestMonthWithData(payments),
        canGoToNextMonth = month < YearMonth.now()
    )
    private fun MainUiState.withYear(year: Int) : MainUiState = copy(
        analyticsYear = year,
        canGoToNextYear = year < YearMonth.now().year,
        canGoToPreviousYear = year > earliestYearWithData(payments)
    )
    private fun previousAnalyticsMonth() {
        val current = _state.value
        val target = current.analyticsMonth.minusMonths(1)
        if (target >= earliestMonthWithData(current.payments)) {
            _state.update { it.withMonth(target) }
        }
    }
    private fun nextAnalyticsMonth() {
        val current = _state.value
        val target = current.analyticsMonth.plusMonths(1)
        if (target <= YearMonth.now()){
            _state.update { it.withMonth(target) }
        }
    }
    private fun previousAnalyticsYear() {
        val current = _state.value
        val target = current.analyticsYear - 1
        if (target >= earliestYearWithData(current.payments)){
            _state.update { it.withYear(target) }
        }
    }
    private fun nextAnalyticsYear() {
        val current = _state.value
        val target = current.analyticsYear + 1
        if (target <= YearMonth.now().year){
            _state.update { it.withYear(target) }
        }
    }
    private fun refreshCurrencies() = viewModelScope.launch {
        if (state.value.currencies.size > 1) {
            return@launch
        }
        val currencies = apiRepo.availableCurrencies()
        if (currencies.size > 1) {
            _state.update{it.copy(currencies=currencies)}
        }
    }

    private fun load() = viewModelScope.launch {
        val user = session.currentUser()
        if (user == null) { _effects.send(MainEffect.GoToLogin); return@launch }
        _state.update { it.copy(isLoading = true, username = user) }
        val payments = globStore.getPayments(user)
        val total = userRepo.currentMoney(user) ?: 0.0
        // getUser throws NoSuchElementException, not NoSuchFileException — catching
        // the wrong type let it escape and crash the app on every launch whenever
        // the session named a user that was no longer in the database.
        val profilePicturePath = try { userRepo.getUser(user).profilePicturePath } catch (_: NoSuchElementException) { null }
        _state.update { it.withPayments(payments).copy(total = total, isLoading = false, profilePicturePath = profilePicturePath) }
        refreshCurrencies()
    }

    private fun save(intent: MainIntent.SavePayment) = viewModelScope.launch {
        val amount = intent.cost.toDoubleOrNull()
        if (amount == null || amount <= 0.0){
            _effects.send(MainEffect.ShowError("Enter a valid amount"))
            return@launch
        }
        val user = _state.value.username.ifBlank { return@launch }
        val eur = try {
            apiRepo.toEur(amount, intent.currency)
        }catch (_: Exception) {
            _effects.send(MainEffect.ShowError("Couldn't fetch exchange rate"))
            return@launch
        }
        val payment = Payment(
            id = _state.value.editing?.id ?: UUID.randomUUID().toString(),
            username = user,
            category = intent.category,
            cost = eur,
            timestamp = intent.timeMillis,
            // Editing an imported payment must not drop its receipt link, or the
            // receipt could be imported a second time.
            receiptUid = _state.value.editing?.receiptUid,
            receiptLine = _state.value.editing?.receiptLine
        )
        globStore.addPayment(payment)
        val payments = globStore.getPayments(user)
        val total = userRepo.currentMoney(user) ?: 0.0
        println("the user total is $total")
        _state.update { it.withPayments(payments).copy(total = total, editing = null, formVersion = it.formVersion + 1) }
        _effects.send(MainEffect.GoToList)
    }

    private fun scanReceipt(qr: String) = viewModelScope.launch {
        val user = _state.value.username.ifBlank { return@launch }
        _state.update { it.copy(isScanningReceipt = true) }
        try {
            val info = receiptRepo.fetch(qr)
            if (globStore.findByReceipt(user, info.uid) != null) {
                _state.update { it.copy(isScanningReceipt = false) }
                _effects.send(MainEffect.ShowError("That receipt is already saved"))
                return@launch
            }

            // AADE publishes only the issue date — none of the provider's exports
            // carry the time shown on the paper receipt — so imports land at
            // midnight. Analytics groups by day, so the time doesn't affect it.
            val timestamp = LocalDate.parse(info.issueDate)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val category = context.getString(R.string.other)

            // Amounts are already in EUR, so no conversion. If a provider ever
            // returns no line detail, fall back to a single payment for the total.
            val payments = if (info.lines.isNotEmpty()) {
                info.lines.map { line ->
                    newReceiptPayment(user, category, line.gross, timestamp, info.uid, line.number)
                }
            } else {
                listOf(newReceiptPayment(user, category, info.grossValue, timestamp, info.uid, null))
            }

            globStore.addPayments(payments)
            val refreshed = globStore.getPayments(user)
            val total = userRepo.currentMoney(user) ?: 0.0
            _state.update {
                it.withPayments(refreshed).copy(total = total, isScanningReceipt = false)
            }
            _effects.send(MainEffect.ShowMessage("Added ${payments.size} payments"))
            _effects.send(MainEffect.GoToList)
        } catch (_: IllegalArgumentException) {
            // A QR from an unsupported provider is a different problem from a
            // failed request, and the user can act on the difference.
            _state.update { it.copy(isScanningReceipt = false) }
            _effects.send(MainEffect.ShowError("Unrecognised receipt QR"))
        } catch (_: Exception) {
            _state.update { it.copy(isScanningReceipt = false) }
            _effects.send(MainEffect.ShowError("Couldn't read the receipt"))
        }
    }

    private fun newReceiptPayment(
        user: String,
        category: String,
        cost: Double,
        timestamp: Long,
        receiptUid: String,
        receiptLine: Int?
    ) = Payment(
        id = UUID.randomUUID().toString(),
        username = user,
        category = category,
        cost = cost,
        timestamp = timestamp,
        receiptUid = receiptUid,
        receiptLine = receiptLine
    )

    private fun delete(payment: Payment){
        val previous = _state.value.payments
        _state.update { s -> s.withPayments(s.payments.filterNot{it.id == payment.id}) }
        viewModelScope.launch {
            try {
                globStore.deletePayment(payment)
                val total = userRepo.currentMoney(_state.value.username) ?: 0.0
                println("the user total is $total on delete")
                _state.update {it.copy(total = total)}
            } catch (_: Exception) {
                _state.update { it.withPayments(previous) }
                _effects.send(MainEffect.ShowError("Couldn't delete"))
            }
        }
    }

    private fun logout() = viewModelScope.launch{
        _state.update { it.copy(showLogoutDialog = false) }
        session.clear()
        _effects.send(MainEffect.GoToLogin)
    }

    private fun setProfilePicture(uri: Uri) = viewModelScope.launch {
        val user = _state.value.username.ifBlank {
            return@launch
        }
        val path = try {
            copyToInternalStorage(uri, user)
        } catch (_: Exception) {
            _effects.send(MainEffect.ShowError("Couldn't save picture"))
            return@launch
        }
        userRepo.updateProfilePicture(user, path)
        _state.update { it.copy(profilePicturePath = path, profilePictureVersion = it.profilePictureVersion + 1) }
    }

    private fun copyToInternalStorage(uri: Uri, username: String) : String{
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw java.io.IOException("Couldn't open picked image")
        val orientation = ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val degrees = when(orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw java.io.IOException("Couldn't decode picked image")
        val rotated = if (degrees == 0f) bitmap else {
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(bitmap, 0,0, bitmap.width, bitmap.height, matrix, true)
        }
        val file = File(context.filesDir, "profile_$username.jpg")
        file.outputStream().use { out -> rotated.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        return file.absolutePath
    }

    private fun analyticsFrom(payments: List<Payment>, month: YearMonth) : AnalyticsStats {
        val zone = ZoneId.systemDefault()
        val daysElapsed = if (month == YearMonth.now()) LocalDate.now().dayOfMonth else month.lengthOfMonth()
        val thisMonth = payments.filter {
            YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()) == month
        }
        val spent = thisMonth.sumOf { it.cost }
        val fixed = thisMonth.filter { it.category in FIXED_CATEGORIES }.sumOf { it.cost }
        val variable = spent - fixed
        val variablePerDay = if (daysElapsed > 0) variable / daysElapsed else 0.0

        return AnalyticsStats(
            spentThisMonth = spent,
            fixedThisMonth = fixed,
            variablePerDay = variablePerDay,
            projectedTotal = if (daysElapsed < MIN_DAYS_FOR_PROJECTION) null else fixed + (variablePerDay) * month.lengthOfMonth()
        )
    }

    private fun changePassword(currentPassword: String, newPassword: String) = viewModelScope.launch {
        val user = _state.value.username.ifBlank { return@launch }
        if (newPassword.isBlank()) {
            _effects.send(MainEffect.ShowError("Enter a new password"))
            return@launch
        }else if (newPassword == currentPassword){
            _effects.send(MainEffect.ShowError("Enter another password"))
            return@launch
        }
        val success = userRepo.changePassword(user, currentPassword, newPassword)
        if (success) {
            _effects.send(MainEffect.ShowMessage("Password Changed"))
        }else{
            _effects.send(MainEffect.ShowError("Current password is incorrect"))
        }
    }

    private fun observeConnectivity() = callbackFlow<Boolean> {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
}

fun checkDouble(num: Double?): Double?{
    if (num != null && num > 0.0) return num
    return null
}