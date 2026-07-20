package com.example.reroplero

import com.example.reroplero.data.PaymentRepository
import com.example.reroplero.data.local.models.Payment

class FakePaymentRepository : PaymentRepository {
    val payments = mutableListOf<Payment>()
    override suspend fun addPayment(payment: Payment): Boolean {
        payments.add(payment)
        return true
    }

    override suspend fun getPayments(username: String) = payments.filter{it.username == username}
    override suspend fun deletePayment(payment: Payment) { payments.remove(payment) }
    override suspend fun deletePaymentID(id: String) { payments.removeIf { it.id == id } }
    override suspend fun deleteAllPaymentsUser(user: String) {
        payments.removeIf { it.username == user }
    }

}

class MainPageViewModelTest {
//    @Test
//    fun `getPay returns empty list when nobody is logged in`() = runTest {
//        val payments = FakePaymentRepository()
//        val vm = MainPageViewModel(
//            session = FakePaymentRepository(currentUser = null),
//            globStore = payments,
//            userRepo = FakeUserRepository()
//        )
//        assertTrue(vm.getPay().isEmpty())
//    }


}